/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.mapping.JaxbCollectionUserTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbCompositeUserTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbConverter;
import org.hibernate.boot.jaxb.mapping.JaxbConverterRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddable;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddableInstantiatorRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.JaxbJavaTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbJdbcTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.JaxbNamedNativeQuery;
import org.hibernate.boot.jaxb.mapping.JaxbNamedQuery;
import org.hibernate.boot.jaxb.mapping.JaxbNamedStoredProcedureQuery;
import org.hibernate.boot.jaxb.mapping.JaxbUserTypeRegistration;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.models.orm.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.orm.xml.spi.XmlDocument;

import jakarta.persistence.AccessType;

import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * @author Steve Ebersole
 */
public class XmlDocumentImpl implements XmlDocument {
	private final DefaultsImpl defaults;
	private final List<JaxbEntity> entityMappings;
	private final List<JaxbMappedSuperclass> mappedSuperclassMappings;
	private final List<JaxbEmbeddable> embeddableMappings;
	private final List<JaxbConverter> converters;
	private final List<JaxbConverterRegistration> converterRegistrations;
	private final List<JaxbJavaTypeRegistration> javaTypeRegistrations;
	private final List<JaxbJdbcTypeRegistration> jdbcTypeRegistrations;
	private final List<JaxbUserTypeRegistration> userTypeRegistrations;
	private final List<JaxbCompositeUserTypeRegistration> compositeUserTypeRegistrations;
	private final List<JaxbCollectionUserTypeRegistration> collectionUserTypeRegistrations;
	private final List<JaxbEmbeddableInstantiatorRegistration> embeddableInstantiatorRegistrations;
	private final Map<String, JaxbNamedQuery> jpaNamedQueries;
	private final Map<String, JaxbNamedNativeQuery> jpaNamedNativeQueries;
	private final Map<String, JaxbHbmNamedQueryType> hibernateNamedQueries;
	private final Map<String, JaxbHbmNamedNativeQueryType> hibernateNamedNativeQueries;
	private final Map<String, JaxbNamedStoredProcedureQuery> namedStoredProcedureQueries;

	private XmlDocumentImpl(
			DefaultsImpl defaults,
			List<JaxbEntity> entityMappings,
			List<JaxbMappedSuperclass> mappedSuperclassMappings,
			List<JaxbEmbeddable> embeddableMappings,
			List<JaxbConverter> converters,
			List<JaxbConverterRegistration> converterRegistrations,
			List<JaxbJavaTypeRegistration> javaTypeRegistrations,
			List<JaxbJdbcTypeRegistration> jdbcTypeRegistrations,
			List<JaxbUserTypeRegistration> userTypeRegistrations,
			List<JaxbCompositeUserTypeRegistration> compositeUserTypeRegistrations,
			List<JaxbCollectionUserTypeRegistration> collectionUserTypeRegistrations,
			List<JaxbEmbeddableInstantiatorRegistration> embeddableInstantiatorRegistrations,
			Map<String, JaxbNamedQuery> jpaNamedQueries,
			Map<String, JaxbNamedNativeQuery> jpaNamedNativeQueries,
			Map<String, JaxbNamedStoredProcedureQuery> namedStoredProcedureQueries,
			Map<String, JaxbHbmNamedQueryType> hibernateNamedQueries,
			Map<String, JaxbHbmNamedNativeQueryType> hibernateNamedNativeQueries) {
		this.defaults = defaults;
		this.entityMappings = entityMappings;
		this.mappedSuperclassMappings = mappedSuperclassMappings;
		this.embeddableMappings = embeddableMappings;
		this.converters = converters;
		this.converterRegistrations = converterRegistrations;
		this.javaTypeRegistrations = javaTypeRegistrations;
		this.jdbcTypeRegistrations = jdbcTypeRegistrations;
		this.userTypeRegistrations = userTypeRegistrations;
		this.compositeUserTypeRegistrations = compositeUserTypeRegistrations;
		this.collectionUserTypeRegistrations = collectionUserTypeRegistrations;
		this.embeddableInstantiatorRegistrations = embeddableInstantiatorRegistrations;
		this.jpaNamedQueries = jpaNamedQueries;
		this.jpaNamedNativeQueries = jpaNamedNativeQueries;
		this.namedStoredProcedureQueries = namedStoredProcedureQueries;
		this.hibernateNamedQueries = hibernateNamedQueries;
		this.hibernateNamedNativeQueries = hibernateNamedNativeQueries;
	}

	@Override
	public Defaults getDefaults() {
		return defaults;
	}

	@Override
	public List<JaxbEntity> getEntityMappings() {
		return entityMappings;
	}

	@Override
	public List<JaxbMappedSuperclass> getMappedSuperclassMappings() {
		return mappedSuperclassMappings;
	}

	@Override
	public List<JaxbEmbeddable> getEmbeddableMappings() {
		return embeddableMappings;
	}

	@Override
	public List<JaxbConverter> getConverters() {
		return converters;
	}

	@Override
	public List<JaxbConverterRegistration> getConverterRegistrations() {
		return converterRegistrations;
	}

	@Override
	public List<JaxbJavaTypeRegistration> getJavaTypeRegistrations() {
		return javaTypeRegistrations;
	}

	@Override
	public List<JaxbJdbcTypeRegistration> getJdbcTypeRegistrations() {
		return jdbcTypeRegistrations;
	}

	@Override
	public List<JaxbUserTypeRegistration> getUserTypeRegistrations() {
		return userTypeRegistrations;
	}

	@Override
	public List<JaxbCompositeUserTypeRegistration> getCompositeUserTypeRegistrations() {
		return compositeUserTypeRegistrations;
	}

