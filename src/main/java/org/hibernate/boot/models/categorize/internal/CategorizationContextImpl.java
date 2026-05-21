/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.List;

import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.models.categorize.spi.CategorizationContext;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * @author Steve Ebersole
 */
public class CategorizationContextImpl implements CategorizationContext {
	private final MetadataBuildingContext metadataBuildingContext;
	private final GlobalRegistrations globalRegistrations;

	public CategorizationContextImpl(
			MetadataBuildingContext metadataBuildingContext,
			GlobalRegistrations globalRegistrations) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.globalRegistrations = globalRegistrations;
	}

	@Override
	public MetadataBuildingContext getMetadataBuildingContext() {
		return metadataBuildingContext;
	}

	@Override
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	@Override
	public PersistenceUnitMetadata getPersistenceUnitMetadata() {
		return metadataBuildingContext.getMetadataCollector().getPersistenceUnitMetadata();
	}

	@Override
	public EffectiveMappingDefaults getEffectiveMappingDefaults() {
		return metadataBuildingContext.getEffectiveDefaults();
	}

	@Override
	public ConverterRegistry getConverterRegistry() {
		return metadataBuildingContext.getMetadataCollector().getConverterRegistry();
	}

	@Override
	public Database getDatabase() {
		return metadataBuildingContext.getMetadataCollector().getDatabase();
	}

	@Override
	public List<JpaEventListener> getDefaultEventListeners() {
		return getGlobalRegistrations().getEntityListenerRegistrations();
	}
}
