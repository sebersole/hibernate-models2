/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.util.List;
import java.util.Map;


/**
 * @author Steve Ebersole
 */
public interface GlobalRegistrations {
	List<EntityListenerRegistration> getEntityListenerRegistrations();

	List<ConversionRegistration> getConverterRegistrations();

	List<JavaTypeRegistration> getJavaTypeRegistrations();

	List<JdbcTypeRegistration> getJdbcTypeRegistrations();

	List<UserTypeRegistration> getUserTypeRegistrations();

	List<CompositeUserTypeRegistration> getCompositeUserTypeRegistrations();

	List<CollectionTypeRegistration> getCollectionTypeRegistrations();

	List<EmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations();

	Map<String, FilterDefRegistration> getFilterDefRegistrations();

	Map<String, SequenceGeneratorRegistration> getSequenceGeneratorRegistrations();

	Map<String, TableGeneratorRegistration> getTableGeneratorRegistrations();

	Map<String, GenericGeneratorRegistration> getGenericGeneratorRegistrations();

	// todo : named entity graphs
	// todo : named queries
}