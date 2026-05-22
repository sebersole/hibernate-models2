/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

/// Narrow contracts for coordinator-driven type binding phases.
///
/// Each nested interface represents one phase a type binder may participate in.
/// Binders implement only the phases that apply to them, avoiding no-op phase
/// methods while still making the coordinator's ordering explicit.
///
/// @author Steve Ebersole
public interface TypeBindingPhase {
	/// Publish the minimal type skeleton so other binders can resolve it.
	interface TypeSkeleton {
		void bindTypeSkeleton();
	}

	/// Bind table shells owned by the participating type.
	interface Tables {
		void bindTables();
	}

	/// Wire the participating type to its resolved super type.
	interface SuperType {
		void bindSuperType();
	}

	/// Bind entity-level metadata that does not require member value binding.
	interface EntityMetadata {
		void bindEntityMetadata();
	}

	/// Bind the root identifier shape for an entity hierarchy.
	interface Identifiers {
		void bindIdentifier();
	}

	/// Resolve identifier attributes that are themselves associations after all
	/// root identifier shapes are available.
	interface AssociationIdentifiers {
		void bindAssociationIdentifiers();
	}

	/// Bind table keys that depend on the completed root identifier shape and
	/// table-valued members.
	///
	/// Examples include joined-subclass table keys, secondary-table join keys,
	/// and association-table join keys.
	interface TableKeys {
		void bindTableKeys();
	}

	/// Resolve inverse associations that depend on key/value state produced by
	/// owning-side association binding.
	interface InverseAssociations {
		void bindInverseAssociations();
	}

	/// Create and customize physical foreign-key constraints after association
	/// values, table keys, and inverse association structures are available.
	interface ForeignKeys {
		void bindForeignKeys();
	}

	/// Bind discriminator, version, tenant id, and attributes.
	interface Members {
		void bindMembers();
	}

	/// Resolve collection index values that depend on member bindings from
	/// another type.
	///
	/// For example, {@code @MapKey(name)} points at a property of the collection
	/// element type.  The collection member can be created before that property is
	/// bound, so this phase runs after all members and before table keys call
	/// collection key creation.
	interface CollectionIndexes {
		void bindCollectionIndexes();
	}

	/// Resolve JPA collection ordering fragments that depend on element property
	/// and identifier mappings.
	///
	/// Hibernate `@SQLOrder` can be applied immediately because it is already a
	/// SQL fragment.  JPA `@OrderBy` names element properties, and an empty value
	/// means the element entity identifier, so it is translated after members are
	/// available.
	interface CollectionOrderings {
		void bindCollectionOrderings();
	}

	/// Resolve association target properties for non-primary-key references.
	interface AssociationTargets {
		void bindAssociationTargets();
	}

	/// Resolve derived identifier associations such as {@code @MapsId} after
	/// owner identifiers and to-one members have both been created.
	interface DerivedIdentifiers {
		void bindDerivedIdentifiers();
	}
}
