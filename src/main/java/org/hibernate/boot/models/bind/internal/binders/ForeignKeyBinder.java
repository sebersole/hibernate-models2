/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ManyToOne;

/// Creates physical foreign-key constraints for pending association values.
///
/// @author Steve Ebersole
class ForeignKeyBinder {
	private final EntityTypeBinder entityBinder;

	ForeignKeyBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
	}

	void bindForeignKeys() {
		entityBinder.getBindingState().forEachForeignKeyBinding( (foreignKeyBinding) -> {
			if ( foreignKeyBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				bindForeignKey( foreignKeyBinding );
			}
		} );
	}

	private void bindForeignKey(ForeignKeyBinding foreignKeyBinding) {
		final ManyToOne value = foreignKeyBinding.value();
		if ( value.isReferenceToPrimaryKey() ) {
			value.createForeignKey();
		}
		else {
			value.createPropertyRefConstraints(
					entityBinder.getBindingState().getMetadataBuildingContext()
							.getMetadataCollector()
							.getEntityBindingMap()
			);
		}
		applyForeignKeySource( value, foreignKeyBinding.foreignKeySource() );
	}

	private void applyForeignKeySource(ManyToOne value, ForeignKeySource foreignKeySource) {
		if ( foreignKeySource == null ) {
			return;
		}

		final ForeignKey foreignKey = findForeignKey( value );
		if ( foreignKey == null ) {
			return;
		}
		if ( foreignKeySource.isNoConstraint() ) {
			foreignKey.disableCreation();
		}
		if ( StringHelper.isNotEmpty( foreignKeySource.name() ) ) {
			foreignKey.setName( foreignKeySource.name() );
		}
		if ( StringHelper.isNotEmpty( foreignKeySource.definition() ) ) {
			foreignKey.setKeyDefinition( foreignKeySource.definition() );
		}
		if ( StringHelper.isNotEmpty( foreignKeySource.options() ) ) {
			foreignKey.setOptions( foreignKeySource.options() );
		}
	}

	private ForeignKey findForeignKey(ManyToOne value) {
		for ( ForeignKey foreignKey : value.getTable().getForeignKeyCollection() ) {
			if ( value.getReferencedEntityName().equals( foreignKey.getReferencedEntityName() ) ) {
				return foreignKey;
			}
		}
		return null;
	}
}
