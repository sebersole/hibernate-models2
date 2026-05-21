/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.globals;

import java.util.List;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;

import org.junit.jupiter.api.Test;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class JpaEventListenerTests {
	@Test
	void testGlobalRegistration() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/globals.xml" );
			final AvailableResources availableResources = AvailableResources.from(
					persistenceConfiguration,
					metadataBuildingContext
			);
			final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
					availableResources,
					metadataBuildingContext
			);
			final List<JpaEventListener> registrations = categorizedDomainModel
					.getGlobalRegistrations()
					.getEntityListenerRegistrations();
			assertThat( registrations ).hasSize( 1 );
			final JpaEventListener registration = registrations.get( 0 );
			final MethodDetails postPersistMethod = registration.getPostPersistMethod();
			assertThat( postPersistMethod ).isNotNull();
			assertThat( postPersistMethod.getReturnType() ).isEqualTo( ClassDetails.VOID_CLASS_DETAILS );
			assertThat( postPersistMethod.getArgumentTypes() ).hasSize( 1 );
		}
	}

}
