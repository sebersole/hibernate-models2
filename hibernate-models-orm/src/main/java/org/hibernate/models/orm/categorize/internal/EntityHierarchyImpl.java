/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.models.orm.JpaAnnotations;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.CacheRegion;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.IdMapping;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.orm.categorize.spi.NaturalIdCacheRegion;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;

import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.hibernate.models.orm.categorize.ModelCategorizationLogging.MODEL_CATEGORIZATION_LOGGER;

/**
 *
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements EntityHierarchy {
	private final EntityTypeMetadata rootEntityTypeMetadata;

	private final InheritanceType inheritanceType;
	private final OptimisticLockStyle optimisticLockStyle;

	private final IdMapping idMapping;
	private final AttributeMetadata versionAttribute;
	private final AttributeMetadata tenantIdAttribute;

	private final CacheRegion cacheRegion;
	private final NaturalIdCacheRegion naturalIdCacheRegion;

	public EntityHierarchyImpl(
			ClassDetails rootEntityClassDetails,
			jakarta.persistence.AccessType defaultAccessType,
			AccessType defaultCacheAccessType,
			Consumer<IdentifiableTypeMetadata> typeConsumer,
			ModelCategorizationContext modelBuildingContext) {
		// NOTE : because the super-type consumer is "walked up" the
		//		inheritance tree, we want to collect the first usages
		//		which would be the "closest" usage
		final HierarchyMetadataCollector metadataCollector = new HierarchyMetadataCollector( this, modelBuildingContext );
		this.rootEntityTypeMetadata = new EntityTypeMetadataImpl(
				rootEntityClassDetails,
				this,
				defaultAccessType,
				metadataCollector,
				typeConsumer,
				modelBuildingContext
		);

		this.inheritanceType = determineInheritanceType( metadataCollector );
		this.optimisticLockStyle = determineOptimisticLockStyle( metadataCollector );

		this.idMapping = metadataCollector.getIdMapping();
		this.versionAttribute = metadataCollector.getVersionAttribute();
		this.tenantIdAttribute = metadataCollector.getTenantIdAttribute();

		this.cacheRegion = determineCacheRegion( metadataCollector, defaultCacheAccessType );
		this.naturalIdCacheRegion = determineNaturalIdCacheRegion( metadataCollector, cacheRegion );
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
	public IdMapping getIdMapping() {
		return idMapping;
	}

	@Override
	public AttributeMetadata getVersionAttribute() {
		return versionAttribute;
	}

	@Override
	public AttributeMetadata getTenantIdAttribute() {
		return tenantIdAttribute;
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

	private InheritanceType determineInheritanceType(HierarchyMetadataCollector metadataCollector) {
		if ( MODEL_CATEGORIZATION_LOGGER.isDebugEnabled() ) {
			// Validate that there is no @Inheritance annotation further down the hierarchy
			ensureNoInheritanceAnnotationsOnSubclasses( rootEntityTypeMetadata );
		}

		final AnnotationUsage<Inheritance> inheritanceAnnotation = metadataCollector.getInheritanceAnnotation();
		if ( inheritanceAnnotation != null ) {
			return inheritanceAnnotation.getAttributeValue( "strategy" );
		}

		return InheritanceType.SINGLE_TABLE;
	}

	private OptimisticLockStyle determineOptimisticLockStyle(HierarchyMetadataCollector metadataCollector) {
		final AnnotationUsage<OptimisticLocking> optimisticLockingAnnotation = metadataCollector.getOptimisticLockingAnnotation();
		if ( optimisticLockingAnnotation != null ) {
			optimisticLockingAnnotation.getEnum( "type", DEFAULT_LOCKING_STRATEGY );
		}
		return DEFAULT_LOCKING_STRATEGY;
	}

	private CacheRegion determineCacheRegion(
			HierarchyMetadataCollector metadataCollector,
			AccessType defaultCacheAccessType) {
		final AnnotationUsage<Cache> cacheAnnotation = metadataCollector.getCacheAnnotation();
		return new CacheRegion( cacheAnnotation, defaultCacheAccessType, rootEntityTypeMetadata.getEntityName() );
	}

	private NaturalIdCacheRegion determineNaturalIdCacheRegion(
			HierarchyMetadataCollector metadataCollector,
			CacheRegion cacheRegion) {
		final AnnotationUsage<NaturalIdCache> naturalIdCacheAnnotation = metadataCollector.getNaturalIdCacheAnnotation();
		return new NaturalIdCacheRegion( naturalIdCacheAnnotation, cacheRegion );
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
				MODEL_CATEGORIZATION_LOGGER.debugf(
						"@javax.persistence.Inheritance was specified on non-root entity [%s]; ignoring...",
						type.getClassDetails().getName()
				);
			}
			ensureNoInheritanceAnnotationsOnSubclasses( subType );
		} );
	}

}
