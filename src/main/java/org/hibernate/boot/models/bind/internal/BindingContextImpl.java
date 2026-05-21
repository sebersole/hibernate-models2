/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class BindingContextImpl implements BindingContext {
	private final GlobalRegistrations globalRegistrations;

	private final ImplicitNamingStrategy implicitNamingStrategy;
	private final PhysicalNamingStrategy physicalNamingStrategy;
	private final SharedCacheMode sharedCacheMode;
	private final BootstrapContext bootstrapContext;

	public BindingContextImpl(CategorizedDomainModel categorizedDomainModel, BootstrapContext bootstrapContext) {
		this(
				categorizedDomainModel.getGlobalRegistrations(),
				bootstrapContext.getMetadataBuildingOptions().getImplicitNamingStrategy(),
				bootstrapContext.getMetadataBuildingOptions().getPhysicalNamingStrategy(),
				bootstrapContext.getMetadataBuildingOptions().getSharedCacheMode(),
				bootstrapContext
		);
	}

	public BindingContextImpl(
			GlobalRegistrations globalRegistrations,
			ImplicitNamingStrategy implicitNamingStrategy,
			PhysicalNamingStrategy physicalNamingStrategy,
			SharedCacheMode sharedCacheMode,
			BootstrapContext bootstrapContext) {
		this.implicitNamingStrategy = implicitNamingStrategy;
		this.physicalNamingStrategy = physicalNamingStrategy;
		this.bootstrapContext = bootstrapContext;
		this.globalRegistrations = globalRegistrations;
		this.sharedCacheMode = sharedCacheMode;
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	@Override
	public ImplicitNamingStrategy getImplicitNamingStrategy() {
		return implicitNamingStrategy;
	}

	@Override
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return physicalNamingStrategy;
	}
}
