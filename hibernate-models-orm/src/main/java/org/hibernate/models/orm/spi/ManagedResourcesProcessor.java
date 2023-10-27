/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.orm.internal.ClassLoaderServiceLoading;
import org.hibernate.models.orm.internal.DomainModelCategorizationCollector;
import org.hibernate.models.orm.internal.OrmModelBuildingContextImpl;
import org.hibernate.models.orm.internal.OrmModelLogging;
import org.hibernate.models.orm.xml.spi.XmlProcessingResult;
import org.hibernate.models.orm.xml.spi.XmlPreProcessingResult;
import org.hibernate.models.orm.xml.spi.XmlPreProcessor;
import org.hibernate.models.orm.xml.spi.XmlProcessor;
import org.hibernate.models.source.internal.SourceModelBuildingContextImpl;
import org.hibernate.models.source.internal.jandex.JandexClassDetails;
import org.hibernate.models.source.internal.jandex.JandexIndexerHelper;
import org.hibernate.models.source.internal.jandex.JandexPackageDetails;
import org.hibernate.models.source.internal.jdk.JdkBuilders;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.RegistryPrimer;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.ClassLoading;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import static org.hibernate.models.orm.internal.EntityHierarchyBuilder.createEntityHierarchies;

/**
 * Processes {@linkplain ManagedResources} references (classes, mapping, etc.) and
 * produces a CategorizedDomainModel
 *
 * @author Steve Ebersole
 */
public class ManagedResourcesProcessor {
	public static CategorizedDomainModel processManagedResources(
			ManagedResources managedResources,
			BootstrapContext bootstrapContext) {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// 	- pre-process the XML
		// 	- collect all known classes
		// 	- resolve Jandex index
		// 	- build the SourceModelBuildingContext
		//
		// INPUTS:
		//		- serviceRegistry
		//		- managedResources
		//		- bootstrapContext (supplied Jandex index, if one)
		//
		// OUTPUTS:
		//		- xmlPreProcessingResult
		//		- allKnownClassNames (technically could be included in xmlPreProcessingResult)
		//		- sourceModelBuildingContext

		final ClassLoaderService classLoaderService = bootstrapContext.getServiceRegistry().getService( ClassLoaderService.class );
		final ClassLoaderServiceLoading classLoading = new ClassLoaderServiceLoading( classLoaderService );

		final XmlPreProcessingResult xmlPreProcessingResult = XmlPreProcessor.preProcessXmlResources( managedResources );

		final List<String> allKnownClassNames = CollectionHelper.mutableJoin(
				managedResources.getAnnotatedClassNames(),
				xmlPreProcessingResult.getMappedClasses()
		);
		managedResources.getAnnotatedClassReferences().forEach( (clazz) -> allKnownClassNames.add( clazz.getName() ) );

		// At this point we know all managed class names across all sources.
		// Resolve the Jandex Index and build the SourceModelBuildingContext.
		final IndexView jandexIndex = resolveJandexIndex( allKnownClassNames, bootstrapContext.getJandexView(), classLoading );
		final SourceModelBuildingContextImpl sourceModelBuildingContext = new SourceModelBuildingContextImpl(
				classLoading,
				jandexIndex,
				ManagedResourcesProcessor::preFillRegistries
		);


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
		final ClassDetailsRegistry mutableClassDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();
		final DomainModelCategorizationCollector modelCategorizationCollector = new DomainModelCategorizationCollector(
				areIdGeneratorsGlobal,
				mutableClassDetailsRegistry
		);
		final XmlProcessingResult xmlProcessingResult = XmlProcessor.processXml( xmlPreProcessingResult, modelCategorizationCollector, sourceModelBuildingContext );

		allKnownClassNames.forEach( (className) -> {
			final ClassDetails classDetails = mutableClassDetailsRegistry.resolveClassDetails( className );
			modelCategorizationCollector.apply( classDetails );
		} );
		xmlPreProcessingResult.getMappedNames().forEach( (className) -> {
			final ClassDetails classDetails = mutableClassDetailsRegistry.resolveClassDetails( className );
			modelCategorizationCollector.apply( classDetails );
		} );

		xmlProcessingResult.apply( xmlPreProcessingResult.getPersistenceUnitMetadata(), sourceModelBuildingContext );


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

		final ClassDetailsRegistry classDetailsRegistryImmutable = mutableClassDetailsRegistry
				.makeImmutableCopy();

		final AnnotationDescriptorRegistry annotationDescriptorRegistryImmutable = sourceModelBuildingContext
				.getAnnotationDescriptorRegistry()
				.makeImmutableCopy();

		// Collect the entity hierarchies based on the set of `rootEntities`
		final OrmModelBuildingContextImpl mappingBuildingContext = new OrmModelBuildingContextImpl(
				classDetailsRegistryImmutable,
				annotationDescriptorRegistryImmutable,
				bootstrapContext.getClassmateContext()
		);

		final Set<EntityHierarchy> entityHierarchies;
		if ( OrmModelLogging.ORM_MODEL_LOGGER.isDebugEnabled() ) {
			final Map<String,ClassDetails> unusedMappedSuperClasses = new HashMap<>( modelCategorizationCollector.getMappedSuperclasses() );
			entityHierarchies = createEntityHierarchies(
					modelCategorizationCollector.getRootEntities(),
					(identifiableType) -> {
						if ( identifiableType instanceof MappedSuperclassTypeMetadata ) {
							unusedMappedSuperClasses.remove( identifiableType.getClassDetails().getClassName() );
						}
					},
					mappingBuildingContext
			);
			warnAboutUnusedMappedSuperclasses( unusedMappedSuperClasses );
		}
		else {
			entityHierarchies = createEntityHierarchies(
					modelCategorizationCollector.getRootEntities(),
					ManagedResourcesProcessor::ignore,
					mappingBuildingContext
			);
		}

		return modelCategorizationCollector.createResult( entityHierarchies, classDetailsRegistryImmutable, annotationDescriptorRegistryImmutable );
	}

