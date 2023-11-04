/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal.binders;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.SoftDeletable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.models.ModelsException;
import org.hibernate.models.orm.bind.internal.SecondaryTable;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.internal.BindingHelper;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.bind.spi.TableReference;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;

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

	private final TableReference primaryTable;
	private final DelegateBinders delegateBinders;
	private Map<String,SecondaryTable> secondaryTableMap;

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

		state.getMetadataBuildingContext().getMetadataCollector().addImport( importName, entityName );


		this.primaryTable = delegateBinders.getTableBinder().processPrimaryTable( getManagedType() );
		final var table = primaryTable.getBinding();
		( (TableOwner) binding ).setTable( table );

		final List<SecondaryTable> secondaryTables = delegateBinders.getTableBinder().processSecondaryTables( getManagedType() );
		secondaryTables.forEach( (secondaryTable) -> {
			final Join join = new Join();
			join.setTable( secondaryTable.binding() );
			join.setPersistentClass( binding );
			join.setOptional( secondaryTable.optional() );
			join.setInverse( !secondaryTable.owned() );
		} );

		if ( binding instanceof RootClass ) {
			processSoftDelete( primaryTable.getBinding(), classDetails, state, context );
			processOptimisticLocking( primaryTable.getBinding(), classDetails, state, context );
			processCacheRegions( classDetails, state, context );
		}

		processCaching( classDetails, state, context );

		prepareBinding( delegateBinders );
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
		( (SoftDeletable) primaryTable ).enableSoftDelete( softDeleteIndicatorColumn );
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
				softDeleteConfig.getString( softDeleteConfig.getString( "columnName" ) )
		);
		final Identifier physicalColumnName = namingStrategy.toPhysicalColumnName(
				database.toIdentifier( logicalColumnName ),
				database.getJdbcEnvironment()
		);
		softDeleteColumn.setName( physicalColumnName.render( database.getDialect() ) );
	}

	private void processOptimisticLocking(Table binding, ClassDetails classDetails, BindingState state, BindingContext context) {
		final var optimisticLocking = classDetails.getAnnotationUsage( OptimisticLocking.class );

		if ( optimisticLocking != null ) {
			final var optimisticLockingType = optimisticLocking.getEnum( "type", OptimisticLockType.VERSION );
			final var rootEntity = (RootClass) getTypeBinding();
			rootEntity.setOptimisticLockStyle( OptimisticLockStyle.valueOf( optimisticLockingType.name() ) );
		}
	}

	private void processCacheRegions(ClassDetails classDetails, BindingState state, BindingContext context) {

	}

	private void processCaching(ClassDetails classDetails, BindingState state, BindingContext context) {

	}

	@Override
	public void processSecondPasses() {
		delegateBinders.getTableBinder().processSecondPasses();
		super.processSecondPasses();
	}
}
