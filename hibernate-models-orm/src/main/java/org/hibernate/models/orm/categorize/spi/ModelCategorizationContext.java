/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.spi;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.models.orm.categorize.internal.StandardPersistentAttributeMemberResolver;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.ClassDetailsRegistry;

import jakarta.persistence.SharedCacheMode;

/**
 * Contextual information used while building {@linkplain ManagedTypeMetadata} and friends.
 *
 * @author Steve Ebersole
 */
public interface ModelCategorizationContext {
	ClassDetailsRegistry getClassDetailsRegistry();

	AnnotationDescriptorRegistry getAnnotationDescriptorRegistry();

	default PersistentAttributeMemberResolver getPersistentAttributeMemberResolver() {
		return StandardPersistentAttributeMemberResolver.INSTANCE;
	}

	SharedCacheMode getSharedCacheMode();

}
