/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.models.categorize.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.categorize.internal.ManagedTypeInheritanceState;
import org.hibernate.boot.models.categorize.internal.CategorizationContextImpl;
import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.xml.AvailableXmlMappings;
import org.hibernate.boot.models.source.xml.AvailableXmlMappingsPreProcessor;
import org.hibernate.boot.models.source.xml.AvailableXmlMappingsProcessor;
import org.hibernate.boot.models.source.xml.XmlProcessingResult;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.models.categorize.internal.EntityHierarchyBuilder.createEntityHierarchies;

/// Processes {@linkplain AvailableResources available resources} and produces a
/// {@linkplain CategorizedDomainModel categorized domain model}.
///
/// XML mappings are pre-processed first so they can contribute managed class names
/// and metadata-complete annotations.  The resulting visible persistent types are
/// then organized into managed-type inheritance state before entity hierarchies are
/// created.
///
/// This is the public entry point for the categorization phase.  It owns the
/// transition from discovered sources to categorized contracts; later phases should
/// consume the resulting {@link CategorizedDomainModel} rather than repeat source
/// discovery.
///
/// @author Steve Ebersole
public class DomainModelCategorizer {
	private DomainModelCategorizer() {
	}

	public static CategorizedDomainModel categorize(
			AvailableResources availableResources,
			MetadataBuildingContext metadataBuildingContext) {
		final var bootstrapContext = metadataBuildingContext.getBootstrapContext();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 	- pre-process the XML
		// 	- collect all known classes
		// 	- use the BootstrapContext's ModelsContext
		//
		// INPUTS:
		//		- availableResources
		//		- bootstrapContext
		//
		// OUTPUTS:
		//		- availableXmlMappings
		//		- allKnownClassNames (technically could be included in xmlPreProcessingResult)
		//		- modelsContext

		final AvailableXmlMappings availableXmlMappings = AvailableXmlMappingsPreProcessor.preProcess(
				availableResources,
				metadataBuildingContext.getMetadataCollector().getPersistenceUnitMetadata(),
				bootstrapContext
		);

		final List<String> allKnownClassNames = new ArrayList<>( availableXmlMappings.getMappedClasses() );
		availableResources.managedClassDetails().forEach( (classDetails) -> allKnownClassNames.add( classDetails.getName() ) );
		availableResources.packageDetails().forEach( (packageDetails) -> allKnownClassNames.add( packageDetails.getName() ) );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 	- process metadata-complete XML
		//	- collect overlay XML
		//	- process annotations (including those from metadata-complete XML)
		//	- apply overlay XML
		//
		// INPUTS:
		//		- "options" (areIdGeneratorsGlobal, etc)
		//		- xmlPreProcessingResult
		//		- sourceModelBuildingContext
		//
		// OUTPUTS
		//		- rootEntities
		//		- mappedSuperClasses
		//  	- embeddables

		// JPA id generator global-ity thing
		final boolean areIdGeneratorsGlobal = true;
		final ModelsContext modelsContext = bootstrapContext.getModelsContext();
		final ClassDetailsRegistry mutableClassDetailsRegistry = modelsContext.getClassDetailsRegistry();
		final DomainModelCategorizationCollector modelCategorizationCollector = new DomainModelCategorizationCollector(
				areIdGeneratorsGlobal,
				modelsContext
		);

		final RootMappingDefaults mappingDefaults = rootMappingDefaults( metadataBuildingContext );
		final XmlProcessingResult xmlProcessingResult = AvailableXmlMappingsProcessor.process(
				availableXmlMappings,
				bootstrapContext,
				mappingDefaults,
				(jaxbRoot, xmlDocumentContext) -> modelCategorizationCollector.apply( jaxbRoot )
		);

		allKnownClassNames.forEach( (className) -> {
			final ClassDetails classDetails = mutableClassDetailsRegistry.resolveClassDetails( className );
			modelCategorizationCollector.apply( classDetails );
		} );
		availableXmlMappings.getMappedNames().forEach( (className) -> {
			final ClassDetails classDetails = mutableClassDetailsRegistry.resolveClassDetails( className );
			modelCategorizationCollector.apply( classDetails );
		} );

		xmlProcessingResult.apply();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		//	- create entity-hierarchies
		//	- create the CategorizedDomainModel
		//
		// INPUTS:
		//		- rootEntities
		//		- mappedSuperClasses
		//  	- embeddables
		//
		// OUTPUTS:
		//		- CategorizedDomainModel

		// Collect the entity hierarchies based on the scoped managed type inheritance state
		final CategorizationContextImpl mappingBuildingContext = new CategorizationContextImpl(
				metadataBuildingContext,
				modelCategorizationCollector.getGlobalRegistrations()
		);

		final ManagedTypeInheritanceState inheritanceState = new ManagedTypeInheritanceState(
				modelCategorizationCollector.getSourcePersistentTypes()
		);
		final Set<EntityHierarchy> entityHierarchies = createEntityHierarchies(
				inheritanceState,
				mappingBuildingContext
		);

		return modelCategorizationCollector.createResult( entityHierarchies );
	}

	private static RootMappingDefaults rootMappingDefaults(MetadataBuildingContext metadataBuildingContext) {
		if ( metadataBuildingContext.getEffectiveDefaults() instanceof RootMappingDefaults rootMappingDefaults ) {
			return rootMappingDefaults;
		}

		return new RootMappingDefaults(
				metadataBuildingContext.getBuildingOptions().getMappingDefaults(),
				metadataBuildingContext.getMetadataCollector().getPersistenceUnitMetadata()
		);
	}

}
