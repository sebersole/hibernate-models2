/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

import java.util.Map;

import org.hibernate.boot.settings.BootstrapSettingsResolver;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.models.orm.resources.SimpleEntity;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class TestBootModelProducerTests {
	@Test
	void testRequestDrivenBootstrap(ServiceRegistryScope registryScope) {
		final var metadata = TestBootModelProducer.buildMetadata(
				registryScope.getRegistry(),
				SimpleEntity.class
		);

		final var entityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertThat( entityBinding ).isNotNull();
		assertThat( entityBinding.getIdentifierProperty().getName() ).isEqualTo( "id" );
		assertThat( entityBinding.getProperty( "name" ) ).isNotNull();
	}

	@Test
	void testClassConvenienceOverload(ServiceRegistryScope registryScope) {
		final var metadata = TestBootModelProducer.buildMetadata(
				registryScope.getRegistry(),
				SimpleEntity.class
		);

		assertThat( metadata.getEntityBindings() ).hasSize( 1 );
		final var entityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertThat( entityBinding ).isNotNull();
		assertThat( entityBinding.getTable() ).isNotNull();
		assertThat( entityBinding.getTable().getName() ).isNotBlank();
	}

	@Test
	void testAvailableResourcesHonorsXmlMappingSetting(ServiceRegistryScope registryScope) {
		final var metadataBuildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		persistenceConfiguration.mappingFile( "mappings/complete/simple-complete.xml" );

		final var settings = new BootstrapSettingsResolver().resolve(
				Map.of( MappingSettings.XML_MAPPING_ENABLED, false )
		);
		final var availableResources = TestBootModelProducer.availableResources(
				metadataBuildingContext,
				persistenceConfiguration,
				settings
		);

		assertThat( availableResources.xmlMappings() ).isEmpty();
	}
}
