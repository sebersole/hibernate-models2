/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.TenantId;
import org.hibernate.models.ModelsException;
import org.hibernate.models.orm.categorize.spi.IdMapping;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.MemberDetails;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Version;

import static org.hibernate.models.orm.categorize.ModelCategorizationLogging.MODEL_CATEGORIZATION_LOGGER;

/**
 * Used to collect useful details about a hierarchy as we build its metadata
 *
 * @author Steve Ebersole
 */
public class HierarchyMetadataCollector implements RootEntityAndSuperTypeConsumer {
	private final EntityHierarchy entityHierarchy;
	private final ModelCategorizationContext context;

	private AnnotationUsage<Inheritance> inheritanceAnnotation;
	private AnnotationUsage<OptimisticLocking> optimisticLockingAnnotation;
	private AnnotationUsage<Cache> cacheAnnotation;
	private AnnotationUsage<NaturalIdCache> naturalIdCacheAnnotation;

	private IdMapping idMapping;
	private AttributeMetadata versionAttribute;
	private AttributeMetadata tenantIdAttribute;

	private AnnotationUsage<IdClass> idClassAnnotation;
	private Object collectedIdAttributes;

	public HierarchyMetadataCollector(EntityHierarchy entityHierarchy, ModelCategorizationContext context) {
		this.entityHierarchy = entityHierarchy;
		this.context = context;
	}

	public IdMapping getIdMapping() {
		if ( idMapping == null ) {
			idMapping = buildIdMapping();
		}

		return idMapping;
	}

	public AnnotationUsage<Inheritance> getInheritanceAnnotation() {
		return inheritanceAnnotation;
	}

	public AnnotationUsage<OptimisticLocking> getOptimisticLockingAnnotation() {
		return optimisticLockingAnnotation;
	}

	public AnnotationUsage<Cache> getCacheAnnotation() {
		return cacheAnnotation;
	}

	public AnnotationUsage<NaturalIdCache> getNaturalIdCacheAnnotation() {
		return naturalIdCacheAnnotation;
	}

	public Object getCollectedIdAttributes() {
		return collectedIdAttributes;
	}

	private IdMapping buildIdMapping() {
		if ( collectedIdAttributes instanceof List ) {
			//noinspection unchecked
			final List<AttributeMetadata> idAttributes = (List<AttributeMetadata>) collectedIdAttributes;
			final ClassDetails idClassDetails;
			if ( idClassAnnotation == null ) {
				idClassDetails = null;
			}
			else {
				idClassDetails = idClassAnnotation.getAttributeValue( "value" );
			}
			return new NonAggregatedIdMappingImpl( idAttributes, idClassDetails );
		}

		final AttributeMetadata idAttribute = (AttributeMetadata) collectedIdAttributes;

		if ( idAttribute.getNature() == AttributeMetadata.AttributeNature.BASIC ) {
			return new BasicIdMappingImpl( idAttribute );
		}

		if ( idAttribute.getNature() == AttributeMetadata.AttributeNature.EMBEDDED ) {
			return new AggregatedIdMappingImpl( idAttribute );
		}

		throw new ModelsException(
				String.format(
						Locale.ROOT,
						"Unexpected attribute nature [%s] - %s",
						idAttribute.getNature(),
						entityHierarchy.getRoot().getEntityName()
				)
		);
	}

	public AttributeMetadata getVersionAttribute() {
		return versionAttribute;
	}

	public AttributeMetadata getTenantIdAttribute() {
		return tenantIdAttribute;
	}

	@Override
	public void acceptTypeOrSuperType(IdentifiableTypeMetadata typeMetadata) {
		assert idMapping == null;

		final ClassDetails classDetails = typeMetadata.getClassDetails();

		inheritanceAnnotation = applyLocalAnnotation( Inheritance.class, classDetails, inheritanceAnnotation );
		optimisticLockingAnnotation = applyLocalAnnotation( OptimisticLocking.class, classDetails, optimisticLockingAnnotation );
		cacheAnnotation = applyLocalAnnotation( Cache.class, classDetails, cacheAnnotation );
		naturalIdCacheAnnotation = applyLocalAnnotation( NaturalIdCache.class, classDetails, naturalIdCacheAnnotation );
		idClassAnnotation = applyLocalAnnotation( IdClass.class, classDetails, idClassAnnotation );

		final boolean collectIds = collectedIdAttributes == null;
		if ( collectIds || versionAttribute == null || tenantIdAttribute == null ) {
			// walk the attributes
			typeMetadata.forEachAttribute( (index, attributeMetadata) -> {
				final MemberDetails attributeMember = attributeMetadata.getMember();

				if ( collectIds ) {
					final AnnotationUsage<EmbeddedId> eIdAnn = attributeMember.getAnnotationUsage( EmbeddedId.class );
					if ( eIdAnn != null ) {
						collectIdAttribute( attributeMetadata );
					}

					final AnnotationUsage<Id> idAnn = attributeMember.getAnnotationUsage( Id.class );
					if ( idAnn != null ) {
						collectIdAttribute( attributeMetadata );
					}
				}

				if ( versionAttribute == null ) {
					if ( attributeMember.getAnnotationUsage( Version.class ) != null ) {
						versionAttribute = attributeMetadata;
					}
				}

				if ( tenantIdAttribute == null ) {
					if ( attributeMember.getAnnotationUsage( TenantId.class ) != null ) {
						tenantIdAttribute = attributeMetadata;
					}
				}
			} );
		}
	}

	private <A extends Annotation> AnnotationUsage<A> applyLocalAnnotation(Class<A> annotationType, ClassDetails classDetails, AnnotationUsage<A> currentValue) {
		final AnnotationUsage<A> localInheritanceAnnotation = classDetails.getAnnotationUsage( annotationType );
		if ( localInheritanceAnnotation != null ) {
			if ( currentValue != null ) {
				MODEL_CATEGORIZATION_LOGGER.debugf(
						"Ignoring @%s from %s in favor of usage from %s",
						annotationType.getSimpleName(),
						classDetails.getName(),
						currentValue.getAnnotationTarget().getName()
				);
			}
			else {
				return localInheritanceAnnotation;
			}
		}

		return currentValue;
	}

	public void collectIdAttribute(AttributeMetadata member) {
		assert member != null;

		if ( collectedIdAttributes == null ) {
			collectedIdAttributes = member;
		}
		else if ( collectedIdAttributes instanceof List ) {
			//noinspection unchecked,rawtypes
			final List<AttributeMetadata> membersList = (List) collectedIdAttributes;
			membersList.add( member );
		}
		else if ( collectedIdAttributes instanceof AttributeMetadata ) {
			final ArrayList<AttributeMetadata> combined = new ArrayList<>();
			combined.add( (AttributeMetadata) collectedIdAttributes );
			combined.add( member );
			collectedIdAttributes = combined;
		}
	}
}
