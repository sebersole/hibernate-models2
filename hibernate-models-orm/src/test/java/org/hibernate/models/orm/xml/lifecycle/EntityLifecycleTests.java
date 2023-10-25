/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.lifecycle;

import java.util.List;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.models.orm.spi.CategorizedDomainModel;
import org.hibernate.models.orm.spi.EntityTypeMetadata;
import org.hibernate.models.orm.xml.SimpleEntity;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Marco Belladelli
 */
public class EntityLifecycleTests {
	@Test
	void testEntityLifecycle() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/lifecycle/entity-lifecycle.xml" )
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

			assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 1 );
			final EntityTypeMetadata rootEntity = categorizedDomainModel.getEntityHierarchies()
					.iterator()
					.next()
					.getRoot();
			final ClassDetails classDetails = rootEntity.getClassDetails();
			assertThat( classDetails.getName() ).isEqualTo( SimpleEntity.class.getName() );

			// lifecycle callback methods
			assertThat( classDetails.findMethodByName( "prePersist" ).getAnnotationUsage( PrePersist.class ) ).isNotNull();
			assertThat( classDetails.findMethodByName( "preRemove" ).getAnnotationUsage( PreRemove.class ) ).isNotNull();
			assertThat( classDetails.findMethodByName( "preUpdate" ).getAnnotationUsage( PreUpdate.class ) ).isNotNull();

			// entity listeners
			final AnnotationUsage<EntityListeners> entityListenersAnn = classDetails.getAnnotationUsage( EntityListeners.class );
			assertThat( entityListenersAnn ).isNotNull();
			final List<ClassDetails> entityListeners = entityListenersAnn.getAttributeValue( "value" );
			assertThat( entityListeners ).hasSize( 1 );
			final ClassDetails listener = entityListeners.get( 0 );
			assertThat( listener.getName() ).isEqualTo( SimpleEntityListener.class.getName() );
			assertThat( listener.findMethodByName( "postPersist" ).getAnnotationUsage( PostPersist.class ) ).isNotNull();
			assertThat( listener.findMethodByName( "postRemove" ).getAnnotationUsage( PostRemove.class ) ).isNotNull();
			assertThat( listener.findMethodByName( "postUpdate" ).getAnnotationUsage( PostUpdate.class ) ).isNotNull();
			assertThat( listener.findMethodByName( "postLoad" ).getAnnotationUsage( PostLoad.class ) ).isNotNull();
		}
	}
}
