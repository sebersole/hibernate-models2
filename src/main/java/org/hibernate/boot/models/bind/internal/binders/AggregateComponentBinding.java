/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;

/// Pending aggregate-component work recorded while binding component members.
///
/// Aggregate components need all nested component properties and columns to be
/// known before dialect-specific aggregate read/write expressions and UDT
/// structures can be finalized.  This record keeps that dependency typed rather
/// than registering an opaque callback-style second pass.
///
/// @author Steve Ebersole
public record AggregateComponentBinding(
		PersistentClass ownerBinding,
		Component component,
		ClassDetails componentClassDetails,
		String propertyPath,
		Table table) {
}
