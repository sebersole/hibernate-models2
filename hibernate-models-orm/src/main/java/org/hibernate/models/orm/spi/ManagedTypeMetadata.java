/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.util.Collection;

import org.hibernate.models.internal.IndexedConsumer;
import org.hibernate.models.source.spi.ClassDetails;

import jakarta.persistence.AccessType;

/**
 * Metadata about a {@linkplain jakarta.persistence.metamodel.ManagedType managed type}
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeMetadata {
	/**
	 * The underlying managed-class
	 */
	ClassDetails getClassDetails();

	/**
	 * The class-level access type
	 */
	AccessType getAccessType();

	/**
	 * Get the number of declared attributes
	 */
	int getNumberOfAttributes();

	/**
	 * Get the declared attributes
	 */
	Collection<AttributeMetadata> getAttributes();

	AttributeMetadata findAttribute(String name);

	/**
	 * Visit each declared attributes
	 */
	void forEachAttribute(IndexedConsumer<AttributeMetadata> consumer);
}
