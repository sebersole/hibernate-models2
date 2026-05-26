/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.dynamic;

import java.util.Set;

import org.hibernate.annotations.RowId;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;

import org.junit.jupiter.api.Test;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

import static org.assertj.core.api.Assertions.assertThat;

public class RowIdTest {
	@Test
	void testSimpleDynamicModel() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/dynamic/dynamic-rowid.xml" );
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

			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( entityHierarchies ).hasSize( 3 );

			entityHierarchies.forEach(
					entityHierarchy -> {
						final EntityTypeMetadata root = entityHierarchy.getRoot();
						final RowId rowIdAnnotationUsage = root.getClassDetails().getDirectAnnotationUsage(
								RowId.class );
						final String entityName = root.getEntityName();
						if ( entityName.equals( "EntityWithoutRowId" ) ) {
							assertThat( rowIdAnnotationUsage ).isNull();
						}
						else {
							assertThat( rowIdAnnotationUsage ).isNotNull();
							final String value = rowIdAnnotationUsage.value();
							if ( entityName.equals( "EntityWithRowIdNoValue" ) ) {
								assertThat( value ).isEmpty();
							}
							else {
								assertThat( value ).isEqualTo( "ROW_ID" );
							}
						}
					}
			);
		}
	}
}
