/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddable;
import org.hibernate.boot.jaxb.mapping.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.JaxbMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.ManagedType;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.internal.EntityHierarchyBuilder;
import org.hibernate.models.orm.internal.OrmModelBuildingContextImpl;
import org.hibernate.models.orm.internal.OrmModelLogging;
import org.hibernate.models.orm.internal.ProcessResultCollector;
import org.hibernate.models.orm.xml.internal.ManagedTypeProcessor;
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

import static org.hibernate.models.orm.internal.EntityHierarchyBuilder.createEntityHierarchies;
import static org.hibernate.models.orm.xml.internal.ManagedTypeProcessor.processOverrideEmbeddable;
import static org.hibernate.models.orm.xml.internal.ManagedTypeProcessor.processOverrideEntity;
import static org.hibernate.models.orm.xml.internal.ManagedTypeProcessor.processOverrideMappedSuperclass;

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

	public static class OverrideTuple<M extends ManagedType> {
		private final JaxbEntityMappings jaxbRoot;
		private final M managedType;

		public OverrideTuple(JaxbEntityMappings jaxbRoot, M managedType) {
			this.jaxbRoot = jaxbRoot;
			this.managedType = managedType;
		}

		public JaxbEntityMappings getJaxbRoot() {
			return jaxbRoot;
		}

		public M getManagedType() {
			return managedType;
		}
	}

	public static ProcessResult process(
			ManagedResources managedResources,
			List<String> explicitlyListedClasses,
			Options options,
			SourceModelBuildingContext sourceModelBuildingContext,
			OrmModelBuildingContext mappingBuildingContext) {
		final ProcessResultCollector processResultCollector = new ProcessResultCollector( options.areGeneratorsGlobal(), sourceModelBuildingContext );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// process XML
		//		1. Collect and aggregate information from all known XML mappings
		//		2. Handle registrations (JavaType, etc.) and named references (named query, etc.)
		//		3. Process managed types -
		//			a. create ClassDetails for all "complete" mappings
		//			b. collect "incomplete" (override) mappings
		//		4. Apply XML overrides

		final XmlResources collectedXmlResources = XmlResources.collectXmlResources( managedResources, sourceModelBuildingContext );
		final boolean xmlMappingsGloballyComplete = collectedXmlResources.getPersistenceUnitMetadata().areXmlMappingsComplete();

		final List<OverrideTuple<JaxbEntity>> entityOverrides = new ArrayList<>();
		final List<OverrideTuple<JaxbMappedSuperclass>> mappedSuperclassesOverrides = new ArrayList<>();
		final List<OverrideTuple<JaxbEmbeddable>> embeddableOverrides = new ArrayList<>();

		collectedXmlResources.getDocuments().forEach( (jaxbRoot) -> {
			processResultCollector.apply( jaxbRoot );

			jaxbRoot.getEmbeddables().forEach( (embeddable) -> {
				if ( xmlMappingsGloballyComplete || embeddable.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteEmbeddable( jaxbRoot, embeddable, collectedXmlResources.getPersistenceUnitMetadata(), sourceModelBuildingContext );
				}
				else {
					// otherwise, wait to process it until later
					embeddableOverrides.add( new OverrideTuple<>( jaxbRoot, embeddable ) );
				}
			} );

			jaxbRoot.getMappedSuperclasses().forEach( (mappedSuperclass) -> {
				if ( xmlMappingsGloballyComplete || mappedSuperclass.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteMappedSuperclass( jaxbRoot, mappedSuperclass, collectedXmlResources.getPersistenceUnitMetadata(), sourceModelBuildingContext );
				}
				else {
					// otherwise, wait to process it until later
					mappedSuperclassesOverrides.add( new OverrideTuple<>( jaxbRoot, mappedSuperclass ) );
				}
			});

			jaxbRoot.getEntities().forEach( (entity) -> {
				if ( xmlMappingsGloballyComplete || entity.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteEntity( jaxbRoot, entity, collectedXmlResources.getPersistenceUnitMetadata(), sourceModelBuildingContext );
				}
				else {
					// otherwise, wait to process it until later
					entityOverrides.add( new OverrideTuple<>( jaxbRoot, entity ) );
				}
			} );
		} );

		// At this point, we know all classes in the persistence-unit - begin to process them.
		// But we need to account for `<exclude-unlisted-classes/>` from `persistence.xml` - set up
		// `classInclusions` and `packageInclusions` to handle `<exclude-unlisted-classes/>`
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

		// Now inclusions/exclusions are set up, begin processing the classes in earnest.
		// This includes a number of parts -
		//		1. process "global" annotations - id generators, registrations, named references
		//		2. collect the complete sets of
		//			a. root entities
		//			b. mapped-superclasses
		//			c. *explicit* embeddables[1]
		//		3. apply mapping XML overrides
		//
		// [1] Hibernate supports implicit embeddables, where the class does not define `@Embeddable` but the persistent attribute defines `@Embedded` or `@EmbeddedId`

		final Set<ClassDetails> rootEntities = new HashSet<>();
		final Map<String,ClassDetails> mappedSuperClasses = new HashMap<>();
		final Map<String,ClassDetails> embeddables = new HashMap<>();

		final Map<String,ClassDetails> unusedMappedSuperClasses = new HashMap<>();

		processResources(
				classInclusions,
				packageInclusions,
				processResultCollector,
				rootEntities,
				mappedSuperClasses,
				embeddables,
				unusedMappedSuperClasses,
				mappingBuildingContext
		);

		processOverrideEntity(
				entityOverrides,
				collectedXmlResources.getPersistenceUnitMetadata(),
				sourceModelBuildingContext
		);

		processOverrideMappedSuperclass(
				mappedSuperclassesOverrides,
				collectedXmlResources.getPersistenceUnitMetadata(),
				sourceModelBuildingContext
		);

		processOverrideEmbeddable(
				embeddableOverrides,
				collectedXmlResources.getPersistenceUnitMetadata(),
				sourceModelBuildingContext
		);

		// Collect the entity hierarchies based on the set of `rootEntities`
		final Set<EntityHierarchy> entityHierarchies = createEntityHierarchies(
				rootEntities,
				(identifiableType) -> {
					if ( identifiableType instanceof MappedSuperclassTypeMetadata ) {
						unusedMappedSuperClasses.remove( identifiableType.getClassDetails().getClassName() );
					}
				},
				mappingBuildingContext
		);

		if ( OrmModelLogging.ORM_MODEL_LOGGER.isDebugEnabled() ) {
			warnAboutUnusedMappedSuperclasses( unusedMappedSuperClasses );
		}

		return processResultCollector.createResult( entityHierarchies, mappedSuperClasses, embeddables );
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
		final ClassLoading classLoading = sourceModelBuildingContext.getClassLoading();

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

	private static void processResources(
			ClassInclusions classInclusions,
			PackageInclusions packageInclusions,
			ProcessResultCollector processResultCollector,
			Set<ClassDetails> rootEntities,
			Map<String,ClassDetails> mappedSuperClasses,
			Map<String,ClassDetails> embeddables,
			Map<String,ClassDetails> unusedMappedSuperClasses,
			OrmModelBuildingContext mappingBuildingContext) {
		final ClassDetailsRegistry classDetailsRegistry = mappingBuildingContext.getClassDetailsRegistry();
		classDetailsRegistry.forEachClassDetails( (classDetails) -> {
			if ( classInclusions != null && !classInclusions.shouldInclude( classDetails ) ) {
				// skip this class
				return;
			}

			processResultCollector.apply( classDetails );

			if ( classDetails.getAnnotationUsage( JpaAnnotations.MAPPED_SUPERCLASS ) != null ) {
				unusedMappedSuperClasses.put( classDetails.getName(), classDetails );
				if ( classDetails.getClassName() != null ) {
					mappedSuperClasses.put( classDetails.getClassName(), classDetails );
				}
			}
			else if ( classDetails.getAnnotationUsage( JpaAnnotations.ENTITY ) != null ) {
				if ( EntityHierarchyBuilder.isRoot( classDetails ) ) {
					rootEntities.add( classDetails );
				}
			}
			else if ( classDetails.getAnnotationUsage( Embeddable.class ) != null ) {
				if ( classDetails.getClassName() != null ) {
					embeddables.put( classDetails.getClassName(), classDetails );
				}
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
}
