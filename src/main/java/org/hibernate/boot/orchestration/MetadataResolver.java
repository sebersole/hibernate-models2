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
import org.hibernate.boot.models.source.BootstrapSourceContributions;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;

/// Resolves ORM boot metadata from source contributions and resolved settings.
///
/// This class owns the phase order for the metadata target.  It turns resolved
/// bootstrap settings and source contributions into ORM's boot-time
/// [MetadataImplementor] while preserving intermediate boot-model products in
/// [ResolvedMetadata].
///
/// Higher-level entry points should resolve settings and source contributions
/// before calling this resolver.
///
/// @author Steve Ebersole
public class MetadataResolver {
	/// Resolve only ORM boot metadata.
	///
	/// @param bootstrapSettings Resolved bootstrap and mapping settings
	/// @param sourceContributions Mapping-source contributions supplied by the
	/// entry point
	/// @param serviceRegistry Service registry for the metadata build
	///
	/// @return The metadata collector after categorization, binding, ordering, and
	/// validation have run
	public MetadataImplementor resolveMetadata(
			ResolvedBootstrapSettings bootstrapSettings,
			BootstrapSourceContributions sourceContributions,
			ServiceRegistry serviceRegistry) {
		return resolve( bootstrapSettings, sourceContributions, serviceRegistry ).metadata();
	}

	/// Resolve ORM boot metadata and retain intermediate boot-model products.
	///
	/// This is the preferred prototype entry point for later SessionFactory
	/// construction experiments because it exposes the categorized model,
	/// binding-state bridge, and resolved settings that were used to produce the
	/// metadata.
	///
	/// @param bootstrapSettings Resolved bootstrap and mapping settings
	/// @param sourceContributions Mapping-source contributions supplied by the
	/// entry point
	/// @param serviceRegistry Service registry for the metadata build
	///
	/// @return The resolved metadata product
	public ResolvedMetadata resolve(
			ResolvedBootstrapSettings bootstrapSettings,
			BootstrapSourceContributions sourceContributions,
			ServiceRegistry serviceRegistry) {
		final MetadataBuildingContext metadataBuildingContext = createMetadataBuildingContext(
				serviceRegistry,
				bootstrapSettings
		);
		final AvailableResources availableResources = buildAvailableResources(
				sourceContributions,
				bootstrapSettings,
				metadataBuildingContext
		);
		final CategorizedDomainModel categorizedDomainModel = categorize(
				availableResources,
				metadataBuildingContext
		);
		final BindingStateImpl bindingState = bind(
				categorizedDomainModel,
				bootstrapSettings,
				metadataBuildingContext
		);
		final MetadataImplementor metadata = finalizeMetadata( metadataBuildingContext, bindingState );
		return new ResolvedMetadata(
				metadata,
				categorizedDomainModel,
				bindingState,
				bootstrapSettings
		);
	}

	private static AvailableResources buildAvailableResources(
			BootstrapSourceContributions sourceContributions,
			ResolvedBootstrapSettings bootstrapSettings,
			MetadataBuildingContext metadataBuildingContext) {
		return AvailableResources.from(
				sourceContributions,
				new AvailableResourcesContext(
						metadataBuildingContext.getBootstrapContext().getModelsContext(),
						metadataBuildingContext.getBootstrapContext().getServiceRegistry()
				),
				bootstrapSettings
		);
	}

	private static CategorizedDomainModel categorize(
			AvailableResources availableResources,
			MetadataBuildingContext metadataBuildingContext) {
		return DomainModelCategorizer.categorize(
				availableResources,
				metadataBuildingContext
		);
	}

	private static BindingStateImpl bind(
			CategorizedDomainModel categorizedDomainModel,
			ResolvedBootstrapSettings bootstrapSettings,
			MetadataBuildingContext metadataBuildingContext) {
		final BindingStateImpl bindingState = new BindingStateImpl( metadataBuildingContext );
		BindingCoordinator.coordinateBinding(
				categorizedDomainModel,
				bindingState,
				new BindingOptionsImpl( metadataBuildingContext, bootstrapSettings.mappingSettings() ),
				new BindingContextImpl(
						categorizedDomainModel,
						metadataBuildingContext.getBootstrapContext()
				)
		);
		return bindingState;
	}

	private static MetadataImplementor finalizeMetadata(
			MetadataBuildingContext metadataBuildingContext,
			BindingStateImpl bindingState) {
		bindingState.applyMetadataRegistrations( metadataBuildingContext.getMetadataCollector() );
		final MetadataImplementor metadata = metadataBuildingContext.getMetadataCollector();
		metadata.orderColumns( false );
		metadata.validate();
		return metadata;
	}

	private static MetadataBuildingContext createMetadataBuildingContext(
			ServiceRegistry serviceRegistry,
			ResolvedBootstrapSettings bootstrapSettings) {
		final var standardServiceRegistry = MetadataBuilderImpl.getStandardServiceRegistry( serviceRegistry );
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
