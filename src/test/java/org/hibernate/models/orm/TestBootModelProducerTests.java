/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

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
	void testExplicitAvailableResources(ServiceRegistryScope registryScope) {
		final var metadataBuildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );
		final var availableResources = TestBootModelProducer.availableResources(
				metadataBuildingContext,
				SimpleEntity.class
		);

		final var metadata = TestBootModelProducer.buildMetadata(
				availableResources,
				metadataBuildingContext
		);

		assertThat( metadata ).isSameAs( metadataBuildingContext.getMetadataCollector() );
		final var entityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertThat( entityBinding ).isNotNull();
		assertThat( entityBinding.getIdentifierProperty().getName() ).isEqualTo( "id" );
		assertThat( entityBinding.getProperty( "name" ) ).isNotNull();
	}

	@Test
	void testClassConvenienceOverload(ServiceRegistryScope registryScope) {
		final var metadataBuildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );

		final var metadata = TestBootModelProducer.buildMetadata(
				metadataBuildingContext,
				SimpleEntity.class
		);

		assertThat( metadata.getEntityBindings() ).hasSize( 1 );
		final var entityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertThat( entityBinding ).isNotNull();
		assertThat( entityBinding.getTable() ).isNotNull();
		assertThat( entityBinding.getTable().getName() ).isNotBlank();
	}
}
