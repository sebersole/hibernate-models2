/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.bind.internal.SecondaryTable;
import org.hibernate.boot.models.bind.internal.binders.AssociationTargetBinding;
import org.hibernate.boot.models.bind.internal.binders.AssociationIdentifierBinding;
import org.hibernate.boot.models.bind.internal.binders.AssociationTableBinding;
import org.hibernate.boot.models.bind.internal.binders.CollectionTableBinding;
import org.hibernate.boot.models.bind.internal.binders.DerivedIdentifierBinding;
import org.hibernate.boot.models.bind.internal.binders.ForeignKeyBinding;
import org.hibernate.boot.models.bind.internal.binders.IdentifierBinding;
import org.hibernate.boot.models.bind.internal.binders.IdentifiableTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.InversePluralAssociationBinding;
import org.hibernate.boot.models.bind.internal.binders.InverseToOneAssociationBinding;
import org.hibernate.boot.models.bind.internal.binders.ManagedTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.PropertyMapKeyBinding;
import org.hibernate.boot.models.bind.internal.binders.TableForeignKeyBinding;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.mapping.Join;
import org.hibernate.models.spi.ClassDetails;

/// Mutable state shared by binders while producing Hibernate's boot-time mapping
/// model.
///
/// The role is similar to upstream {@code InFlightMetadataCollector}: it provides
/// access to boot services and tracks the mapping objects, table references, and
/// type binders created during the binding phase.  Improvements made here should
/// generally be considered for the upstream collector when this work is integrated.
///
/// @author Steve Ebersole
public interface BindingState {
	/// Metadata building context for the current boot run.
	MetadataBuildingContext getMetadataBuildingContext();

	/// Database model being populated during binding.
	default Database getDatabase() {
		return getMetadataBuildingContext().getMetadataCollector().getDatabase();
	}

	/// JDBC services used for dialect and identifier handling.
	JdbcServices getJdbcServices();

	/// Apply a categorized global filter definition to the mapping model.
	void apply(FilterDefRegistration registration);

	/// Number of table references currently known to the binding state.
	int getTableCount();

	/// Visit each known table reference keyed by its binding-state name.
	void forEachTable(KeyedConsumer<String,TableReference> consumer);

	/// Resolve a table reference by binding-state name.
	<T extends TableReference> T getTableByName(String name);

	/// Resolve the table reference owned by the given model object.
	<T extends TableReference> T getTableByOwner(TableOwner owner);

	/// Register a table reference for the given owner.
	void addTable(TableOwner owner, TableReference table);

	/// Register a secondary table reference.
	void addSecondaryTable(SecondaryTable table);

	/// Resolve secondary-table state by its Hibernate table binding.
	SecondaryTable getSecondaryTable(org.hibernate.mapping.Table table);

	/// Register a Join that represents an association table.
	void addAssociationTableBinding(AssociationTableBinding associationTableBinding);

	/// Resolve association-table state for a Join, if the Join represents one.
	AssociationTableBinding getAssociationTableBinding(Join join);

	/// Register a collection table whose key is bound after member binding.
	void addCollectionTableBinding(CollectionTableBinding collectionTableBinding);

	/// Visit registered collection table bindings.
	void forEachCollectionTableBinding(java.util.function.Consumer<CollectionTableBinding> consumer);

	/// Register a property-derived map key to resolve after all members are bound.
	void addPropertyMapKeyBinding(PropertyMapKeyBinding propertyMapKeyBinding);

	/// Visit property-derived map keys waiting for collection-index binding.
	void forEachPropertyMapKeyBinding(java.util.function.Consumer<PropertyMapKeyBinding> consumer);

	/// Register an association-valued identifier attribute to resolve after identifiers.
	void addAssociationIdentifierBinding(AssociationIdentifierBinding associationIdentifierBinding);

	/// Visit association-valued identifier attributes waiting for late binding.
	void forEachAssociationIdentifierBinding(java.util.function.Consumer<AssociationIdentifierBinding> consumer);

	/// Register a non-primary-key association target to resolve after members are bound.
	void addAssociationTargetBinding(AssociationTargetBinding associationTargetBinding);

	/// Visit non-primary-key association targets waiting for late binding.
	void forEachAssociationTargetBinding(java.util.function.Consumer<AssociationTargetBinding> consumer);

	/// Register a derived identifier association to resolve after member binding.
	void addDerivedIdentifierBinding(DerivedIdentifierBinding derivedIdentifierBinding);

	/// Visit derived identifier associations waiting for late binding.
	void forEachDerivedIdentifierBinding(java.util.function.Consumer<DerivedIdentifierBinding> consumer);

	/// Register an inverse plural association to resolve after owning collection keys exist.
	void addInversePluralAssociationBinding(InversePluralAssociationBinding inversePluralAssociationBinding);

	/// Visit inverse plural association bindings waiting for owning-side resolution.
	void forEachInversePluralAssociationBinding(java.util.function.Consumer<InversePluralAssociationBinding> consumer);

	/// Register an inverse to-one association to resolve after all members exist.
	void addInverseToOneAssociationBinding(InverseToOneAssociationBinding inverseToOneAssociationBinding);

	/// Visit inverse to-one association bindings waiting for owning-side resolution.
	void forEachInverseToOneAssociationBinding(java.util.function.Consumer<InverseToOneAssociationBinding> consumer);

	/// Register a foreign-key constraint to create after table keys are bound.
	void addForeignKeyBinding(ForeignKeyBinding foreignKeyBinding);

	/// Visit foreign-key constraints waiting for late binding.
	void forEachForeignKeyBinding(java.util.function.Consumer<ForeignKeyBinding> consumer);

	/// Register a table-key foreign-key constraint to create after table keys are bound.
	void addTableForeignKeyBinding(TableForeignKeyBinding tableForeignKeyBinding);

	/// Visit table-key foreign-key constraints waiting for late binding.
	void forEachTableForeignKeyBinding(java.util.function.Consumer<TableForeignKeyBinding> consumer);

	/// Register the identifier binding produced for an entity hierarchy root.
	void addIdentifierBinding(EntityTypeMetadata rootType, IdentifierBinding identifierBinding);

	/// Resolve the identifier binding for an entity hierarchy root.
	IdentifierBinding getIdentifierBinding(EntityTypeMetadata rootType);


	/// Register the binder responsible for a categorized managed type.
	void registerTypeBinder(ManagedTypeMetadata type, ManagedTypeBinder binder);

	/// Resolve the binder responsible for a categorized managed type.
	default ManagedTypeBinder getTypeBinder(ManagedTypeMetadata type) {
		return getTypeBinder( type.getClassDetails() );
	}

	/// Resolve the binder responsible for the given managed class.
	ManagedTypeBinder getTypeBinder(ClassDetails type);

	/// Resolve the identifiable-type binder for the super type of the given class.
	IdentifiableTypeBinder getSuperTypeBinder(ClassDetails type);

	/// Visit each registered managed-type binder keyed by type name.
	void forEachType(KeyedConsumer<String,ManagedTypeBinder> consumer);

}
