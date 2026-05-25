/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.source;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.PersistenceConfiguration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.boot.settings.BootstrapSettingsResolver;
import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.ClassDetails;

/// Model resources available to categorization.
///
/// The record separates the source material into three buckets:
///
/// * class details for explicitly visible managed classes and dynamic model types
/// * class details for package metadata, represented by {@code package-info}
///   class details
/// * already-bound XML mapping documents
///
/// The canonical constructor accepts nullable collections for convenience; the
/// accessor methods expose them as empty collections.
///
/// @apiNote Hibernate hbm.xml bindings are intentionally ignored here. 9.0 will
/// drop support for them altogether.
///
/// @author Steve Ebersole
public record AvailableResources(
		Collection<ClassDetails> managedClassDetails,
		Collection<ClassDetails> packageDetails,
		Collection<Binding<? extends JaxbBindableMappingDescriptor>> xmlMappings) {

	@NonNull
	public Collection<ClassDetails> managedClassDetails() {
		return managedClassDetails == null ? Collections.emptyList() : managedClassDetails;
	}

	@NonNull
	public Collection<ClassDetails> packageDetails() {
		return packageDetails == null ? Collections.emptyList() : packageDetails;
	}

	@NonNull
	public Collection<Binding<? extends JaxbBindableMappingDescriptor>> xmlMappings() {
		return xmlMappings == null ? Collections.emptyList() : xmlMappings;
	}

	/// Creates available resources from Hibernate's descriptor for persistence-unit
	/// information.
	///
	/// Managed class names are resolved through the supplied model context. Mapping
	/// file names are located through the bootstrap class-loading service and bound
	/// immediately.
	///
	/// @param persistenceUnitDescriptor The persistence-unit wrapper
	/// @param context Context used to resolve model details and load resources
	public static AvailableResources from(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			AvailableResourcesContext context) {
		var classLoading = context.getClassLoaderService();
		var classDetailsRegistry = context.modelsContext().getClassDetailsRegistry();

		var managedClassDetails = new ArrayList<ClassDetails>();
		var packageDetailsList = new ArrayList<ClassDetails>();
		persistenceUnitDescriptor.getManagedClassNames().forEach( (managedClassName) -> {
			var classDetails = classDetailsRegistry.resolveClassDetails( managedClassName );
			if ( StringHelper.isEmpty( classDetails.getClassName() ) ) {
				managedClassDetails.add( classDetails );
			}
			else {
				applyClassDetails( classDetails, managedClassDetails, packageDetailsList );
			}
		} );

		final List<Binding<? extends JaxbBindableMappingDescriptor>> xmlBindings;
		if ( persistenceUnitDescriptor.getMappingFileNames().isEmpty() ) {
			xmlBindings = Collections.emptyList();
		}
		else {
			xmlBindings = new ArrayList<>();

			var mappingFileBinder = context.createMappingBinder();
			persistenceUnitDescriptor.getMappingFileNames().forEach( (mappingFile) -> {
				try (var mappingFileStream = classLoading.locateResourceStream( mappingFile )) {
					xmlBindings.add( mappingFileBinder.bind(
							mappingFileStream,
							new Origin( SourceType.RESOURCE, mappingFile )
					) );
				}
				catch (IOException e) {
					throw new RuntimeException( "Error accessing mapping file - " + mappingFile, e );
				}
			} );
		}

		return new AvailableResources( managedClassDetails, packageDetailsList, xmlBindings );
	}

	/// Creates available resources from Hibernate's JPA
	/// {@link HibernatePersistenceConfiguration} extension.
	///
	/// Explicit managed classes and mapping files are included.  Discovery/scanning is
	/// applied here based on [HibernatePersistenceConfiguration#rootUrl()] and
	/// [HibernatePersistenceConfiguration#jarFileUrls()].
	///
	/// @param persistenceConfiguration The PersistenceConfiguration
	/// @param context Context used to resolve model details and load resources
	public static AvailableResources from(
			HibernatePersistenceConfiguration persistenceConfiguration,
			AvailableResourcesContext context) {
		return from(
				persistenceConfiguration,
				context,
				new BootstrapSettingsResolver().resolve( persistenceConfiguration )
		);
	}

	/// Creates available resources from Hibernate's JPA
	/// {@link HibernatePersistenceConfiguration} extension.
	///
	/// Explicit managed classes and mapping files are included.  Discovery/scanning is
	/// applied here based on [HibernatePersistenceConfiguration#rootUrl()] and
	/// [HibernatePersistenceConfiguration#jarFileUrls()].
	///
	/// @param persistenceConfiguration The PersistenceConfiguration
	/// @param context Context used to resolve model details and load resources
	/// @param bootstrapSettings Resolved bootstrap settings used during source discovery
	public static AvailableResources from(
			HibernatePersistenceConfiguration persistenceConfiguration,
			AvailableResourcesContext context,
			ResolvedBootstrapSettings bootstrapSettings) {
		final var classLoading = context.getClassLoaderService();
		final var classDetailsRegistry = context.modelsContext().getClassDetailsRegistry();
		final var mappingFileBinder = context.createMappingBinder();

		var managedClassDetails = new ArrayList<ClassDetails>();
		var packageDetailsList = new ArrayList<ClassDetails>();
		persistenceConfiguration.managedClasses().forEach( (managedClass) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( managedClass.getName() ),
					managedClassDetails,
					packageDetailsList
			);
		} );

		final ScanningResult scanningResult = HibernatePersistenceConfigurationScanner.performScanning(
				persistenceConfiguration,
				bootstrapSettings,
				classLoading
		);
		applyDiscoveredClassDetails(
				scanningResult,
				context.modelsContext(),
				managedClassDetails,
				packageDetailsList
		);

		final var xmlBindings = new ArrayList<Binding<? extends JaxbBindableMappingDescriptor>>();
		persistenceConfiguration.mappingFiles().forEach( (mappingFile) -> {
			try (var mappingFileStream = classLoading.locateResourceStream( mappingFile )) {
				xmlBindings.add( mappingFileBinder.bind(
						mappingFileStream,
						new Origin( SourceType.RESOURCE, mappingFile )
				) );
			}
			catch (IOException e) {
				throw new RuntimeException( "Error accessing mapping file - " + mappingFile, e );
			}
		} );
		applyDiscoveredXmlMappings( scanningResult, mappingFileBinder, xmlBindings );

		return new AvailableResources( managedClassDetails, packageDetailsList, xmlBindings );
	}

	/// Creates available resources from JPA {@link PersistenceConfiguration}.
	///
	/// Explicit managed classes and mapping files are included.  Discovery/scanning is
	/// not applied here.
	///
	/// @param persistenceConfiguration The PersistenceConfiguration
	/// @param context Context used to resolve model details and load resources
	public static AvailableResources from(
			PersistenceConfiguration persistenceConfiguration,
			AvailableResourcesContext context) {
		var classLoading = context.getClassLoaderService();
		var classDetailsRegistry = context.modelsContext().getClassDetailsRegistry();

		var managedClassDetails = new ArrayList<ClassDetails>();
		var packageDetailsList = new ArrayList<ClassDetails>();
		persistenceConfiguration.managedClasses().forEach( (managedClass) -> {
			var classDetails = classDetailsRegistry.resolveClassDetails( managedClass.getName() );
			if ( StringHelper.isEmpty( classDetails.getClassName() ) ) {
				managedClassDetails.add( classDetails );
			}
			else {
				applyClassDetails( classDetails, managedClassDetails, packageDetailsList );
			}
		} );

		final List<Binding<? extends JaxbBindableMappingDescriptor>> xmlBindings;
		if ( persistenceConfiguration.mappingFiles().isEmpty() ) {
			xmlBindings = Collections.emptyList();
		}
		else {
			xmlBindings = new ArrayList<>();

			var mappingFileBinder = context.createMappingBinder();
			persistenceConfiguration.mappingFiles().forEach( (mappingFile) -> {
				try (var mappingFileStream = classLoading.locateResourceStream( mappingFile )) {
					xmlBindings.add( mappingFileBinder.bind(
							mappingFileStream,
							new Origin( SourceType.RESOURCE, mappingFile )
					) );
				}
				catch (IOException e) {
					throw new RuntimeException( "Error accessing mapping file - " + mappingFile, e );
				}
			} );
		}

		return new AvailableResources( managedClassDetails, packageDetailsList, xmlBindings );
	}

	/// Creates available resources from Hibernate's native source accumulator.
	///
	/// Annotated classes, annotated class names, and package names are resolved
	/// through the supplied model context.  Mapping XML bindings already collected
	/// by {@code metadataSources} are carried forward directly.
	///
	/// @param metadataSources The native source accumulator
	/// @param context Context used to resolve model details
	public static AvailableResources from(
			MetadataSources metadataSources,
			AvailableResourcesContext context) {
		var classDetailsRegistry = context.modelsContext().getClassDetailsRegistry();

		var managedClassDetails = new ArrayList<ClassDetails>();
		var packageDetailsList = new ArrayList<ClassDetails>();
		metadataSources.getAnnotatedClasses().forEach( (annotatedClass) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( annotatedClass.getName() ),
					managedClassDetails,
					packageDetailsList
			);
		} );
		metadataSources.getAnnotatedClassNames().forEach( (annotatedClassName) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( annotatedClassName ),
					managedClassDetails,
					packageDetailsList
			);
		} );
		metadataSources.getAnnotatedPackages().forEach( (packageName) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( packageName + ".package-info" ),
					managedClassDetails,
					packageDetailsList
			);
		} );

		final var xmlBindings = new ArrayList<Binding<? extends JaxbBindableMappingDescriptor>>();
		xmlBindings.addAll( metadataSources.getMappingXmlBindings() );
		return new AvailableResources(
				managedClassDetails,
				packageDetailsList,
				xmlBindings
		);
	}

	private static void applyClassDetails(
			ClassDetails classDetails,
			Collection<ClassDetails> managedClassDetails,
			Collection<ClassDetails> packageDetails) {
		if ( StringHelper.isEmpty( classDetails.getClassName() ) ) {
			managedClassDetails.add( classDetails );
		}
		else if ( classDetails.getClassName().endsWith( "package-info" ) ) {
			packageDetails.add( classDetails );
		}
		else {
			managedClassDetails.add( classDetails );
		}
	}

	private static void applyDiscoveredClassDetails(
			ScanningResult scanningResult,
			ModelsContext modelsContext,
			Collection<ClassDetails> managedClassDetails,
			Collection<ClassDetails> packageDetailsList) {
		var classDetailsRegistry = modelsContext.getClassDetailsRegistry();
		scanningResult.discoveredClasses().forEach( (className) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( className ),
					managedClassDetails,
					packageDetailsList
			);
		} );
		scanningResult.discoveredPackages().forEach( (packageName) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( packageName + ".package-info" ),
					managedClassDetails,
					packageDetailsList
			);
		} );
	}

	private static void applyDiscoveredXmlMappings(
			ScanningResult scanningResult,
			MappingBinder mappingFileBinder,
			Collection<Binding<? extends JaxbBindableMappingDescriptor>> xmlBindings) {
		scanningResult.mappingFiles().forEach( (mappingFile) -> {
			xmlBindings.add( bindMappingFile( mappingFile, mappingFileBinder ) );
		} );
	}

	private static Binding<? extends JaxbBindableMappingDescriptor> bindMappingFile(
			URI mappingFile,
			MappingBinder mappingFileBinder) {
		try {
			return org.hibernate.boot.jaxb.internal.UrlXmlSource.fromUrl( mappingFile.toURL(), mappingFileBinder );
		}
		catch (MalformedURLException e) {
			throw new RuntimeException( "Error accessing mapping file - " + mappingFile, e );
		}
	}
}
