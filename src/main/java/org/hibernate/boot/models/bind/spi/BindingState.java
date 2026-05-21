/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.bind.internal.SecondaryTable;
import org.hibernate.boot.models.bind.internal.binders.IdentifierBinding;
import org.hibernate.boot.models.bind.internal.binders.IdentifiableTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.ManagedTypeBinder;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.KeyedConsumer;
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
