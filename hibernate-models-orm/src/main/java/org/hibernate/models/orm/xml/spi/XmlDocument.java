/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.spi;

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
import org.hibernate.boot.jaxb.mapping.JaxbJavaTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbJdbcTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.JaxbNamedNativeQuery;
import org.hibernate.boot.jaxb.mapping.JaxbNamedQuery;
import org.hibernate.boot.jaxb.mapping.JaxbNamedStoredProcedureQuery;
import org.hibernate.boot.jaxb.mapping.JaxbUserTypeRegistration;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public interface XmlDocument {
	List<JaxbEntity> getEntityMappings();

	List<JaxbMappedSuperclass> getMappedSuperclassMappings();

	List<JaxbEmbeddable> getEmbeddableMappings();

	List<JaxbConverter> getConverters();

	List<JaxbConverterRegistration> getConverterRegistrations();

	List<JaxbJavaTypeRegistration> getJavaTypeRegistrations();

	List<JaxbJdbcTypeRegistration> getJdbcTypeRegistrations();

	List<JaxbUserTypeRegistration> getUserTypeRegistrations();

	List<JaxbCompositeUserTypeRegistration> getCompositeUserTypeRegistrations();

	List<JaxbCollectionUserTypeRegistration> getCollectionUserTypeRegistrations();

	List<JaxbEmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations();

	Map<String, JaxbNamedQuery> getJpaNamedQueries();

	Map<String, JaxbNamedNativeQuery> getJpaNamedNativeQueries();

	Map<String, JaxbHbmNamedQueryType> getHibernateNamedQueries();

	Map<String, JaxbHbmNamedNativeQueryType> getHibernateNamedNativeQueries();

	Map<String, JaxbNamedStoredProcedureQuery> getNamedStoredProcedureQueries();

	interface Defaults {
		String getPackage();
		AccessType getAccessType();
		String getAccessorStrategy();
		String getCatalog();
		String getSchema();
		boolean isAutoImport();
		boolean isLazinessImplied();
	}

	Defaults getDefaults();


}
