/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.dynamic;


import java.util.Set;


import org.hibernate.annotations.TenantId;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.models.spi.FieldDetails;


import org.junit.jupiter.api.Test;


import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;


import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

import static org.assertj.core.api.Assertions.assertThat;


public class TenantIdTest {
	@Test
	void testSimpleDynamicModel() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/dynamic/dynamic-tenantid.xml" );
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
			assertThat( entityHierarchies ).hasSize( 2 );

			entityHierarchies.forEach(
					entityHierarchy -> {
						final EntityTypeMetadata root = entityHierarchy.getRoot();
						final String entityName = root.getEntityName();

						final FieldDetails field = root.getClassDetails().findFieldByName( "tenantId" );

						if ( entityName.equals( "EntityWithoutTenantId" ) ) {
							assertThat( field ).isNull();
						}
						else {
							final TenantId tenantIdAnnotationUsage = field.getDirectAnnotationUsage( TenantId.class );

							assertThat( tenantIdAnnotationUsage ).isNotNull();

							final Basic basicAnnotationUsage = field.getDirectAnnotationUsage( Basic.class );
							assertThat( basicAnnotationUsage ).isNotNull();
							assertThat( basicAnnotationUsage.fetch() ).isEqualTo( FetchType.EAGER );
							assertThat( basicAnnotationUsage.optional() ).isTrue();

							final Column columnAnnotationUsage = field.getDirectAnnotationUsage( Column.class );
							assertThat( basicAnnotationUsage ).isNotNull();
							assertThat( columnAnnotationUsage.name() ).isEqualTo( "TENANT_ID" );
							assertThat( columnAnnotationUsage.insertable() ).isFalse();
						}
					}
			);
		}
	}
}
