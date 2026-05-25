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
import org.hibernate.models.spi.ClassDetailsRegistry;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class CategorizationContextImpl implements CategorizationContext {
	private final PersistenceUnitMetadata persistenceUnitMetadata;
	private final EffectiveMappingDefaults effectiveMappingDefaults;
	private final ClassDetailsRegistry classDetailsRegistry;
	private final SharedCacheMode sharedCacheMode;
	private final GlobalRegistrations globalRegistrations;
	private final ConverterRegistry converterRegistry;
	private final Database database;

	public CategorizationContextImpl(
			PersistenceUnitMetadata persistenceUnitMetadata,
			EffectiveMappingDefaults effectiveMappingDefaults,
			ClassDetailsRegistry classDetailsRegistry,
			SharedCacheMode sharedCacheMode,
			GlobalRegistrations globalRegistrations,
			ConverterRegistry converterRegistry,
			Database database) {
		this.persistenceUnitMetadata = persistenceUnitMetadata;
		this.effectiveMappingDefaults = effectiveMappingDefaults;
		this.classDetailsRegistry = classDetailsRegistry;
		this.sharedCacheMode = sharedCacheMode;
		this.globalRegistrations = globalRegistrations;
		this.converterRegistry = converterRegistry;
		this.database = database;
	}

	@Override
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}

	@Override
	public PersistenceUnitMetadata getPersistenceUnitMetadata() {
		return persistenceUnitMetadata;
	}

	@Override
	public EffectiveMappingDefaults getEffectiveMappingDefaults() {
		return effectiveMappingDefaults;
	}

	@Override
	public ClassDetailsRegistry getClassDetailsRegistry() {
		return classDetailsRegistry;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	@Override
	public ConverterRegistry getConverterRegistry() {
		return converterRegistry;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	@Override
	public List<JpaEventListener> getDefaultEventListeners() {
		return getGlobalRegistrations().getEntityListenerRegistrations();
	}
}
