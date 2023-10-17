/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.util.function.Consumer;

/**
 * Metadata about an {@linkplain jakarta.persistence.metamodel.IdentifiableType identifiable type}
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeMetadata extends ManagedTypeMetadata {
	/**
	 * The hierarchy in which this IdentifiableType occurs.
	 */
	EntityHierarchy getHierarchy();

	/**
	 * The super-type, if one
	 */

	IdentifiableTypeMetadata getSuperType();

	/**
	 * Whether this type is considered abstract.
	 */
	default boolean isAbstract() {
		return getClassDetails().isAbstract();
	}

	/**
	 * Whether this type has subtypes
	 */
	boolean hasSubTypes();

	/**
	 * Get the number of direct subtypes
	 */
	int getNumberOfSubTypes();

	/**
	 * Get the direct subtypes
	 */
	Iterable<IdentifiableTypeMetadata> getSubTypes();

	/**
	 * Visit each direct subtype
	 */
	void forEachSubType(Consumer<IdentifiableTypeMetadata> consumer);


//	List<CallbacksMetadata> getJpaCallbacks();
//	void forEachJpaCallback(Consumer<CallbacksMetadata> consumer);
}
