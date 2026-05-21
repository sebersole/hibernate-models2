/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.mapping.Join;

import jakarta.persistence.JoinColumn;

/// Local state for an association table modeled as a Hibernate [Join].
///
/// @author Steve Ebersole
public record AssociationTableBinding(
		Join join,
		List<JoinColumn> joinColumns) {
}
