/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.source;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
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
/// @author Steve Ebersole
public record AvailableResources(
		Collection<ClassDetails> managedClassDetails,
		Collection<ClassDetails> packageDetails,
		Collection<Binding<? extends JaxbBindableMappingDescriptor>> xmlMappings) {

	/// Creates available resources from Hibernate's descriptor for persistence-unit
	/// information.
	///
	/// Managed class names are resolved through the model context associated with
	/// {@code metadataBuildingContext}.  Mapping file names are located through the
	/// bootstrap class-loading service and bound immediately.
	///
	/// @param persistenceUnitDescriptor The persistence-unit wrapper
	/// @param metadataBuildingContext The bootstrap model building context
	public static AvailableResources from(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			MetadataBuildingContext metadataBuildingContext) {
		var bootstrapContext = metadataBuildingContext.getBootstrapContext();
		var classLoading = bootstrapContext.getClassLoaderService();
		var modelsContext = bootstrapContext.getModelsContext();
		var classDetailsRegistry = modelsContext.getClassDetailsRegistry();

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

			var mappingFileBinder = new MappingBinder( bootstrapContext.getServiceRegistry() );
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
	/// @param metadataBuildingContext The bootstrap model building context
	public static AvailableResources from(
			HibernatePersistenceConfiguration persistenceConfiguration,
			MetadataBuildingContext metadataBuildingContext) {
		// todo : handle discovery/scanning here
		// 		for now though just assume no scanning
		return from( (PersistenceConfiguration) persistenceConfiguration, metadataBuildingContext );
	}

	/// Creates available resources from JPA {@link PersistenceConfiguration}.
	///
	/// Explicit managed classes and mapping files are included.  Discovery/scanning is
	/// not applied here.
	///
	/// @param persistenceConfiguration The PersistenceConfiguration
	/// @param metadataBuildingContext The bootstrap model building context
	public static AvailableResources from(
			PersistenceConfiguration persistenceConfiguration,
			MetadataBuildingContext metadataBuildingContext) {
		var bootstrapContext = metadataBuildingContext.getBootstrapContext();
		var classLoading = bootstrapContext.getClassLoaderService();
		var modelsContext = bootstrapContext.getModelsContext();
		var classDetailsRegistry = modelsContext.getClassDetailsRegistry();

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

			var mappingFileBinder = new MappingBinder( bootstrapContext.getServiceRegistry() );
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

	public Collection<ClassDetails> managedClassDetails() {
		return managedClassDetails == null ? Collections.emptyList() : managedClassDetails;
	}

	public Collection<ClassDetails> packageDetails() {
		return packageDetails == null ? Collections.emptyList() : packageDetails;
	}

	public Collection<Binding<? extends JaxbBindableMappingDescriptor>> xmlMappings() {
		return xmlMappings == null ? Collections.emptyList() : xmlMappings;
	}
}
