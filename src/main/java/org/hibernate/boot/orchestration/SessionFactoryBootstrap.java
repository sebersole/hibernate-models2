/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.models.bind.internal.BindingContextImpl;
import org.hibernate.boot.models.bind.internal.BindingOptionsImpl;
import org.hibernate.boot.models.bind.internal.BindingStateImpl;
import org.hibernate.boot.models.bind.spi.BindingCoordinator;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;
import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.settings.BootstrapSettingsResolver;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;

/// Orchestrates the boot-model path toward SessionFactory creation.
///
/// This class owns the ordering between bootstrap phases.  The current PoC slice
/// supports the first bridge needed by SessionFactory bootstrapping: turning
/// available model resources into ORM's boot-time [MetadataImplementor].
///
/// As the prototype grows, this class should remain the place where adjacent
/// phases are composed.  The individual phase components should continue to own
/// their mechanics.
///
/// @author Steve Ebersole
public class SessionFactoryBootstrap {
	/// Build ORM boot metadata from the current narrow bootstrap request.
	///
	/// The request contains early bootstrap inputs.  Settings, available
	/// resources, and the metadata-building context are derived here so each phase
	/// can consume artifacts produced by earlier phases.
	///
	/// @param request The current bootstrap request
	///
	/// @return The metadata collector after categorization, binding, ordering, and
	/// validation have run
	public MetadataImplementor buildMetadata(SessionFactoryBootstrapRequest request) {
		final ResolvedBootstrapSettings bootstrapSettings = new BootstrapSettingsResolver().resolve(
				request.configurationValues(),
				request.jpaBootstrap(),
				request.defaultToOneFetchType()
		);
		final MetadataBuildingContext metadataBuildingContext = createMetadataBuildingContext( request, bootstrapSettings );
		final AvailableResources availableResources = AvailableResources.from(
				request.sourceContributions(),
				new AvailableResourcesContext(
						metadataBuildingContext.getBootstrapContext().getModelsContext(),
						metadataBuildingContext.getBootstrapContext().getServiceRegistry()
				),
				bootstrapSettings
		);

		final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
				availableResources,
				metadataBuildingContext
		);

		BindingCoordinator.coordinateBinding(
				categorizedDomainModel,
				new BindingStateImpl( metadataBuildingContext ),
				new BindingOptionsImpl( metadataBuildingContext ),
				new BindingContextImpl(
						categorizedDomainModel,
						metadataBuildingContext.getBootstrapContext()
				)
		);

		final MetadataImplementor metadata = metadataBuildingContext.getMetadataCollector();
		metadata.orderColumns( false );
		metadata.validate();
		return metadata;
	}

	private static MetadataBuildingContext createMetadataBuildingContext(
			SessionFactoryBootstrapRequest request,
			ResolvedBootstrapSettings bootstrapSettings) {
		final var standardServiceRegistry = MetadataBuilderImpl.getStandardServiceRegistry( request.serviceRegistry() );
		final var metadataBuilder = new MetadataBuilderImpl( new MetadataSources( standardServiceRegistry ), standardServiceRegistry );
		metadataBuilder.applyDefaultToOneFetchType( bootstrapSettings.mappingSettings().defaultToOneFetchType() );
		bootstrapSettings.mappingSettings()
				.cacheRegionDefinitions()
				.forEach( metadataBuilder::applyCacheRegionDefinition );

		final var bootstrapContext = metadataBuilder.getBootstrapContext();
		if ( bootstrapSettings.jpaBootstrap() ) {
			bootstrapContext.markAsJpaBootstrap();
		}

		final var buildingOptions = metadataBuilder.getMetadataBuildingOptions();
		final var metadataCollector = new InFlightMetadataCollectorImpl( bootstrapContext, buildingOptions );
		return new MetadataBuildingContextRootImpl(
				"orm",
				bootstrapContext,
				buildingOptions,
				metadataCollector,
				new RootMappingDefaults(
						buildingOptions.getMappingDefaults(),
						metadataCollector.getPersistenceUnitMetadata()
				)
		);
	}
}
