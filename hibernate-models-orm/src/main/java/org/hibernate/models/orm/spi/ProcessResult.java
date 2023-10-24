/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.util.Map;
import java.util.Set;

import org.hibernate.models.internal.IndexedConsumer;
import org.hibernate.models.internal.KeyedConsumer;
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

	default void forEachEntityHierarchy(IndexedConsumer<EntityHierarchy> hierarchyConsumer) {
		final Set<EntityHierarchy> entityHierarchies = getEntityHierarchies();
		if ( entityHierarchies.isEmpty() ) {
			return;
		}

		int pos = 0;
		for ( EntityHierarchy entityHierarchy : entityHierarchies ) {
			hierarchyConsumer.accept( pos, entityHierarchy );
			pos++;
		}
	}

	/**
	 * All mapped-superclasses defined in the persistence unit
	 */
	Map<String,ClassDetails> getMappedSuperclasses();

	default void forEachMappedSuperclass(KeyedConsumer<String, ClassDetails> consumer) {
		final Map<String, ClassDetails> mappedSuperclasses = getMappedSuperclasses();
		if ( mappedSuperclasses.isEmpty() ) {
			return;
		}

		mappedSuperclasses.forEach( consumer::accept );
	}

	/**
	 * All embeddables defined in the persistence unit
	 */
	Map<String,ClassDetails> getEmbeddables();

	default void forEachEmbeddable(KeyedConsumer<String, ClassDetails> consumer) {
		final Map<String, ClassDetails> embeddables = getEmbeddables();
		if ( embeddables.isEmpty() ) {
			return;
		}

		embeddables.forEach( consumer::accept );
	}

	/**
	 * Global registrations collected while processing the persistence-unit.
	 */
	GlobalRegistrations getGlobalRegistrations();
}
