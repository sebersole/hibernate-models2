/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

/// Categorized metadata about a visible
/// {@linkplain jakarta.persistence.metamodel.MappedSuperclassType mapped-superclass}.
///
/// Only mapped-superclasses that participate in an entity hierarchy are represented
/// by this contract.  Other discovered mapped-superclasses remain available from
/// {@link CategorizedDomainModel#getMappedSuperclasses()} as class details.
///
/// @author Steve Ebersole
public interface MappedSuperclassTypeMetadata extends IdentifiableTypeMetadata {
	@Override
	default Kind getManagedTypeKind() {
		return Kind.MAPPED_SUPER;
	}
}
