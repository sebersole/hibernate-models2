/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddable;
import org.hibernate.boot.jaxb.mapping.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.JaxbMappedSuperclass;
import org.hibernate.boot.jaxb.spi.BindableMappingDescriptor;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.internal.EntityHierarchyBuilder;
import org.hibernate.models.orm.internal.OrmModelBuildingContextImpl;
import org.hibernate.models.orm.internal.OrmModelLogging;
import org.hibernate.models.orm.internal.ProcessResultCollector;
import org.hibernate.models.orm.xml.XmlResourceException;
import org.hibernate.models.orm.xml.internal.ResourceStreamLocatorImpl;
import org.hibernate.models.orm.xml.internal.XmlManagedTypeHelper;
import org.hibernate.models.orm.xml.spi.XmlResources;
import org.hibernate.models.source.UnknownClassException;
import org.hibernate.models.source.internal.jandex.JandexClassDetails;
import org.hibernate.models.source.internal.jandex.JandexPackageDetails;
import org.hibernate.models.source.internal.jdk.JdkBuilders;
import org.hibernate.models.source.internal.jdk.JdkPackageDetailsImpl;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.PackageDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.ClassLoading;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import jakarta.persistence.Embeddable;

import static org.hibernate.boot.jaxb.internal.MappingBinder.NON_VALIDATING;
import static org.hibernate.models.orm.internal.EntityHierarchyBuilder.createEntityHierarchies;

/**
 * Processes {@linkplain ManagedResources managed resources} and produces a
 * {@linkplain ProcessResult result} which collects broad categorizations of the
 * classes defined by those resources based on annotations (and XML mappings).
 *
 * @author Steve Ebersole
 */
public class Processor {
	public interface Options {
		boolean areGeneratorsGlobal();
		boolean shouldIgnoreUnlistedClasses();
	}

	public static ProcessResult process(
			ManagedResources managedResources,
			List<String> explicitlyListedClasses,
			Options options,
			SourceModelBuildingContext sourceModelBuildingContext) {
		fillRegistries( sourceModelBuildingContext );

		final OrmModelBuildingContext ormModelBuildingContext = new OrmModelBuildingContextImpl(
				sourceModelBuildingContext,
				new ClassmateContext()
		);

		return process( managedResources, explicitlyListedClasses, options, sourceModelBuildingContext, ormModelBuildingContext );
	}

