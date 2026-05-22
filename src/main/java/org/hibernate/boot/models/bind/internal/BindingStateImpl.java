/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.bind.internal.binders.AssociationTableBinding;
import org.hibernate.boot.models.bind.internal.binders.CollectionTableBinding;
import org.hibernate.boot.models.bind.internal.binders.EntityTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.IdentifierBinding;
import org.hibernate.boot.models.bind.internal.binders.IdentifiableTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.InversePluralAssociationBinding;
import org.hibernate.boot.models.bind.internal.binders.ManagedTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.MappedSuperTypeBinder;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableOwner;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.mapping.Join;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class BindingStateImpl implements BindingState {
	private final MetadataBuildingContext metadataBuildingContext;

	private final Database database;
	private final JdbcServices jdbcServices;

	private final Map<String, TableReference> tableMap = new HashMap<>();
	private final Map<TableOwner, TableReference> tableByOwnerMap = new HashMap<>();
	private final Map<Join, AssociationTableBinding> associationTableBindings = new HashMap<>();
	private final java.util.List<CollectionTableBinding> collectionTableBindings = new java.util.ArrayList<>();
	private final java.util.List<InversePluralAssociationBinding> inversePluralAssociationBindings = new java.util.ArrayList<>();

	private final Map<ClassDetails, ManagedTypeBinder> typeBinders = new HashMap<>();
	private final Map<ClassDetails, IdentifiableTypeBinder> typeBindersBySuper = new HashMap<>();
	private final Map<EntityTypeMetadata, IdentifierBinding> identifierBindings = new HashMap<>();

	public BindingStateImpl(MetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.database = metadataBuildingContext.getMetadataCollector().getDatabase();
		this.jdbcServices = metadataBuildingContext.getBootstrapContext().getServiceRegistry().getService( JdbcServices.class );
	}

	@Override
	public MetadataBuildingContext getMetadataBuildingContext() {
		return metadataBuildingContext;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	@Override
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	@Override
	public void registerTypeBinder(ManagedTypeMetadata type, ManagedTypeBinder binder) {
		typeBinders.put( type.getClassDetails(), binder );

		if ( type instanceof IdentifiableTypeMetadata identifiableType ) {
			if ( identifiableType.getSuperType() != null ) {
				typeBindersBySuper.put(
						identifiableType.getSuperType().getClassDetails(),
						(IdentifiableTypeBinder) binder
				);
			}
		}

		if ( binder instanceof EntityTypeBinder entityTypeBinder ) {
			metadataBuildingContext.getMetadataCollector().addEntityBinding( entityTypeBinder.getTypeBinding() );
		}
		else if ( binder instanceof MappedSuperTypeBinder mappedSuperBinder ) {
			metadataBuildingContext.getMetadataCollector().addMappedSuperclass(
					mappedSuperBinder.getManagedType().getClassDetails().toJavaClass(),
					mappedSuperBinder.getTypeBinding()
			);
		}
	}

	@Override
	public ManagedTypeBinder getTypeBinder(ClassDetails type) {
		return typeBinders.get( type );
	}

	@Override
	public IdentifiableTypeBinder getSuperTypeBinder(ClassDetails type) {
		return typeBindersBySuper.get( type );
	}

	@Override
	public void addIdentifierBinding(EntityTypeMetadata rootType, IdentifierBinding identifierBinding) {
		identifierBindings.put( rootType, identifierBinding );
	}

	@Override
	public IdentifierBinding getIdentifierBinding(EntityTypeMetadata rootType) {
		return identifierBindings.get( rootType );
	}

	@Override
	public void forEachType(KeyedConsumer<String,ManagedTypeBinder> consumer) {
		typeBinders.forEach( (classDetails, managedTypeBinder) -> consumer.accept( classDetails.getName(), managedTypeBinder ) );
	}

	@Override
	public int getTableCount() {
		return tableMap.size();
	}

	@Override
	public void forEachTable(KeyedConsumer<String,TableReference> consumer) {
		//noinspection unchecked
		tableMap.forEach( (BiConsumer<? super String, ? super TableReference>) consumer );
	}

	@Override
	public <T extends TableReference> T getTableByName(String name) {
		//noinspection unchecked
		return (T) tableMap.get( name );
	}

	@Override
	public <T extends TableReference> T getTableByOwner(TableOwner owner) {
		//noinspection unchecked
		return (T) tableByOwnerMap.get( owner );
	}

	@Override
	public void addTable(TableOwner owner, TableReference table) {
		tableMap.put( table.logicalName().getCanonicalName(), table );
		tableByOwnerMap.put( owner, table );
	}

	@Override
	public void addSecondaryTable(SecondaryTable table) {
		tableMap.put( table.logicalName().getCanonicalName(), table );
	}

	@Override
	public void addAssociationTableBinding(AssociationTableBinding associationTableBinding) {
		associationTableBindings.put( associationTableBinding.join(), associationTableBinding );
	}

	@Override
	public AssociationTableBinding getAssociationTableBinding(Join join) {
		return associationTableBindings.get( join );
	}

	@Override
	public void addCollectionTableBinding(CollectionTableBinding collectionTableBinding) {
		collectionTableBindings.add( collectionTableBinding );
	}

	@Override
	public void forEachCollectionTableBinding(java.util.function.Consumer<CollectionTableBinding> consumer) {
		collectionTableBindings.forEach( consumer );
	}

	@Override
	public void addInversePluralAssociationBinding(InversePluralAssociationBinding inversePluralAssociationBinding) {
		inversePluralAssociationBindings.add( inversePluralAssociationBinding );
	}

	@Override
	public void forEachInversePluralAssociationBinding(java.util.function.Consumer<InversePluralAssociationBinding> consumer) {
		inversePluralAssociationBindings.forEach( consumer );
	}

	private String resolveSchemaName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.getCanonicalName();
		}

		var defaultNamespace = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getDefaultNamespace();
		if ( defaultNamespace != null ) {
			final Identifier defaultSchemaName = defaultNamespace.getName().getSchema();
			if ( defaultSchemaName != null ) {
				return defaultSchemaName.getCanonicalName();
			}
		}
		return null;
	}

	private String resolveCatalogName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.getCanonicalName();
		}

		var defaultNamespace = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getDefaultNamespace();
		if ( defaultNamespace != null ) {
			final Identifier defaultCatalogName = defaultNamespace.getName().getCatalog();
			if ( defaultCatalogName != null ) {
				return defaultCatalogName.getCanonicalName();
			}
		}
		return null;

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Filter def

	@Override
	public void apply(FilterDefRegistration registration) {
		metadataBuildingContext.getMetadataCollector().addFilterDefinition( new FilterDefinition(
				registration.name(),
				registration.defaultCondition(),
				extractParameterMap( registration )
		) );
	}

	private Map<String, JdbcMapping> extractParameterMap(FilterDefRegistration registration) {
		final Map<String, ClassDetails> parameters = registration.parameters();
		if ( CollectionHelper.isEmpty( parameters ) ) {
			return Collections.emptyMap();
		}

		final TypeConfiguration typeConfiguration = metadataBuildingContext.getBootstrapContext().getTypeConfiguration();
		final Map<String, JdbcMapping> result = new HashMap<>();
		parameters.forEach( (name, typeDetails) -> {
			result.put( name, typeConfiguration.getBasicTypeForJavaType( typeDetails.toJavaClass() ) );
		} );
		return result;
	}
}
