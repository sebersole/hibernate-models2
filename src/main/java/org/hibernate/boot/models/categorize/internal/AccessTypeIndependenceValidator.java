/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.boot.models.AccessTypeIndependenceException;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

/**
 * Validates the portable shape requirement from Jakarta Persistence 2.3.4.
 *
 * @author Steve Ebersole
 */
public class AccessTypeIndependenceValidator {
	private final Map<String, PersistentAttributeShape> shapes = new LinkedHashMap<>();

	public void validate(
			ClassDetails managedClass,
			AccessType accessType,
			Collection<AttributeMetadata> attributes) {
		if ( managedClass.hasDirectAnnotationUsage( Access.class ) ) {
			return;
		}

		final PersistentAttributeShape shape = PersistentAttributeShape.from( attributes );
		final PersistentAttributeShape previousShape = shapes.putIfAbsent( managedClass.getName(), shape );
		if ( previousShape != null && !previousShape.equals( shape ) ) {
			throw new AccessTypeIndependenceException(
					managedClass,
					accessType,
					previousShape.toString(),
					shape.toString()
			);
		}
	}

	private record PersistentAttributeShape(Map<String, String> attributeTypes) {
		private static PersistentAttributeShape from(Collection<AttributeMetadata> attributes) {
			final LinkedHashMap<String, String> attributeTypes = new LinkedHashMap<>();
			for ( AttributeMetadata attribute : attributes ) {
				final MemberDetails member = attribute.getMember();
				attributeTypes.put( attribute.getName(), member.getType().determineRawClass().getClassName() );
			}
			return new PersistentAttributeShape( attributeTypes );
		}

		@Override @NonNull
		public String toString() {
			final StringJoiner joiner = new StringJoiner( ", " );
			attributeTypes.forEach( (name, type) -> joiner.add( name + ":" + type ) );
			return joiner.toString();
		}
	}
}
