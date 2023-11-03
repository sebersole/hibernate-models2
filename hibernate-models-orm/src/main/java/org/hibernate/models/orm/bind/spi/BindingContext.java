/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.orm.categorize.spi.GlobalRegistrations;
import org.hibernate.models.source.spi.SourceModelContext;
import org.hibernate.service.ServiceRegistry;

import jakarta.persistence.SharedCacheMode;

/**
 * Contextual information used while {@linkplain BindingCoordinator binding}
 * {@linkplain org.hibernate.boot.model.process.spi.ManagedResources managed-resources} into
 * into Hibernate's {@linkplain org.hibernate.mapping boot-time model}.
 *
 * @author Steve Ebersole
 */
public interface BindingContext extends SourceModelContext {

	GlobalRegistrations getGlobalRegistrations();

	ClassmateContext getClassmateContext();

	SharedCacheMode getSharedCacheMode();

	BootstrapContext getBootstrapContext();

	default ServiceRegistry getServiceRegistry() {
		return getBootstrapContext().getServiceRegistry();
	}
}
