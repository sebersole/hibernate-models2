/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.spi.ClassDetails;

/**
 * Local state for an inverse plural association whose physical table/key details
 * are resolved from the owning side after table keys have been bound.
 */
public record InversePluralAssociationBinding(
		Nature nature,
		IdentifiableTypeMetadata ownerType,
		PersistentClass ownerBinding,
		AttributeMetadata attributeMetadata,
		Collection collection,
		ClassDetails targetClassDetails,
		String mappedBy) {
	public enum Nature {
		MANY_TO_MANY,
		ONE_TO_MANY
	}
}
