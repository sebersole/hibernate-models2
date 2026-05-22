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

	/// Bind discriminator, version, tenant id, and attributes.
	interface Members {
		void bindMembers();
	}
}
