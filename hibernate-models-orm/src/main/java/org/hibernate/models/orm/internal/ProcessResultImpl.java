/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.models.orm.spi.ProcessResult;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.source.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public class ProcessResultImpl implements ProcessResult {
	private final Set<EntityHierarchy> entityHierarchies;

	private final Map<String, ClassDetails> mappedSuperclasses;
	private final Map<String, ClassDetails> embeddables;
	private final List<JavaTypeRegistration> javaTypeRegistrations;
	private final List<JdbcTypeRegistration> jdbcTypeRegistrations;
	private final List<ConversionRegistration> converterRegistrations;
	private final List<ClassDetails> autoAppliedConverters;
	private final List<UserTypeRegistration> userTypeRegistrations;
	private final List<CompositeUserTypeRegistration> compositeUserTypeRegistrations;
	private final List<CollectionTypeRegistration> collectionTypeRegistrations;
	private final List<EmbeddableInstantiatorRegistration> embeddableInstantiatorRegistrations;
	private final Map<String, FilterDefRegistration> filterDefRegistrations;
	private final Map<String, NamedQueryRegistration> jpaNamedQueries;
	private final Map<String, NamedQueryRegistration> hibernateNamedHqlQueries;
	private final Map<String, NamedQueryRegistration> hibernateNamedNativeQueries;

	public ProcessResultImpl(
			Set<EntityHierarchy> entityHierarchies,
			Map<String, ClassDetails> mappedSuperclasses,
			Map<String, ClassDetails> embeddables,
			List<JavaTypeRegistration> javaTypeRegistrations,
			List<JdbcTypeRegistration> jdbcTypeRegistrations,
			List<ConversionRegistration> converterRegistrations,
			List<ClassDetails> autoAppliedConverters,
			List<UserTypeRegistration> userTypeRegistrations,
			List<CompositeUserTypeRegistration> compositeUserTypeRegistrations,
			List<CollectionTypeRegistration> collectionTypeRegistrations,
			List<EmbeddableInstantiatorRegistration> embeddableInstantiatorRegistrations,
			Map<String, FilterDefRegistration> filterDefRegistrations,
			Map<String, NamedQueryRegistration> jpaNamedQueries,
			Map<String, NamedQueryRegistration> hibernateNamedHqlQueries,
			Map<String, NamedQueryRegistration> hibernateNamedNativeQueries) {
		this.entityHierarchies = entityHierarchies;
		this.mappedSuperclasses = mappedSuperclasses;
		this.embeddables = embeddables;
		this.javaTypeRegistrations = javaTypeRegistrations;
		this.jdbcTypeRegistrations = jdbcTypeRegistrations;
		this.converterRegistrations = converterRegistrations;
		this.autoAppliedConverters = autoAppliedConverters;
		this.userTypeRegistrations = userTypeRegistrations;
		this.compositeUserTypeRegistrations = compositeUserTypeRegistrations;
		this.collectionTypeRegistrations = collectionTypeRegistrations;
		this.embeddableInstantiatorRegistrations = embeddableInstantiatorRegistrations;
		this.filterDefRegistrations = filterDefRegistrations;
		this.jpaNamedQueries = jpaNamedQueries;
		this.hibernateNamedHqlQueries = hibernateNamedHqlQueries;
		this.hibernateNamedNativeQueries = hibernateNamedNativeQueries;
	}

	@Override
	public Set<EntityHierarchy> getEntityHierarchies() {
		return entityHierarchies;
	}

	public Map<String, ClassDetails> getMappedSuperclasses() {
		return mappedSuperclasses;
	}

	@Override
	public Map<String, ClassDetails> getEmbeddables() {
		return embeddables;
	}

	@Override
	public List<JavaTypeRegistration> getJavaTypeRegistrations() {
		return javaTypeRegistrations;
	}

	@Override
	public List<JdbcTypeRegistration> getJdbcTypeRegistrations() {
		return jdbcTypeRegistrations;
	}

	@Override
	public List<ConversionRegistration> getConverterRegistrations() {
		return converterRegistrations;
	}

	@Override
	public List<ClassDetails> getAutoAppliedConverters() {
		return autoAppliedConverters;
	}

	@Override
	public List<UserTypeRegistration> getUserTypeRegistrations() {
		return userTypeRegistrations;
	}

	@Override
	public List<CompositeUserTypeRegistration> getCompositeUserTypeRegistrations() {
		return compositeUserTypeRegistrations;
	}

	@Override
	public List<CollectionTypeRegistration> getCollectionTypeRegistrations() {
		return collectionTypeRegistrations;
	}

	@Override
	public List<EmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations() {
		return embeddableInstantiatorRegistrations;
	}

	@Override
	public Map<String, FilterDefRegistration> getFilterDefRegistrations() {
		return filterDefRegistrations;
	}

	@Override
	public Map<String, NamedQueryRegistration> getJpaNamedQueries() {
		return jpaNamedQueries;
	}

	@Override
	public Map<String, NamedQueryRegistration> getHibernateNamedHqlQueries() {
		return hibernateNamedHqlQueries;
	}

	@Override
	public Map<String, NamedQueryRegistration> getHibernateNamedNativeQueries() {
		return hibernateNamedNativeQueries;
	}
}
