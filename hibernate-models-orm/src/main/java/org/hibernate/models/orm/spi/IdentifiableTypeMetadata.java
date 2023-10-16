/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.util.function.Consumer;

import org.hibernate.models.source.spi.ClassDetails;

/**
 * Intermediate representation of an {@linkplain jakarta.persistence.metamodel.IdentifiableType identifiable type}
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


//	List<CallbacksMetadata> getJpaCallbacks();
//	void forEachJpaCallback(Consumer<CallbacksMetadata> consumer);
}
