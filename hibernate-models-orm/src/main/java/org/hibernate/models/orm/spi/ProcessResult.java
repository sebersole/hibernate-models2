/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.models.orm.internal.CollectionTypeRegistration;
import org.hibernate.models.orm.internal.CompositeUserTypeRegistration;
import org.hibernate.models.orm.internal.ConversionRegistration;
import org.hibernate.models.orm.internal.EmbeddableInstantiatorRegistration;
import org.hibernate.models.orm.internal.IdGeneratorRegistration;
import org.hibernate.models.orm.internal.JavaTypeRegistration;
import org.hibernate.models.orm.internal.JdbcTypeRegistration;
import org.hibernate.models.orm.internal.NamedQueryRegistration;
import org.hibernate.models.orm.internal.UserTypeRegistration;
import org.hibernate.models.source.spi.ClassDetails;

/**
 * The result of {@linkplain Processor#process processing} the domain model
 *
 * @author Steve Ebersole
 */
public interface ProcessResult {
	Set<EntityHierarchy> getEntityHierarchies();

	List<JavaTypeRegistration> getJavaTypeRegistrations();

	List<JdbcTypeRegistration> getJdbcTypeRegistrations();

	List<ConversionRegistration> getConverterRegistrations();

	List<ClassDetails> getAutoAppliedConverters();

	List<UserTypeRegistration> getUserTypeRegistrations();

	List<CompositeUserTypeRegistration> getCompositeUserTypeRegistrations();

	List<CollectionTypeRegistration> getCollectionTypeRegistrations();

	List<EmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations();

	Map<String, IdGeneratorRegistration> getGlobalIdGeneratorRegistrations();

	Map<String, NamedQueryRegistration> getJpaNamedQueries();

	Map<String, NamedQueryRegistration> getHibernateNamedHqlQueries();

	Map<String, NamedQueryRegistration> getHibernateNamedNativeQueries();
}
