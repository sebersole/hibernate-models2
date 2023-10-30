/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.spi;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.ModelsException;
import org.hibernate.models.source.spi.AnnotationUsage;

/**
 * Models the caching options for an entity, natural-id, or collection.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class CacheRegion {
	private String regionName;
	private AccessType accessType;
	private boolean cacheLazyProperties;

	public CacheRegion(
			AnnotationUsage<Cache> cacheAnnotation,
			AccessType implicitCacheAccessType,
			String implicitRegionName) {
		if ( cacheAnnotation == null ) {
			regionName = implicitRegionName;
			accessType = implicitCacheAccessType;
			cacheLazyProperties = true;
		}
		else {
			final String explicitRegionName = cacheAnnotation.getString( "region" );
			regionName = StringHelper.isEmpty( explicitRegionName ) ? implicitRegionName : explicitRegionName;

			accessType = interpretAccessStrategy( cacheAnnotation.getAttributeValue( "usage" ) );

			final Boolean explicitIncludeLazy = cacheAnnotation.getBoolean( "includeLazy" );
			if ( explicitIncludeLazy != null ) {
				cacheLazyProperties = explicitIncludeLazy;
			}
			else {
				final String include = cacheAnnotation.getAttributeValue( "include" );
				assert "all".equals( include ) || "non-lazy".equals( include );
				cacheLazyProperties = include.equals( "all" );
			}
		}
	}

	private AccessType interpretAccessStrategy(CacheConcurrencyStrategy usage) {
		if ( usage == null ) {
			return null;
		}
		switch ( usage ) {
			case NONE: {
				return null;
			}
			case READ_ONLY: {
				return AccessType.READ_ONLY;
			}
			case READ_WRITE: {
				return AccessType.READ_WRITE;
			}
			case NONSTRICT_READ_WRITE: {
				return AccessType.NONSTRICT_READ_WRITE;
			}
			case TRANSACTIONAL: {
				return AccessType.TRANSACTIONAL;
			}
			default: {
				throw new ModelsException( "Unexpected cache concurrency strategy specified - " + usage );
			}
		}
	}

	public String getRegionName() {
		return regionName;
	}

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public AccessType getAccessType() {
		return accessType;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	public boolean isCacheLazyProperties() {
		return cacheLazyProperties;
	}

	public void setCacheLazyProperties(boolean cacheLazyProperties) {
		this.cacheLazyProperties = cacheLazyProperties;
	}

	public void overlay(CacheRegionDefinition overrides) {
		if ( overrides == null ) {
			return;
		}

		accessType = AccessType.fromExternalName( overrides.getUsage() );
		if ( StringHelper.isEmpty( overrides.getRegion() ) ) {
			regionName = overrides.getRegion();
		}
		// ugh, primitive boolean
		cacheLazyProperties = overrides.isCacheLazy();
	}

	public void overlay(CacheRegion overrides) {
		if ( overrides == null ) {
			return;
		}

		this.accessType = overrides.accessType;
		this.regionName = overrides.regionName;
		this.cacheLazyProperties = overrides.cacheLazyProperties;
	}

	@Override
	public String toString() {
		return "Caching{" +
				"region='" + regionName + '\''
				+ ", accessType=" + accessType
				+ ", cacheLazyProperties=" + cacheLazyProperties + '}';
	}

}
