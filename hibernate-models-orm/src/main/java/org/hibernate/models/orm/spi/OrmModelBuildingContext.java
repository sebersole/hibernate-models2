/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.models.orm.internal.StandardPersistentAttributeMemberResolver;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ClassLoading;

import org.jboss.jandex.IndexView;

import jakarta.persistence.SharedCacheMode;

/**
 * Contextual information used while building {@linkplain ManagedTypeMetadata} and friends.
 *
 * @author Steve Ebersole
 */
public interface OrmModelBuildingContext {
	ClassDetailsRegistry getClassDetailsRegistry();

	AnnotationDescriptorRegistry getAnnotationDescriptorRegistry();

	ClassmateContext getClassmateContext();

	default PersistentAttributeMemberResolver getPersistentAttributeMemberResolver() {
		return StandardPersistentAttributeMemberResolver.INSTANCE;
	}

	SharedCacheMode getSharedCacheMode();

}
