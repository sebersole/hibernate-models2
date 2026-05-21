/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.Map;
import java.util.Set;

import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.models.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public class CategorizedDomainModelImpl implements CategorizedDomainModel {
	private final Set<EntityHierarchy> entityHierarchies;
	private final Map<String, ClassDetails> mappedSuperclasses;
	private final Map<String, ClassDetails> embeddables;
	private final GlobalRegistrations globalRegistrations;

	public CategorizedDomainModelImpl(
			Set<EntityHierarchy> entityHierarchies,
			Map<String, ClassDetails> mappedSuperclasses,
			Map<String, ClassDetails> embeddables,
			GlobalRegistrations globalRegistrations) {
		this.entityHierarchies = entityHierarchies;
		this.mappedSuperclasses = mappedSuperclasses;
		this.embeddables = embeddables;
		this.globalRegistrations = globalRegistrations;
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
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}
}