	private static void ignore(IdentifiableTypeMetadata identifiableTypeMetadata) {
	}

	private static void warnAboutUnusedMappedSuperclasses(Map<String, ClassDetails> mappedSuperClasses) {
		assert OrmModelLogging.ORM_MODEL_LOGGER.isDebugEnabled();
		for ( Map.Entry<String, ClassDetails> entry : mappedSuperClasses.entrySet() ) {
			OrmModelLogging.ORM_MODEL_LOGGER.debugf(
					"Encountered MappedSuperclass [%s] which was unused in any entity hierarchies",
					entry.getKey()
			);
		}
	}

	public static IndexView resolveJandexIndex(
			List<String> allKnownClassNames,
			IndexView suppliedJandexIndex,
			ClassLoading classLoading) {
		// todo : we could build a new Jandex (Composite)Index that includes the `managedResources#getAnnotatedClassNames`
		// 		and all classes from `managedResources#getXmlMappingBindings`.  Only really worth it in the case
		//		of runtime enhancement.  This would definitely need to be toggle-able.
		//		+
		//		For now, let's not as it does not matter for this smoke-test
		if ( 1 == 1 ) {
			return suppliedJandexIndex;
		}

		final Indexer jandexIndexer = new Indexer();
		for ( String knownClassName : allKnownClassNames ) {
			JandexIndexerHelper.apply( knownClassName, jandexIndexer, classLoading );
		}

		if ( suppliedJandexIndex == null ) {
			return jandexIndexer.complete();
		}

		return CompositeIndex.create( suppliedJandexIndex, jandexIndexer.complete() );
	}

	public static void preFillRegistries(RegistryPrimer.Contributions contributions, SourceModelBuildingContext buildingContext) {
		final IndexView jandexIndex = buildingContext.getJandexIndex();
		if ( jandexIndex == null ) {
			return;
		}

		final ClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry();
		final AnnotationDescriptorRegistry annotationDescriptorRegistry = buildingContext.getAnnotationDescriptorRegistry();

		for ( ClassInfo knownClass : jandexIndex.getKnownClasses() ) {
			final String className = knownClass.name().toString();
			if ( className.endsWith( "package-info" ) ) {
				classDetailsRegistry.resolvePackageDetails(
						className,
						() -> new JandexPackageDetails( knownClass, buildingContext )
				);
				continue;
			}

			if ( knownClass.isAnnotation() ) {
				// it is always safe to load the annotation classes - we will never be enhancing them
				//noinspection rawtypes
				final Class annotationClass = buildingContext
						.getClassLoading()
						.classForName( className );
				//noinspection unchecked
				annotationDescriptorRegistry.resolveDescriptor(
						annotationClass,
						(t) -> JdkBuilders.buildAnnotationDescriptor( annotationClass, buildingContext )
				);
			}

			classDetailsRegistry.resolveClassDetails(
					className,
					() -> new JandexClassDetails( knownClass, buildingContext )
			);
		}
	}

}
