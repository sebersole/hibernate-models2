/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.models.orm.JpaAnnotations;
import org.hibernate.models.orm.spi.CacheRegion;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.orm.spi.EntityTypeMetadata;
import org.hibernate.models.orm.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.spi.NaturalIdCacheRegion;
import org.hibernate.models.orm.spi.OrmModelBuildingContext;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;

import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.hibernate.models.orm.internal.OrmModelLogging.ORM_MODEL_LOGGER;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements EntityHierarchy {
	private final EntityTypeMetadata rootEntityTypeMetadata;

	private final InheritanceType inheritanceType;
	private final OptimisticLockStyle optimisticLockStyle;

	private final CacheRegion cacheRegion;
	private final NaturalIdCacheRegion naturalIdCacheRegion;

	public EntityHierarchyImpl(
			ClassDetails rootEntityClassDetails,
			jakarta.persistence.AccessType defaultAccessType,
			AccessType defaultCacheAccessType,
			Consumer<IdentifiableTypeMetadata> typeConsumer,
			OrmModelBuildingContext modelBuildingContext) {
		this.rootEntityTypeMetadata = new EntityTypeMetadataImpl(
				rootEntityClassDetails,
				this,
				defaultAccessType,
				typeConsumer,
				modelBuildingContext
		);

		this.inheritanceType = determineInheritanceType( rootEntityTypeMetadata );
		this.optimisticLockStyle = determineOptimisticLockStyle( rootEntityTypeMetadata );

		final AnnotationUsage<Cache> cacheAnnotation = rootEntityClassDetails.getAnnotationUsage( Cache.class );
		final AnnotationUsage<NaturalIdCache> naturalIdCacheAnnotation = rootEntityClassDetails.getAnnotationUsage( NaturalIdCache.class );
		cacheRegion = new CacheRegion( cacheAnnotation, defaultCacheAccessType, rootEntityClassDetails.getName() );
		naturalIdCacheRegion = new NaturalIdCacheRegion( naturalIdCacheAnnotation, cacheRegion );
	}

	@Override
	public EntityTypeMetadata getRoot() {
		return rootEntityTypeMetadata;
	}

	@Override
	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return optimisticLockStyle;
	}

	@Override
	public CacheRegion getCacheRegion() {
		return cacheRegion;
	}

	@Override
	public NaturalIdCacheRegion getNaturalIdCacheRegion() {
		return naturalIdCacheRegion;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"EntityHierarchy(`%s` (%s))",
				rootEntityTypeMetadata.getEntityName(),
				inheritanceType.name()
		);
	}


	private static final OptimisticLockStyle DEFAULT_LOCKING_STRATEGY = OptimisticLockStyle.VERSION;

	private OptimisticLockStyle determineOptimisticLockStyle(EntityTypeMetadata rootEntityTypeMetadata) {
		final AnnotationUsage<OptimisticLocking> lockStrategyAnn = rootEntityTypeMetadata
				.getClassDetails()
				.getAnnotationUsage( OptimisticLocking.class );
		if ( lockStrategyAnn == null ) {
			return DEFAULT_LOCKING_STRATEGY;
		}
		return lockStrategyAnn.getEnum( "type", DEFAULT_LOCKING_STRATEGY );
	}

	private InheritanceType determineInheritanceType(EntityTypeMetadata root) {
		if ( ORM_MODEL_LOGGER.isDebugEnabled() ) {
			// Validate that there is no @Inheritance annotation further down the hierarchy
			ensureNoInheritanceAnnotationsOnSubclasses( root );
		}

		IdentifiableTypeMetadata current = root;
		while ( current != null ) {
			final InheritanceType inheritanceType = getLocallyDefinedInheritanceType( current.getClassDetails() );
			if ( inheritanceType != null ) {
				return inheritanceType;
			}

			current = current.getSuperType();
		}

		return InheritanceType.SINGLE_TABLE;
	}

	/**
	 * Find the InheritanceType from the locally defined {@link Inheritance} annotation,
	 * if one.  Returns {@code null} if {@link Inheritance} is not locally defined.
	 *
	 * @apiNote Used when building the {@link EntityHierarchy}
	 */
	private static InheritanceType getLocallyDefinedInheritanceType(ClassDetails managedClass) {
		final AnnotationUsage<Inheritance> localAnnotation = managedClass.getAnnotationUsage( JpaAnnotations.INHERITANCE );
		if ( localAnnotation == null ) {
			return null;
		}

		return localAnnotation.getAttributeValue( "strategy" );
	}

	private void ensureNoInheritanceAnnotationsOnSubclasses(IdentifiableTypeMetadata type) {
		type.forEachSubType( (subType) -> {
			if ( getLocallyDefinedInheritanceType( subType.getClassDetails() ) != null ) {
				ORM_MODEL_LOGGER.debugf(
						"@javax.persistence.Inheritance was specified on non-root entity [%s]; ignoring...",
						type.getClassDetails().getName()
				);
			}
			ensureNoInheritanceAnnotationsOnSubclasses( subType );
		} );
	}

}
