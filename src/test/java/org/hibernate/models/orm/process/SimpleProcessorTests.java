/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.process;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.models.spi.ClassDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleProcessorTests {
	@Test
	void testSimpleUsage() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.managedClass( Person.class );
			persistenceConfiguration.managedClass( Root.class );
			persistenceConfiguration.managedClass( Sub.class );
			persistenceConfiguration.managedClass( MyStringConverter.class );
			persistenceConfiguration.managedClass( MyUuidConverter.class );
			final AvailableResources availableResources = AvailableResources.from(
					persistenceConfiguration,
					new AvailableResourcesContext(
							metadataBuildingContext.getBootstrapContext().getModelsContext(),
							metadataBuildingContext.getBootstrapContext().getServiceRegistry()
					)
			);
			final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
					availableResources,
					metadataBuildingContext
			);

			assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 2 );
			final Iterator<EntityHierarchy> hierarchies = categorizedDomainModel.getEntityHierarchies().iterator();
			final EntityHierarchy one = hierarchies.next();
			final EntityHierarchy two = hierarchies.next();

			assertThat( one.getRoot() ).isNotNull();
			assertThat( one.getRoot().getClassDetails() ).isNotNull();
			assertThat( one.getRoot().getClassDetails().getClassName() ).isNotNull();
			if ( one.getRoot().getClassDetails().getClassName().endsWith( "Person" ) ) {
				validatePersonHierarchy( one );
				validateJoinedHierarchy( two );
			}
			else {
				validatePersonHierarchy( two );
				validateJoinedHierarchy( one );
			}

			validateFilterDefs( categorizedDomainModel.getGlobalRegistrations().getFilterDefRegistrations() );
		}
	}

	private void validatePersonHierarchy(EntityHierarchy hierarchy) {
		assertThat( hierarchy.getInheritanceType() ).isEqualTo( InheritanceType.SINGLE_TABLE );
		final EntityTypeMetadata personMetadata = hierarchy.getRoot();
		assertThat( personMetadata.getClassDetails().getClassName() ).isEqualTo( Person.class.getName() );
		assertThat( personMetadata.getJpaEntityName() ).isEqualTo( "Person" );
		assertThat( personMetadata.getEntityName() ).isEqualTo( Person.class.getName() );

		assertThat( personMetadata.getSuperType() ).isNull();
		assertThat( personMetadata.hasSubTypes() ).isFalse();
		assertThat( personMetadata.getNumberOfSubTypes() ).isEqualTo( 0 );
	}

	private void validateJoinedHierarchy(EntityHierarchy hierarchy) {
		assertThat( hierarchy.getInheritanceType() ).isEqualTo( InheritanceType.JOINED );
		final EntityTypeMetadata rootMetadata = hierarchy.getRoot();
		assertThat( rootMetadata.getClassDetails().getClassName() ).isEqualTo( Root.class.getName() );
		assertThat( rootMetadata.getJpaEntityName() ).isEqualTo( "Root" );
		assertThat( rootMetadata.getEntityName() ).isEqualTo( Root.class.getName() );

		assertThat( rootMetadata.getSuperType() ).isNull();
		assertThat( rootMetadata.hasSubTypes() ).isTrue();
		assertThat( rootMetadata.getNumberOfSubTypes() ).isEqualTo( 1 );

		final EntityTypeMetadata subMetadata = (EntityTypeMetadata) rootMetadata.getSubTypes().iterator().next();
		assertThat( subMetadata ).isNotNull();
		assertThat( subMetadata.getClassDetails().getClassName() ).isEqualTo( Sub.class.getName() );
		assertThat( subMetadata.getJpaEntityName() ).isEqualTo( "Sub" );
		assertThat( subMetadata.getEntityName() ).isEqualTo( Sub.class.getName() );
		assertThat( subMetadata.getSuperType() ).isEqualTo( rootMetadata );
		assertThat( subMetadata.hasSubTypes() ).isFalse();
		assertThat( subMetadata.getNumberOfSubTypes() ).isEqualTo( 0 );
	}

	private void validateFilterDefs(Map<String, FilterDefRegistration> filterDefRegistrations) {
		assertThat( filterDefRegistrations ).hasSize( 1 );
		assertThat( filterDefRegistrations ).containsKey( "name_filter" );
		final FilterDefRegistration nameFilter = filterDefRegistrations.get( "name_filter" );
		assertThat( nameFilter.defaultCondition() ).isEqualTo( "name = :name" );
		final Map<String, ClassDetails> parameters = nameFilter.parameters();
		assertThat( parameters ).hasSize( 1 );
		assertThat( parameters ).containsKey( "name" );
		assertThat( parameters.get( "name" ).getName() ).isEqualTo( String.class.getName() );
	}
}
