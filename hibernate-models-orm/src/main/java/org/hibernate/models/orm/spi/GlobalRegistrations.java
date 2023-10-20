/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.annotations.EmbeddableInstantiatorRegistration;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.annotations.TypeRegistration;
import org.hibernate.models.orm.internal.NamedQueryRegistration;
import org.hibernate.models.source.spi.AnnotationUsage;

import jakarta.persistence.Converter;

/**
 * @author Steve Ebersole
 */
public interface GlobalRegistrations {

	List<AnnotationUsage<ConverterRegistration>> getConverterRegistrations();

	List<AnnotationUsage<Converter>> getAutoAppliedConverters();

	List<AnnotationUsage<JavaTypeRegistration>> getJavaTypeRegistrations();

	List<AnnotationUsage<JdbcTypeRegistration>> getJdbcTypeRegistrations();

	List<AnnotationUsage<TypeRegistration>> getUserTypeRegistrations();

	List<AnnotationUsage<CompositeTypeRegistration>> getCompositeUserTypeRegistrations();

	List<AnnotationUsage<CollectionTypeRegistration>> getCollectionTypeRegistrations();

	List<AnnotationUsage<EmbeddableInstantiatorRegistration>> getEmbeddableInstantiatorRegistrations();

	Map<String, AnnotationUsage<FilterDef>> getFilterDefRegistrations();

	Map<String, NamedQueryRegistration> getJpaNamedQueries();

	Map<String, NamedQueryRegistration> getHibernateNamedHqlQueries();

	Map<String, NamedQueryRegistration> getHibernateNamedNativeQueries();
}
