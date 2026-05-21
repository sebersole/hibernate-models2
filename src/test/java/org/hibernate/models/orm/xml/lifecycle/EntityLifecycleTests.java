/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.lifecycle;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.models.orm.xml.SimpleEntity;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
public class EntityLifecycleTests {
	@Test
	void testEntityLifecycle() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/lifecycle/entity-lifecycle.xml" );
			final AvailableResources availableResources = AvailableResources.from(
					persistenceConfiguration,
					metadataBuildingContext
			);
			final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
					availableResources,
					metadataBuildingContext
			);

			assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 1 );
			final EntityTypeMetadata rootEntity = categorizedDomainModel.getEntityHierarchies()
					.iterator()
					.next()
					.getRoot();
			final ClassDetails classDetails = rootEntity.getClassDetails();
			assertThat( classDetails.getName() ).isEqualTo( SimpleEntity.class.getName() );

			// lifecycle callback methods
			getMethodDetails( classDetails, "prePersist" ).forEach( method -> {
				final PrePersist prePersist = method.getDirectAnnotationUsage( PrePersist.class );
				if ( !method.getArgumentTypes().isEmpty() ) {
					assertThat( prePersist ).isNull();
				}
				else {
					assertThat( prePersist ).isNotNull();
				}
			} );
			assertThat( getMethodDetails( classDetails, "preRemove" ).get( 0 ).getDirectAnnotationUsage( PreRemove.class ) ).isNotNull();
			assertThat( getMethodDetails( classDetails, "preUpdate" ).get( 0 ).getDirectAnnotationUsage( PreUpdate.class ) ).isNotNull();

			// entity listeners
			final EntityListeners entityListenersAnn = classDetails.getDirectAnnotationUsage( EntityListeners.class );
			assertThat( entityListenersAnn ).isNotNull();
			final Class<?>[] entityListeners = entityListenersAnn.value();
			assertThat( entityListeners ).hasSize( 1 );
			final ClassDetails listener = metadataBuildingContext.getBootstrapContext().getModelsContext()
					.getClassDetailsRegistry()
					.resolveClassDetails( entityListeners[0].getName() );
			assertThat( listener.getName() ).isEqualTo( SimpleEntityListener.class.getName() );
			getMethodDetails( classDetails, "postPersist" ).forEach( method -> {
				final PostPersist prePersist = method.getDirectAnnotationUsage( PostPersist.class );
				if ( method.getArgumentTypes().size() != 1 ) {
					assertThat( prePersist ).isNull();
				}
				else {
					assertThat( prePersist ).isNotNull();
				}
			} );
			assertThat( getMethodDetails( listener, "postRemove" ).get( 0 ).getDirectAnnotationUsage( PostRemove.class ) ).isNotNull();
			assertThat( getMethodDetails( listener, "postUpdate" ).get( 0 ).getDirectAnnotationUsage( PostUpdate.class ) ).isNotNull();
			assertThat( getMethodDetails( listener, "postLoad" ).get( 0 ).getDirectAnnotationUsage( PostLoad.class ) ).isNotNull();
		}
	}

	private List<MethodDetails> getMethodDetails(ClassDetails classDetails, String name) {
		return classDetails.getMethods()
				.stream()
				.filter( m -> m.getName().equals( name ) )
				.collect( Collectors.toList() );
	}
}
