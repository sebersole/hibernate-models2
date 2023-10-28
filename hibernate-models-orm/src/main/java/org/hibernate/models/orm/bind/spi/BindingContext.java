/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.ClassDetailsRegistry;

import jakarta.persistence.SharedCacheMode;

/**
 * Contextual information used while {@linkplain BindingCoordinator binding}
 * {@linkplain org.hibernate.boot.model.process.spi.ManagedResources managed-resources} into
 * into Hibernate's {@linkplain org.hibernate.mapping boot-time model}.
 *
 * @author Steve Ebersole
 */
public interface BindingContext {
	ClassDetailsRegistry getClassDetailsRegistry();

	AnnotationDescriptorRegistry getAnnotationDescriptorRegistry();

	ClassmateContext getClassmateContext();

	SharedCacheMode getSharedCacheMode();
}
