/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.resources;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class AvailableResourcesTests {
	@Test
	void nullCollectionsAreExposedAsEmptyCollections() {
		final AvailableResources availableResources = new AvailableResources( null, null, null );

		assertThat( availableResources.managedClassDetails() ).isEmpty();
		assertThat( availableResources.packageDetails() ).isEmpty();
		assertThat( availableResources.xmlMappings() ).isEmpty();
	}

	@Test
	void testPersistenceConfigurationSource(ServiceRegistryScope registryScope) {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );
		var classLoading = registryScope.getRegistry().requireService( ClassLoaderService.class );

		var config = new HibernatePersistenceConfiguration( "test" );
		config.managedClass( SimpleEntity.class );
		config.managedClass( classLoading.classForName( "org.hibernate.models.orm.resources.package-info" ) );
		config.mappingFile( "mappings/available.xml" );

		var modelSources = AvailableResources.from( config, buildingContext );

		assertThat( modelSources.managedClassDetails() ).hasSize( 1 );
		assertThat( modelSources.packageDetails() ).hasSize( 1 );
		assertThat( modelSources.xmlMappings() ).hasSize( 1 );
	}

	@Test
	void testPersistenceUnitInfoSource(ServiceRegistryScope registryScope) {
		var buildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );

		var pui = new PersistenceUnitInfoAdapter();
		pui.managedClassNames.add( SimpleEntity.class.getName() );
		pui.managedClassNames.add( "org.hibernate.models.orm.resources.package-info" );
		pui.mappingFiles.add( "mappings/available.xml" );

		var puiWrapper = new PersistenceUnitInfoDescriptor( pui );
		var modelSources = AvailableResources.from( puiWrapper, buildingContext );

		assertThat( modelSources.managedClassDetails() ).hasSize( 1 );
		assertThat( modelSources.packageDetails() ).hasSize( 1 );
		assertThat( modelSources.xmlMappings() ).hasSize( 1 );
	}


	private static class PersistenceUnitInfoAdapter extends org.hibernate.testing.orm.jpa.PersistenceUnitInfoAdapter {
		private final List<String> managedClassNames = new ArrayList<>();
		private final List<String> mappingFiles = new ArrayList<>();

		@Override
		public List<String> getMappingFileNames() {
			return mappingFiles;
		}

		@Override
		public List<String> getManagedClassNames() {
			return managedClassNames;
		}
	}
}
