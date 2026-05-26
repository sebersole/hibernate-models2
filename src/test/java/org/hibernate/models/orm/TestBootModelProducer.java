/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

import java.util.Map;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.models.source.BootstrapSourceContributions;
import org.hibernate.boot.orchestration.MetadataResolver;
import org.hibernate.boot.orchestration.ResolvedMetadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.settings.BootstrapSettingsResolver;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.service.ServiceRegistry;

/**
 * Test helper that composes categorization and binding into ORM boot metadata.
 *
 * @author Steve Ebersole
 */
public class TestBootModelProducer {
	public static MetadataImplementor buildMetadata(
			MetadataBuildingContext metadataBuildingContext,
			Class<?>... domainClasses) {
		return buildMetadata(
				metadataBuildingContext.getBootstrapContext().getServiceRegistry(),
				domainClasses
		);
	}

	public static MetadataImplementor buildMetadata(
			ServiceRegistry serviceRegistry,
			Class<?>... domainClasses) {
		final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		for ( Class<?> domainClass : domainClasses ) {
			persistenceConfiguration.managedClass( domainClass );
		}
		return buildMetadata(
				serviceRegistry,
				persistenceConfiguration
		);
	}

	public static MetadataImplementor buildMetadata(
			MetadataBuildingContext metadataBuildingContext,
			HibernatePersistenceConfiguration persistenceConfiguration) {
		return buildMetadata(
				metadataBuildingContext.getBootstrapContext().getServiceRegistry(),
				persistenceConfiguration
		);
	}

	public static MetadataImplementor buildMetadata(
			ServiceRegistry serviceRegistry,
			HibernatePersistenceConfiguration persistenceConfiguration) {
		return buildMetadata( serviceRegistry, persistenceConfiguration, Map.of() );
	}

	public static MetadataImplementor buildMetadata(
			ServiceRegistry serviceRegistry,
			HibernatePersistenceConfiguration persistenceConfiguration,
			Map<String, Object> configurationValues) {
		return resolveMetadata(
				serviceRegistry,
				persistenceConfiguration,
				configurationValues
		).metadata();
	}

	public static ResolvedMetadata resolveMetadata(
			ServiceRegistry serviceRegistry,
			HibernatePersistenceConfiguration persistenceConfiguration) {
		return resolveMetadata( serviceRegistry, persistenceConfiguration, Map.of() );
	}

	public static ResolvedMetadata resolveMetadata(
			ServiceRegistry serviceRegistry,
			HibernatePersistenceConfiguration persistenceConfiguration,
			Map<String, Object> configurationValues) {
		final ResolvedBootstrapSettings bootstrapSettings = new BootstrapSettingsResolver().resolve(
				persistenceConfiguration,
				configurationValues
		);
		final BootstrapSourceContributions sourceContributions = BootstrapSourceContributions.from(
				persistenceConfiguration,
				bootstrapSettings,
				serviceRegistry.requireService( ClassLoaderService.class )
		);
		return new MetadataResolver().resolve(
				bootstrapSettings,
				sourceContributions,
				serviceRegistry
		);
	}

	public static AvailableResources availableResources(
			MetadataBuildingContext metadataBuildingContext,
			Class<?>... domainClasses) {
		final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		for ( Class<?> domainClass : domainClasses ) {
			persistenceConfiguration.managedClass( domainClass );
		}
		return availableResources( metadataBuildingContext, persistenceConfiguration );
	}

	public static AvailableResources availableResources(
			MetadataBuildingContext metadataBuildingContext,
			HibernatePersistenceConfiguration persistenceConfiguration) {
		return AvailableResources.from(
				persistenceConfiguration,
				new AvailableResourcesContext(
						metadataBuildingContext.getBootstrapContext().getModelsContext(),
						metadataBuildingContext.getBootstrapContext().getServiceRegistry()
				)
		);
	}

	public static AvailableResources availableResources(
			MetadataBuildingContext metadataBuildingContext,
			HibernatePersistenceConfiguration persistenceConfiguration,
			ResolvedBootstrapSettings bootstrapSettings) {
		return AvailableResources.from(
				BootstrapSourceContributions.from(
						persistenceConfiguration,
						bootstrapSettings,
						metadataBuildingContext.getBootstrapContext()
								.getServiceRegistry()
								.requireService( ClassLoaderService.class )
				),
				new AvailableResourcesContext(
						metadataBuildingContext.getBootstrapContext().getModelsContext(),
						metadataBuildingContext.getBootstrapContext().getServiceRegistry()
				),
				bootstrapSettings
		);
	}

	private TestBootModelProducer() {
	}
}
