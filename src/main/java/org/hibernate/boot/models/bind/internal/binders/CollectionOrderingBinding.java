/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.internal.sources.CollectionSource;
import org.hibernate.mapping.Collection;

/// Pending JPA `@OrderBy` binding for a collection.
///
/// Unlike Hibernate `@SQLOrder`, JPA `@OrderBy` is expressed in terms of element
/// property names, with an empty value meaning the target entity identifier.  The
/// collection member can be discovered before the element entity/component members
/// are fully bound, so this record carries the source facts into a later phase
/// where property paths can be translated to physical column fragments.
///
/// @author Steve Ebersole
public record CollectionOrderingBinding(
		Collection collection,
		CollectionSource source,
		String orderBy) {
}
