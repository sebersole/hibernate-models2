/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

/// Composite key mapping physically represented by one embeddable attribute.
///
/// @see jakarta.persistence.EmbeddedId
///
/// @author Steve Ebersole
public interface AggregatedKeyMapping extends CompositeKeyMapping, SingleAttributeKeyMapping {
}
