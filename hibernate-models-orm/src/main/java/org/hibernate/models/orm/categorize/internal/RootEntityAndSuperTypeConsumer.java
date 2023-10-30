/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import java.util.function.Consumer;

import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.source.spi.ClassDetails;

import jakarta.persistence.AccessType;

/**
 * Callback for types as the root of an entity hierarchy is processed.  It is
 * called for the root itself and then each of its super-types in "ascending" order
 *
 * @see AbstractIdentifiableTypeMetadata#AbstractIdentifiableTypeMetadata(ClassDetails, EntityHierarchy, boolean, AccessType, RootEntityAndSuperTypeConsumer, Consumer, ModelCategorizationContext)
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface RootEntityAndSuperTypeConsumer {
	/**
	 * Callback for the root type or one of its supers
	 */
	void acceptTypeOrSuperType(IdentifiableTypeMetadata superType);
}
