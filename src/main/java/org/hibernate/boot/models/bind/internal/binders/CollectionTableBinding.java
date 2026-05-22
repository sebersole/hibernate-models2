/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.mapping.Collection;

import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.UniqueConstraint;

/// Local state for a collection table whose key depends on the owner identifier.
///
/// @author Steve Ebersole
public record CollectionTableBinding(
		Collection collection,
		List<JoinColumn> joinColumns,
		ForeignKeySource foreignKeySource,
		UniqueConstraint[] uniqueConstraints,
		Index[] indexes) {
}
