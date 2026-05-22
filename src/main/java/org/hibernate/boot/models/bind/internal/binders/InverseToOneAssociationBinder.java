/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;

/**
 * Resolves inverse to-one associations from their owning-side mapping values.
 */
class InverseToOneAssociationBinder {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;

	InverseToOneAssociationBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
	}

	void bindInverseAssociations() {
		bindingState.forEachInverseToOneAssociationBinding( (inverseBinding) -> {
			if ( inverseBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				bindInverseOneToOne( inverseBinding );
			}
		} );
	}

	private void bindInverseOneToOne(InverseToOneAssociationBinding inverseBinding) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				inverseBinding.targetClassDetails()
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for inverse to-one association target entity - "
							+ inverseBinding.targetClassDetails().getClassName()
			);
		}

		final Property owningProperty = targetTypeBinder.getTypeBinding().getProperty( inverseBinding.mappedBy() );
		final Value owningValue = owningProperty.getValue();
		if ( !( owningValue instanceof ManyToOne owningToOne ) ) {
			throw new MappingException(
					"Inverse @OneToOne mappedBy did not name an owning to-one attribute - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( !owningToOne.isLogicalOneToOne() ) {
			throw new MappingException(
					"Inverse @OneToOne mappedBy did not name an owning one-to-one attribute - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( !inverseBinding.ownerBinding().getEntityName().equals( owningToOne.getReferencedEntityName() ) ) {
			throw new MappingException(
					"Inverse @OneToOne mappedBy named a to-one attribute that targets `"
							+ owningToOne.getReferencedEntityName() + "` rather than `"
							+ inverseBinding.ownerBinding().getEntityName() + "` - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( owningToOne.getTable() != targetTypeBinder.getTable() ) {
			throw new UnsupportedOperationException(
					"Inverse @OneToOne mappedBy through @JoinTable is not yet implemented"
			);
		}

		inverseBinding.value().setReferencedPropertyName( inverseBinding.mappedBy() );
		inverseBinding.value().setReferenceToPrimaryKey( false );
		bindingState.getMetadataBuildingContext().getMetadataCollector()
				.addUniquePropertyReference( inverseBinding.value().getReferencedEntityName(), inverseBinding.mappedBy() );
		inverseBinding.value().sortProperties();
	}
}
