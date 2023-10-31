/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.spi;

import org.hibernate.engine.OptimisticLockStyle;

import jakarta.persistence.InheritanceType;

/**
 * Models an entity hierarchy comprised of {@linkplain EntityTypeMetadata entity}
 * and {@linkplain MappedSuperclassTypeMetadata mapped-superclass} types.
 *
 * @author Steve Ebersole
 */
public interface EntityHierarchy {
	/**
	 * The hierarchy's root type.
	 */
	EntityTypeMetadata getRoot();

	/**
	 * The absolute root of the hierarchy, which might be a mapped-superclass
	 * above the {@linkplain #getRoot() root entity}
	 */
	IdentifiableTypeMetadata getRootRoot();

	/**
	 * The inheritance strategy for the hierarchy.
	 */
	InheritanceType getInheritanceType();

	IdMapping getIdMapping();

	AttributeMetadata getVersionAttribute();

	AttributeMetadata getTenantIdAttribute();

	/**
	 * Style of optimistic locking for the hierarchy.
	 */
	OptimisticLockStyle getOptimisticLockStyle();

	/**
	 * The caching configuration for entities in this hierarchy.
	 */
	CacheRegion getCacheRegion();

	/**
	 * The caching configuration for this hierarchy's {@linkplain org.hibernate.annotations.NaturalId natural-id}
	 */
	NaturalIdCacheRegion getNaturalIdCacheRegion();
}
