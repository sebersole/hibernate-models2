/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.orchestration.SessionFactoryBootstrap;
import org.hibernate.boot.orchestration.SessionFactoryBootstrapRequest;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;

/**
 * Test helper that composes categorization and binding into ORM boot metadata.
 *
 * @author Steve Ebersole
 */
public class TestBootModelProducer {
	public static MetadataImplementor buildMetadata(
			AvailableResources availableResources,
			MetadataBuildingContext metadataBuildingContext) {
		return new SessionFactoryBootstrap().buildMetadata(
				new SessionFactoryBootstrapRequest( availableResources, metadataBuildingContext )
		);
	}

	public static MetadataImplementor buildMetadata(
			MetadataBuildingContext metadataBuildingContext,
			Class<?>... domainClasses) {
		return buildMetadata(
				availableResources( metadataBuildingContext, domainClasses ),
				metadataBuildingContext
		);
	}

	public static MetadataImplementor buildMetadata(
			MetadataBuildingContext metadataBuildingContext,
			HibernatePersistenceConfiguration persistenceConfiguration) {
		return buildMetadata(
				availableResources( metadataBuildingContext, persistenceConfiguration ),
				metadataBuildingContext
		);
	}

	public static AvailableResources availableResources(
			MetadataBuildingContext metadataBuildingContext,
			Class<?>... domainClasses) {
		final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		for ( Class<?> domainClass : domainClasses ) {
			persistenceConfiguration.managedClass( domainClass );
		}
		return availableResources( metadataBuildingContext, persistenceConfiguration );
	}

	public static AvailableResources availableResources(
			MetadataBuildingContext metadataBuildingContext,
			HibernatePersistenceConfiguration persistenceConfiguration) {
		return AvailableResources.from(
				persistenceConfiguration,
				new AvailableResourcesContext(
						metadataBuildingContext.getBootstrapContext().getModelsContext(),
						metadataBuildingContext.getBootstrapContext().getServiceRegistry()
				)
		);
	}

	private TestBootModelProducer() {
	}
}
