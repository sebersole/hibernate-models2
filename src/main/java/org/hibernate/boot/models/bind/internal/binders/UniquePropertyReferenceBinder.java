/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.ToOne;

/// Centralizes the collector side-channel for associations that reference a unique property.
///
/// @author Steve Ebersole
class UniquePropertyReferenceBinder {
	static void bindUniquePropertyReference(
			BindingState bindingState,
			ToOne value,
			String referencedPropertyName) {
		value.setReferencedPropertyName( referencedPropertyName );
		value.setReferenceToPrimaryKey( false );
		bindingState.getMetadataBuildingContext().getMetadataCollector()
				.addUniquePropertyReference( value.getReferencedEntityName(), referencedPropertyName );
	}
}
