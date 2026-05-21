/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.sources;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.SecondaryTable;

/**
 * Unified source for foreign-key mapping metadata.
 *
 * @author Steve Ebersole
 */
public interface ForeignKeySource {
	static ForeignKeySource from(ForeignKey foreignKey) {
		return foreignKey == null ? null : new JpaForeignKeySource( foreignKey );
	}

	static ForeignKeySource from(JoinColumn joinColumn) {
		return joinColumn == null ? null : from( joinColumn.foreignKey() );
	}

	static ForeignKeySource from(JoinTable joinTable) {
		return joinTable == null ? null : from( joinTable.foreignKey() );
	}

	static ForeignKeySource inverseFrom(JoinTable joinTable) {
		return joinTable == null ? null : from( joinTable.inverseForeignKey() );
	}

	static ForeignKeySource from(SecondaryTable secondaryTable) {
		return secondaryTable == null ? null : from( secondaryTable.foreignKey() );
	}

	String name();

	ConstraintMode constraintMode();

	String definition();

	String options();

	default boolean isNoConstraint() {
		return constraintMode() == ConstraintMode.NO_CONSTRAINT;
	}

	record JpaForeignKeySource(ForeignKey foreignKey) implements ForeignKeySource {
		@Override
		public String name() {
			return foreignKey.name();
		}

		@Override
		public ConstraintMode constraintMode() {
			return foreignKey.value();
		}

		@Override
		public String definition() {
			return foreignKey.foreignKeyDefinition();
		}

		@Override
		public String options() {
			return foreignKey.options();
		}
	}
}
