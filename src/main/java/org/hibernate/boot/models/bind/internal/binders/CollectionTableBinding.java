/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.mapping.Collection;

import jakarta.persistence.JoinColumn;

/**
 * Local state for a collection table whose key depends on the owner identifier.
 */
public record CollectionTableBinding(
		Collection collection,
		List<JoinColumn> joinColumns) {
}