	@Override
	public List<JaxbCollectionUserTypeRegistration> getCollectionUserTypeRegistrations() {
		return collectionUserTypeRegistrations;
	}

	@Override
	public List<JaxbEmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations() {
		return embeddableInstantiatorRegistrations;
	}

	@Override
	public Map<String, JaxbNamedQuery> getJpaNamedQueries() {
		return jpaNamedQueries;
	}

	@Override
	public Map<String, JaxbNamedNativeQuery> getJpaNamedNativeQueries() {
		return jpaNamedNativeQueries;
	}

	@Override
	public Map<String, JaxbHbmNamedQueryType> getHibernateNamedQueries() {
		return hibernateNamedQueries;
	}

	@Override
	public Map<String, JaxbHbmNamedNativeQueryType> getHibernateNamedNativeQueries() {
		return hibernateNamedNativeQueries;
	}

	@Override
	public Map<String, JaxbNamedStoredProcedureQuery> getNamedStoredProcedureQueries() {
		return namedStoredProcedureQueries;
	}

	private static class DefaultsImpl implements Defaults {
		private final String pckg;
		private final AccessType accessType;
		private final String accessorStrategy;
		private final String catalog;
		private final String schema;
		private final boolean autoImport;
		private final boolean impliedLaziness;

		private DefaultsImpl(
				String pckg,
				AccessType accessType,
				String accessorStrategy,
				String catalog,
				String schema,
				Boolean autoImport,
				Boolean impliedLaziness) {
			this.pckg = pckg;
			this.accessType = accessType;
			this.accessorStrategy = accessorStrategy;
			this.catalog = catalog;
			this.schema = schema;
			this.autoImport = NullnessHelper.nullif( autoImport, true );
			this.impliedLaziness = NullnessHelper.nullif( impliedLaziness, false );
		}

		@Override
		public String getPackage() {
			return pckg;
		}

		@Override
		public AccessType getAccessType() {
			return accessType;
		}

		@Override
		public String getAccessorStrategy() {
			return accessorStrategy;
		}

		@Override
		public String getCatalog() {
			return catalog;
		}

		@Override
		public String getSchema() {
			return schema;
		}

		@Override
		public boolean isAutoImport() {
			return autoImport;
		}

		@Override
		public boolean isLazinessImplied() {
			return impliedLaziness;
		}

		static DefaultsImpl consume(JaxbEntityMappings jaxbRoot, PersistenceUnitMetadata metadata) {
			return new DefaultsImpl(
					jaxbRoot.getPackage(),
					NullnessHelper.coalesce( jaxbRoot.getAccess(), metadata.getAccessType() ),
					NullnessHelper.coalesce( jaxbRoot.getAttributeAccessor(), metadata.getDefaultAccessStrategyName() ),
					NullnessHelper.coalesce( jaxbRoot.getCatalog(), metadata.getDefaultCatalog() ),
					NullnessHelper.coalesce( jaxbRoot.getSchema(), metadata.getDefaultSchema() ),
					jaxbRoot.isAutoImport(),
					jaxbRoot.isDefaultLazy()
			);
		}
	}

	public static XmlDocumentImpl consume(JaxbEntityMappings jaxbRoot, PersistenceUnitMetadata metadata) {
		return new XmlDocumentImpl(
				DefaultsImpl.consume( jaxbRoot, metadata ),
				jaxbRoot.getEntities(),
				jaxbRoot.getMappedSuperclasses(),
				jaxbRoot.getEmbeddables(),
				jaxbRoot.getConverters(),
				jaxbRoot.getConverterRegistrations(),
				jaxbRoot.getJavaTypeRegistrations(),
				jaxbRoot.getJdbcTypeRegistrations(),
				jaxbRoot.getUserTypeRegistrations(),
				jaxbRoot.getCompositeUserTypeRegistrations(),
				jaxbRoot.getCollectionUserTypeRegistrations(),
				jaxbRoot.getEmbeddableInstantiatorRegistrations(),
				toNamedQueryMap( jaxbRoot.getNamedQueries() ),
				toNamedNativeQueryMap( jaxbRoot.getNamedNativeQueries() ),
				toNamedProcedureQueryMap( jaxbRoot.getNamedProcedureQueries() ),
				// not sure what's up with the Hibernate-specific named query nodes, but they are not in the root mapping
				Collections.emptyMap(),
				Collections.emptyMap()
		);
	}

	private static Map<String, JaxbNamedQuery> toNamedQueryMap(List<JaxbNamedQuery> namedQueries) {
		if ( isEmpty( namedQueries ) ) {
			return Collections.emptyMap();
		}

		final Map<String, JaxbNamedQuery> map = new HashMap<>();
		namedQueries.forEach( (query) -> map.put( query.getName(), query ) );
		return map;
	}

	private static Map<String, JaxbNamedNativeQuery> toNamedNativeQueryMap(List<JaxbNamedNativeQuery> namedQueries) {
		if ( isEmpty( namedQueries ) ) {
			return Collections.emptyMap();
		}

		final Map<String, JaxbNamedNativeQuery> map = new HashMap<>();
		namedQueries.forEach( (query) -> map.put( query.getName(), query ) );
		return map;
	}

	private static Map<String,JaxbNamedStoredProcedureQuery> toNamedProcedureQueryMap(List<JaxbNamedStoredProcedureQuery> namedQueries) {
		if ( isEmpty( namedQueries ) ) {
			return Collections.emptyMap();
		}

		final Map<String, JaxbNamedStoredProcedureQuery> map = new HashMap<>();
		namedQueries.forEach( (query) -> map.put( query.getName(), query ) );
		return map;
	}
}
