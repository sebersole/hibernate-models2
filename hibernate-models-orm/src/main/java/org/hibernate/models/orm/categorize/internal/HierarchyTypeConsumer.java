/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;

/**
 * Consumer of types as we walk the managed-type hierarchy
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface HierarchyTypeConsumer {
	void acceptType(IdentifiableTypeMetadata type);
}
