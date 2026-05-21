/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.List;

import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.categorize.internal.StandardPersistentAttributeMemberResolver;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.SharedCacheMode;

/// Categorization-time access to bootstrap services and shared state.
///
/// The context adapts {@link MetadataBuildingContext} for the categorizer and the
/// metadata objects it creates.  It exposes Hibernate Models infrastructure, mapping
/// defaults, XML defaults, type services, converter services, and the global
/// registrations being collected for the persistence unit.
///
/// Services exposed here are inputs to, or working state for, categorization.  They
/// are intentionally separate from {@link CategorizedDomainModel}, which represents
/// the categorized result consumed by later binding phases.
///
/// @author Steve Ebersole
public interface CategorizationContext {
	MetadataBuildingContext getMetadataBuildingContext();

	default BootstrapContext getBootstrapContext() {
		return getMetadataBuildingContext().getBootstrapContext();
	}

	default StandardServiceRegistry getServiceRegistry() {
		return getBootstrapContext().getServiceRegistry();
	}

	default TypeConfiguration getTypeConfiguration() {
		return getBootstrapContext().getTypeConfiguration();
	}

	default ModelsContext getModelsContext() {
		return getBootstrapContext().getModelsContext();
	}

	default MetadataBuildingOptions getMetadataBuildingOptions() {
		return getBootstrapContext().getMetadataBuildingOptions();
	}

	default ClassDetailsRegistry getClassDetailsRegistry() {
		return getModelsContext().getClassDetailsRegistry();
	}

	default AnnotationDescriptorRegistry getAnnotationDescriptorRegistry() {
		return getModelsContext().getAnnotationDescriptorRegistry();
	}

	default SharedCacheMode getSharedCacheMode() {
		return getMetadataBuildingOptions().getSharedCacheMode();
	}

	default PersistentAttributeMemberResolver getPersistentAttributeMemberResolver() {
		return StandardPersistentAttributeMemberResolver.INSTANCE;
	}

	default PersistentAttributeMemberResolver getAttributeMemberResolver() {
		return getPersistentAttributeMemberResolver();
	}

	PersistenceUnitMetadata getPersistenceUnitMetadata();

	EffectiveMappingDefaults getEffectiveMappingDefaults();

	GlobalRegistrations getGlobalRegistrations();

	ConverterRegistry getConverterRegistry();

	Database getDatabase();

	List<JpaEventListener> getDefaultEventListeners();
}
