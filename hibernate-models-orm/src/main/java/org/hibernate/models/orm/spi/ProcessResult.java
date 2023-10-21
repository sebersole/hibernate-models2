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
import org.hibernate.models.orm.internal.EmbeddableInstantiatorRegistration;
import org.hibernate.models.orm.internal.FilterDefRegistration;
import org.hibernate.models.orm.internal.NamedQueryRegistration;
import org.hibernate.models.source.spi.ClassDetails;

/**
 * The result of {@linkplain Processor#process processing} the domain model
 *
 * @author Steve Ebersole
 */
public interface ProcessResult {
	/**
	 * All entity hierarchies defined in the persistence unit
	 */
	Set<EntityHierarchy> getEntityHierarchies();

	/**
	 * All mapped-superclasses defined in the persistence unit
	 */
	Map<String,ClassDetails> getMappedSuperclasses();

	/**
	 * All embeddables defined in the persistence unit
	 */
	Map<String,ClassDetails> getEmbeddables();

	/**
	 * Global registrations collected while processing the persistence-unit.
	 */
	GlobalRegistrations getGlobalRegistrations();
}