	public static ProcessResult process(
			ManagedResources managedResources,
			List<String> explicitlyListedClasses,
			Options options,
			SourceModelBuildingContext sourceModelBuildingContext,
			OrmModelBuildingContext mappingBuildingContext) {
		final XmlResources collectedXmlResources = collectXmlResources( managedResources, mappingBuildingContext );
		final ProcessResultCollector processResultCollector = new ProcessResultCollector( options.areGeneratorsGlobal(), sourceModelBuildingContext );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// process XML
		//		1. handle registrations (JavaType, etc.) and named references (named query, etc.)
		//		2. process managed types -
		//			a. create ClassDetails for all "complete" mappings
		//			b. collect "incomplete" (override) mappings
		//		3. apply XML overrides

		final List<JaxbEntity> entityOverrides = new ArrayList<>();
		final List<JaxbEntity> entityCompletes = new ArrayList<>();
		final List<JaxbMappedSuperclass> mappedSuperclassesOverrides = new ArrayList<>();
		final List<JaxbMappedSuperclass> mappedSuperclassesCompletes = new ArrayList<>();
		final List<JaxbEmbeddable> embeddableOverrides = new ArrayList<>();
		final List<JaxbEmbeddable> embeddableCompletes = new ArrayList<>();

		final boolean xmlMappingsGloballyComplete = collectedXmlResources.getPersistenceUnitMetadata().areXmlMappingsComplete();

		collectedXmlResources.getDocuments().forEach( (jaxbRoot) -> {
			processResultCollector.apply( jaxbRoot );

			jaxbRoot.getEmbeddables().forEach( (embeddable) -> {
				if ( xmlMappingsGloballyComplete || embeddable.isMetadataComplete() ) {
					embeddableCompletes.add( embeddable );
					XmlManagedTypeHelper.makeCompleteEmbeddableMapping( jaxbRoot, embeddable, collectedXmlResources.getPersistenceUnitMetadata(), sourceModelBuildingContext );
				}
				else {
					embeddableOverrides.add( embeddable );
				}
			} );

			jaxbRoot.getMappedSuperclasses().forEach( (mappedSuperclass) -> {
				if ( xmlMappingsGloballyComplete || mappedSuperclass.isMetadataComplete() ) {
					mappedSuperclassesCompletes.add( mappedSuperclass );
					XmlManagedTypeHelper.makeCompleteMappedSuperclassMapping( jaxbRoot, mappedSuperclass, collectedXmlResources.getPersistenceUnitMetadata(), sourceModelBuildingContext );
				}
				else {
					mappedSuperclassesOverrides.add( mappedSuperclass );
				}
			});

			jaxbRoot.getEntities().forEach( (entity) -> {
				if ( xmlMappingsGloballyComplete || entity.isMetadataComplete() ) {
					entityCompletes.add( entity );
					XmlManagedTypeHelper.makeCompleteEntityMapping( jaxbRoot, entity, collectedXmlResources.getPersistenceUnitMetadata(), sourceModelBuildingContext );
				}
				else {
					entityOverrides.add( entity );
				}
			} );
		} );

		final ActiveClassInclusions classInclusions;
		final ActivePackageInclusions packageInclusions;
		if ( options.shouldIgnoreUnlistedClasses() ) {
			classInclusions = new ActiveClassInclusions();
			packageInclusions = new ActivePackageInclusions();
			if ( CollectionHelper.isEmpty( explicitlyListedClasses ) ) {
				OrmModelLogging.ORM_MODEL_LOGGER.debugf( "Ignore unlisted classes was requested, but no classes were listed" );
			}
			else {
				collectListedResources( explicitlyListedClasses, classInclusions, packageInclusions, sourceModelBuildingContext );
			}
		}
		else {
			classInclusions = null;
			packageInclusions = null;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// process "global" annotations - id generators, registrations, named references
		// todo : possibly handle annotation versions of registrations and named references at the same time as (XML#1)?

		final Set<ClassDetails> rootEntities = new HashSet<>();
		final Map<String,ClassDetails> allEntities = new HashMap<>();
		final Map<String,ClassDetails> mappedSuperClasses = new HashMap<>();
		final Map<String,ClassDetails> embeddables = new HashMap<>();

		processResources(
				classInclusions,
				packageInclusions,
				processResultCollector,
				rootEntities,
				allEntities,
				mappedSuperClasses,
				embeddables,
				mappingBuildingContext
		);

		XmlManagedTypeHelper.applyEntityOverrides( allEntities, entityOverrides );
		XmlManagedTypeHelper.applyMappedSuperclassOverrides( mappedSuperClasses, mappedSuperclassesOverrides );
		XmlManagedTypeHelper.applyEmbeddableOverrides( embeddables, embeddableOverrides );

		final Set<EntityHierarchy> entityHierarchies = createEntityHierarchies(
				rootEntities,
				(identifiableType) -> {
					if ( identifiableType instanceof MappedSuperclassTypeMetadata ) {
						mappedSuperClasses.remove( identifiableType.getClassDetails().getClassName() );
					}
				},
				mappingBuildingContext
		);

		if ( OrmModelLogging.ORM_MODEL_LOGGER.isDebugEnabled() ) {
			warnAboutUnusedMappedSuperclasses( mappedSuperClasses );
		}

		return processResultCollector.createResult( entityHierarchies );
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

	private static void collectListedResources(
			List<String> explicitlyListedClasses,
			ActiveClassInclusions classInclusions,
			ActivePackageInclusions packageInclusions,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();
		final ClassLoading classLoading = sourceModelBuildingContext.getClassLoadingAccess();

		explicitlyListedClasses.forEach( (listed) -> {
			if ( listed.endsWith( "package-info" ) ) {
				// we know it is a package
				final String packageName = StringHelper.qualifier( listed );
				final Package packageForName = classLoading.packageForName( packageName );
				assert packageForName != null;
				packageInclusions.addInclusion( classDetailsRegistry.resolvePackageDetails(
						packageName,
						() -> new JdkPackageDetailsImpl( packageForName, sourceModelBuildingContext )
				) );
			}
			else {
				// could be a class or package
				try {
					final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( listed );
					if ( classDetails != null ) {
						classInclusions.addInclusion( classDetails );
					}
				}
				catch (UnknownClassException e) {
					// see if it is a package
					final Package packageForName = classLoading.packageForName( listed );
					if ( packageForName == null ) {
						// todo : what to do here?
					}
					else {
						packageInclusions.addInclusion( classDetailsRegistry.resolvePackageDetails(
								listed,
								() -> new JdkPackageDetailsImpl( packageForName, sourceModelBuildingContext )
						) );
					}
				}
			}
		} );
	}

	private static XmlResources collectXmlResources(
			ManagedResources managedResources,
			OrmModelBuildingContext mappingBuildingContext) {
		final ClassLoading classLoading = mappingBuildingContext.getClassLoading();
		final ResourceStreamLocatorImpl resourceStreamLocator = new ResourceStreamLocatorImpl( classLoading );
		final MappingBinder mappingBinder = new MappingBinder( resourceStreamLocator, NON_VALIDATING );
		final XmlResources collected = new XmlResources();

		final List<String> xmlMappings = managedResources.getXmlMappings();
		for ( int i = 0; i < xmlMappings.size(); i++ ) {
			final String xmlMapping = xmlMappings.get( i );
			final URL resource = classLoading.locateResource( xmlMapping );
			if ( resource == null ) {
				throw new XmlResourceException( "Unable to locate XML mapping - " + xmlMapping );
			}
			try (InputStream inputStream = resource.openStream()) {
				final Binding<BindableMappingDescriptor> binding = mappingBinder.bind(
						inputStream,
						new Origin( SourceType.RESOURCE, xmlMapping )
				);
				collected.addDocument( (JaxbEntityMappings) binding.getRoot() );
			}
			catch (IOException e) {
				throw new XmlResourceException( "Unable to bind XML mapping - " + xmlMapping, e );
			}
		}

		return collected;
	}

	private static void fillRegistries(SourceModelBuildingContext buildingContext) {
		final ClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry();
		final AnnotationDescriptorRegistry annotationDescriptorRegistry = buildingContext.getAnnotationDescriptorRegistry();
		final IndexView jandexIndex = buildingContext.getJandexIndex();

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
						.getClassLoadingAccess()
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

	private static void processResources(
			ClassInclusions classInclusions,
			PackageInclusions packageInclusions,
			ProcessResultCollector processResultCollector,
			Set<ClassDetails> rootEntities,
			Map<String,ClassDetails> allEntities,
			Map<String,ClassDetails> mappedSuperClasses,
			Map<String,ClassDetails> embeddables,
			OrmModelBuildingContext mappingBuildingContext) {
		final ClassDetailsRegistry classDetailsRegistry = mappingBuildingContext.getClassDetailsRegistry();
		classDetailsRegistry.forEachClassDetails( (classDetails) -> {
			if ( classInclusions != null && !classInclusions.shouldInclude( classDetails ) ) {
				// skip this class
				return;
			}

			processResultCollector.apply( classDetails );

			if ( classDetails.getAnnotationUsage( JpaAnnotations.MAPPED_SUPERCLASS ) != null ) {
				if ( classDetails.getClassName() != null ) {
					mappedSuperClasses.put( classDetails.getClassName(), classDetails );
				}
				processIdentifiableType( classDetails, mappingBuildingContext );
			}
			else if ( classDetails.getAnnotationUsage( JpaAnnotations.ENTITY ) != null ) {
				if ( classDetails.getClassName() != null ) {
					allEntities.put( classDetails.getClassName(), classDetails );
				}
				if ( EntityHierarchyBuilder.isRoot( classDetails ) ) {
					rootEntities.add( classDetails );
				}
				processIdentifiableType( classDetails, mappingBuildingContext );
			}
			else if ( classDetails.getAnnotationUsage( Embeddable.class ) != null ) {
				if ( classDetails.getClassName() != null ) {
					embeddables.put( classDetails.getClassName(), classDetails );
				}
				processNonIdentifiableType( classDetails, mappingBuildingContext );
			}
			else {
				processNonIdentifiableType( classDetails, mappingBuildingContext );
			}
		} );

		classDetailsRegistry.forEachPackageDetails( (packageDetails) -> {
			if ( packageInclusions != null && !packageInclusions.shouldInclude( packageDetails ) ) {
				// skip this class
				return;
			}

			processResultCollector.apply( packageDetails );
		} );
	}

	@FunctionalInterface
	private interface ClassInclusions {
		boolean shouldInclude(ClassDetails classDetails);
	}

	private static class ActiveClassInclusions implements ClassInclusions {
		private final Set<ClassDetails> inclusionList = new HashSet<>();

		private void addInclusion(ClassDetails classDetails) {
			inclusionList.add( classDetails );
		}

		@Override
		public boolean shouldInclude(ClassDetails classDetails) {
			return inclusionList.contains( classDetails );
		}
	}

	@FunctionalInterface
	private interface PackageInclusions {
		boolean shouldInclude(PackageDetails packageDetails);
	}

	private static class ActivePackageInclusions implements PackageInclusions {
		private final Set<PackageDetails> inclusionList = new HashSet<>();

		private void addInclusion(PackageDetails packageDetails) {
			inclusionList.add( packageDetails );
		}

		@Override
		public boolean shouldInclude(PackageDetails packageDetails) {
			return inclusionList.contains( packageDetails );
		}
	}

	private static void processIdentifiableType(
			ClassDetails classDetails,
			OrmModelBuildingContext mappingBuildingContext) {

	}

	private static void processNonIdentifiableType(
			ClassDetails classDetails,
			OrmModelBuildingContext mappingBuildingContext) {

	}

}
