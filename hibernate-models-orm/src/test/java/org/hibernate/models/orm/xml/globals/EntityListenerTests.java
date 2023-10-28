/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.globals;

import java.util.List;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.EntityListenerRegistration;
import org.hibernate.models.source.spi.MethodDetails;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.categorize.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Steve Ebersole
 */
public class EntityListenerTests {
	@Test
	void testGlobalRegistration() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/globals.xml" )
				.build();

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
					serviceRegistry,
					new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
			);
			final CategorizedDomainModel categorizedDomainModel = processManagedResources(
					managedResources,
					bootstrapContext
			);
			final List<EntityListenerRegistration> registrations = categorizedDomainModel
					.getGlobalRegistrations()
					.getEntityListenerRegistrations();
			assertThat( registrations ).hasSize( 1 );
			final EntityListenerRegistration registration = registrations.get( 0 );
			final MethodDetails postPersistMethod = registration.getPostPersistMethod();
			assertThat( postPersistMethod ).isNotNull();
			assertThat( postPersistMethod.getReturnType() ).isNull();
			assertThat( postPersistMethod.getArgumentTypes() ).hasSize( 1 );
		}
	}

}
