/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.bind.internal.BindingHelper;
import org.hibernate.boot.models.bind.internal.SecondaryTable;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.event.internal.EntityCallback;
import org.hibernate.jpa.event.internal.ListenerCallback;
import org.hibernate.jpa.event.spi.CallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.models.ModelsException;
import org.hibernate.boot.models.categorize.spi.CacheRegion;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.categorize.spi.NaturalIdCacheRegion;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.InheritanceType;

import static org.hibernate.internal.util.StringHelper.coalesce;

/**
 * Binder for binding a {@linkplain PersistentClass} from a {@linkplain EntityTypeMetadata}
 *
 * @author Steve Ebersole
 */
public class EntityTypeBinder extends IdentifiableTypeBinder {
	private final PersistentClass binding;

	private final DelegateBinders delegateBinders;

	public EntityTypeBinder(
			EntityTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			DelegateBinders delegateBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		super( type, superType, hierarchyRelation, state, options, context );
		this.delegateBinders = delegateBinders;

		final ClassDetails classDetails = type.getClassDetails();
		this.binding = createBinding();

		final AnnotationUsage<Entity> entityAnn = classDetails.getAnnotationUsage( Entity.class );
		final String jpaEntityName = BindingHelper.getValue( entityAnn, "name", null );
		final String entityName;
		final String importName;

		if ( classDetails.getName() != null
				&& !classDetails.getName().equals( classDetails.getClassName() ) ) {
			// should indicate a dynamic model
			entityName = classDetails.getName();
		}
		else {
			entityName = classDetails.getClassName();
		}

		if ( jpaEntityName != null ) {
			importName = jpaEntityName;
		}
		else {
			importName = StringHelper.unqualifyEntityName( entityName );
		}

		binding.setClassName( classDetails.getClassName() );
		binding.setEntityName( entityName );
		binding.setJpaEntityName( jpaEntityName );

		state.registerTypeBinder( type, this );
		state.getMetadataBuildingContext().getMetadataCollector().addImport( importName, entityName );

		final var primaryTable = delegateBinders.getTableBinder().processPrimaryTable( getManagedType() );
		final var table = primaryTable.getBinding();
		( (TableOwner) binding ).setTable( table );

		final var secondaryTables = delegateBinders.getTableBinder().processSecondaryTables( getManagedType() );
		secondaryTables.forEach( this::processSecondaryTable );

		final IdentifiableTypeBinder superTypeBinder = getSuperTypeBinder();
		final EntityTypeBinder superEntityBinder = getSuperEntityBinder();
		if ( binding instanceof RootClass rootClass ) {
			processSoftDelete( primaryTable.getBinding(), rootClass, classDetails, state, context );
			processOptimisticLocking( primaryTable.getBinding(), rootClass, classDetails, state, context );
			processCacheRegions( getManagedType(), rootClass, classDetails, state, context );

			assert superEntityBinder == null;

			if ( superTypeBinder != null ) {
				rootClass.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinder.getTypeBinding() );
			}
		}
		else {
			final Subclass subclass = (Subclass) binding;

			if ( (superTypeBinder == superEntityBinder && superTypeBinder != null) ) {
				// the super is an entity
				subclass.setSuperclass( superEntityBinder.getTypeBinding() );
			}
			else if ( superTypeBinder != null ) {
				subclass.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinder.getTypeBinding() );
			}
		}

		processCaching( classDetails, state, context );
		processFilters( classDetails, state, context );
		processJpaEventListeners( type, state, context );

		prepareBinding( delegateBinders );
	}

	private void processJpaEventListeners(EntityTypeMetadata type, BindingState state, BindingContext context) {
		final List<JpaEventListener> listeners = type.getCompleteJpaEventListeners();
		if ( CollectionHelper.isEmpty( listeners ) ) {
			return;
		}

		listeners.forEach( (listener) -> {
			if ( listener.getStyle() == JpaEventListenerStyle.CALLBACK ) {
				processEntityCallbacks( listener );
			}
			else {
				assert listener.getStyle() == JpaEventListenerStyle.LISTENER;
				processListenerCallbacks( listener );
			}
		} );
	}

	private void processEntityCallbacks(JpaEventListener listener) {
		final Class<?> entityClass = listener.getCallbackClass().toJavaClass();
		processJpaEventCallbacks( entityClass, listener, JpaEventListenerStyle.CALLBACK, null );
	}

	private void processJpaEventCallbacks(
			Class<?> listenerClass,
			JpaEventListener listener,
			JpaEventListenerStyle style,
			Class<?> methodArgumentType) {
		assert style == JpaEventListenerStyle.CALLBACK || methodArgumentType != null;

		// todo : would be nicer to allow injecting them one at a time.
		//  		upstream is defined currently to accept a List
		final List<CallbackDefinition> callbackDefinitions = new ArrayList<>();

		final MethodDetails prePersistMethod = listener.getPrePersistMethod();
		if ( prePersistMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, prePersistMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.PRE_PERSIST
			) );
		}

		final MethodDetails postPersistMethod = listener.getPostPersistMethod();
		if ( postPersistMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postPersistMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_PERSIST
			) );
		}

		final MethodDetails preUpdateMethod = listener.getPreUpdateMethod();
		if ( preUpdateMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, preUpdateMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.PRE_UPDATE
			) );
		}

		final MethodDetails postUpdateMethod = listener.getPostUpdateMethod();
		if ( postUpdateMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postUpdateMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_UPDATE
			) );
		}

		final MethodDetails preRemoveMethod = listener.getPreRemoveMethod();
		if ( preRemoveMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, preRemoveMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.PRE_REMOVE
			) );
		}

		final MethodDetails postRemoveMethod = listener.getPostRemoveMethod();
		if ( postRemoveMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postRemoveMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_REMOVE
			) );
		}

		final MethodDetails postLoadMethod = listener.getPostLoadMethod();
		if ( postLoadMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postLoadMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_LOAD
			) );
		}

		binding.addCallbackDefinitions( callbackDefinitions );
	}

	private static CallbackDefinition createCallbackDefinition(
			Class<?> listenerClass,
			Method callbackMethod,
			JpaEventListenerStyle style,
			CallbackType callbackType) {
		final CallbackDefinition callback;
		if ( style == JpaEventListenerStyle.CALLBACK ) {
			callback = new EntityCallback.Definition( callbackMethod, callbackType );
		}
		else {
			callback = new ListenerCallback.Definition( listenerClass, callbackMethod, callbackType );
		}
		return callback;
	}

	private void processListenerCallbacks(JpaEventListener listener) {
		final Class<?> listenerClass = listener.getCallbackClass().toJavaClass();
		processJpaEventCallbacks( listenerClass, listener, JpaEventListenerStyle.LISTENER, getManagedType().getClassDetails().toJavaClass() );
	}

	private Method findCallbackMethod(
			Class<?> callbackTarget,
			MethodDetails callbackMethod,
			Class<?> entityType) {
		try {
			if ( callbackMethod.getArgumentTypes().isEmpty() ) {
				return callbackTarget.getDeclaredMethod( callbackMethod.getName() );
			}
			else {
				final ClassDetails argClassDetails = callbackMethod.getArgumentTypes().get( 0 );
				// we don't
				return callbackTarget.getMethod( callbackMethod.getName(), argClassDetails.toJavaClass() );
			}
		}
		catch (NoSuchMethodException e) {
			final ModelsException modelsException = new ModelsException(
					String.format(
							Locale.ROOT,
							"Unable to locate callback method - %s.%s",
							callbackTarget.getName(),
							callbackMethod.getName()
					)
			);
			modelsException.addSuppressed( e );
			throw modelsException;
		}
	}

	@Override
	public PersistentClass getTypeBinding() {
		return binding;
	}

	@Override
	public Table getTable() {
		return binding.getTable();
	}

	@Override
	public EntityTypeMetadata getManagedType() {
		return (EntityTypeMetadata) super.getManagedType();
	}

	@Override
	public EntityTypeMetadata findSuperEntity() {
		return getManagedType();
	}

	private PersistentClass createBinding() {
		if ( getHierarchyRelation() == EntityHierarchy.HierarchyRelation.SUB ) {
			return createSubclass();
		}
		else {
			return new RootClass( getBindingState().getMetadataBuildingContext() );
		}
	}

	private PersistentClass createSubclass() {
		final IdentifiableTypeMetadata superType = getSuperType();

		final IdentifiableTypeBinder superTypeBinder = (IdentifiableTypeBinder) getBindingState().getTypeBinder( superType.getClassDetails() );
		final IdentifiableTypeClass superTypeBinding = superTypeBinder.getTypeBinding();

		// we have a few cases to handle here, complicated by how Hibernate has historically modeled
		// mapped-superclass in its PersistentClass model (aka, not well) which manifests in some
		// craziness over how these PersistentClass instantiations happen

		final InheritanceType inheritanceType = superType.getHierarchy().getInheritanceType();
		if ( inheritanceType == InheritanceType.JOINED ) {
			return createJoinedSubclass( superTypeBinding );
		}

		if ( inheritanceType == InheritanceType.TABLE_PER_CLASS ) {
			return createUnionSubclass( superTypeBinding );
		}

		assert inheritanceType == null || inheritanceType == InheritanceType.SINGLE_TABLE;
		return createSingleTableSubclass( superTypeBinding );
	}

	private UnionSubclass createUnionSubclass(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass superEntity ) {
			return new UnionSubclass( superEntity, getBindingState().getMetadataBuildingContext() );
		}
		else {
			assert superTypeBinding instanceof MappedSuperclass;

			final PersistentClass superEntity = resolveSuperEntity( superTypeBinding );
			final UnionSubclass binding = new UnionSubclass(
					superEntity,
					getBindingState().getMetadataBuildingContext()
			);
			binding.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinding );
			return binding;
		}
	}

	private JoinedSubclass createJoinedSubclass(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass superEntity ) {
			return new JoinedSubclass( superEntity, getBindingState().getMetadataBuildingContext() );
		}
		else {
			assert superTypeBinding instanceof MappedSuperclass;

			final PersistentClass superEntity = resolveSuperEntity( superTypeBinding );
			final JoinedSubclass binding = new JoinedSubclass(
					superEntity,
					getBindingState().getMetadataBuildingContext()
			);
			binding.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinding );
			return binding;
		}
	}

	private SingleTableSubclass createSingleTableSubclass(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass superEntity ) {
			return new SingleTableSubclass( superEntity, getBindingState().getMetadataBuildingContext() );
		}
		else {
			assert superTypeBinding instanceof MappedSuperclass;

			final PersistentClass superEntity = resolveSuperEntity( superTypeBinding );
			final SingleTableSubclass binding = new SingleTableSubclass(
					superEntity,
					getBindingState().getMetadataBuildingContext()
			);
			binding.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinding );
			return binding;
		}
	}

	private PersistentClass resolveSuperEntity(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass ) {
			return (PersistentClass) superTypeBinding;
		}

		if ( superTypeBinding.getSuperType() != null ) {
			return resolveSuperEntity( superTypeBinding.getSuperType() );
		}

		throw new ModelsException( "Unable to resolve super PersistentClass for " + superTypeBinding );
	}

	@Override
	protected void prepareBinding(DelegateBinders delegateBinders) {
		// todo : possibly Hierarchy details - version, tenant-id, ...

		super.prepareBinding( delegateBinders );
	}

	private void processSoftDelete(
			Table primaryTable,
			RootClass rootClass,
			ClassDetails classDetails,
			BindingState state,
			BindingContext context) {
		final AnnotationUsage<SoftDelete> softDeleteConfig = getTypeBinding() instanceof RootClass
				? classDetails.getAnnotationUsage( SoftDelete.class )
				: null;
		if ( softDeleteConfig == null ) {
			return;
		}

		final BasicValue softDeleteIndicatorValue = createSoftDeleteIndicatorValue( softDeleteConfig, primaryTable, state, context );
		final Column softDeleteIndicatorColumn = createSoftDeleteIndicatorColumn(
				softDeleteConfig,
				softDeleteIndicatorValue,
				state,
				context
		);
		primaryTable.addColumn( softDeleteIndicatorColumn );
		rootClass.enableSoftDelete( softDeleteIndicatorColumn );
	}

	private static BasicValue createSoftDeleteIndicatorValue(
			AnnotationUsage<SoftDelete> softDeleteAnn,
			Table table,
			BindingState state,
			BindingContext context) {
		assert softDeleteAnn != null;

		final var converterClassDetails = softDeleteAnn.getClassDetails( "converter" );
		final ClassBasedConverterDescriptor converterDescriptor = new ClassBasedConverterDescriptor(
				converterClassDetails.toJavaClass(),
				context.getBootstrapContext().getClassmateContext()
		);

		final BasicValue softDeleteIndicatorValue = new BasicValue( state.getMetadataBuildingContext(), table );
		softDeleteIndicatorValue.makeSoftDelete( softDeleteAnn.getEnum( "strategy" ) );
		softDeleteIndicatorValue.setJpaAttributeConverterDescriptor( converterDescriptor );
		softDeleteIndicatorValue.setImplicitJavaTypeAccess( (typeConfiguration) -> converterDescriptor.getRelationalValueResolvedType().getErasedType() );
		return softDeleteIndicatorValue;
	}

	private static Column createSoftDeleteIndicatorColumn(
			AnnotationUsage<SoftDelete> softDeleteConfig,
			BasicValue softDeleteIndicatorValue,
			BindingState state,
			BindingContext context) {
		final Column softDeleteColumn = new Column();

		applyColumnName( softDeleteColumn, softDeleteConfig, state, context );

		softDeleteColumn.setLength( 1 );
		softDeleteColumn.setNullable( false );
		softDeleteColumn.setUnique( false );
		softDeleteColumn.setComment( "Soft-delete indicator" );

		softDeleteColumn.setValue( softDeleteIndicatorValue );
		softDeleteIndicatorValue.addColumn( softDeleteColumn );

		return softDeleteColumn;
	}

	private static void applyColumnName(
			Column softDeleteColumn,
			AnnotationUsage<SoftDelete> softDeleteConfig,
			BindingState state,
			BindingContext context) {
		final Database database = state.getMetadataBuildingContext().getMetadataCollector().getDatabase();
		final PhysicalNamingStrategy namingStrategy = state.getMetadataBuildingContext().getBuildingOptions().getPhysicalNamingStrategy();
		final SoftDeleteType strategy = softDeleteConfig.getEnum( "strategy" );
		final String logicalColumnName = coalesce(
				strategy.getDefaultColumnName(),
				softDeleteConfig.getString( "columnName" )
		);
		final Identifier physicalColumnName = namingStrategy.toPhysicalColumnName(
				database.toIdentifier( logicalColumnName ),
				database.getJdbcEnvironment()
		);
		softDeleteColumn.setName( physicalColumnName.render( database.getDialect() ) );
	}

	private static void processOptimisticLocking(
			Table binding,
			RootClass rootEntity,
			ClassDetails classDetails,
			BindingState state,
			BindingContext context) {
		final var optimisticLocking = classDetails.getAnnotationUsage( OptimisticLocking.class );

		if ( optimisticLocking != null ) {
			final var optimisticLockingType = optimisticLocking.getEnum( "type", OptimisticLockType.VERSION );
			rootEntity.setOptimisticLockStyle( OptimisticLockStyle.valueOf( optimisticLockingType.name() ) );
		}
	}

	private static void processCacheRegions(
			EntityTypeMetadata source,
			RootClass rootClass,
			ClassDetails classDetails,
			BindingState state,
			BindingContext context) {
		final EntityHierarchy hierarchy = source.getHierarchy();
		final CacheRegion cacheRegion = hierarchy.getCacheRegion();
		final NaturalIdCacheRegion naturalIdCacheRegion = hierarchy.getNaturalIdCacheRegion();

		if ( cacheRegion != null ) {
			rootClass.setCacheRegionName( cacheRegion.getRegionName() );
			rootClass.setCacheConcurrencyStrategy( cacheRegion.getAccessType().getExternalName() );
			rootClass.setLazyPropertiesCacheable( cacheRegion.isCacheLazyProperties() );
		}

		if ( naturalIdCacheRegion != null ) {
			rootClass.setNaturalIdCacheRegionName( naturalIdCacheRegion.getRegionName() );
		}
	}

	private void processCaching(ClassDetails classDetails, BindingState state, BindingContext context) {
		final var cacheableAnn = classDetails.getAnnotationUsage( Cacheable.class );
		if ( cacheableAnn == null ) {
			return;
		}

		final boolean cacheable = cacheableAnn.getBoolean( "value", true );
		binding.setCached( cacheable );
	}

	private void processFilters(ClassDetails classDetails, BindingState state, BindingContext context) {
		final List<AnnotationUsage<Filter>> filters = classDetails.getRepeatedAnnotationUsages( Filter.class );
		if ( CollectionHelper.isEmpty( filters ) ) {
			return;
		}

		filters.forEach( (filter) -> {
			binding.addFilter(
					filter.getString( "name" ),
					filter.getString( "condition", null ),
					filter.getAttributeValue( "deduceAliasInjectionPoints", true ),
					extractFilterAliasTableMap( filter ),
					extractFilterAliasEntityMap( filter )
			);
		} );
	}

	private Map<String, String> extractFilterAliasTableMap(AnnotationUsage<Filter> filter) {
		return null;
	}

	private Map<String, String> extractFilterAliasEntityMap(AnnotationUsage<Filter> filter) {
		return null;
	}

	@Override
	public void processSecondPasses() {
		delegateBinders.getTableBinder().processSecondPasses();
		super.processSecondPasses();
	}

	private void processSecondaryTable(SecondaryTable secondaryTable) {
		final Join join = new Join();
		join.setTable( secondaryTable.binding() );
		join.setPersistentClass( binding );
		join.setOptional( secondaryTable.optional() );
		join.setInverse( !secondaryTable.owned() );
	}
}
