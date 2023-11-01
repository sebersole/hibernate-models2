/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.dynamic;

import java.util.Set;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.FieldDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.categorize.spi.ManagedResourcesProcessor.processManagedResources;

public class TenantIdTest {
	@Test
	void testSimpleDynamicModel() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/dynamic/dynamic-tenantid.xml" )
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

			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( entityHierarchies ).hasSize( 2 );

			entityHierarchies.forEach(
					entityHierarchy -> {
						final EntityTypeMetadata root = entityHierarchy.getRoot();
						final AnnotationUsage<TenantId> tenantIdAnnotationUsage = root.getClassDetails()
								.getAnnotationUsage( TenantId.class );
						final String entityName = root.getEntityName();
						if ( entityName.equals( "EntityWithoutTenantId" ) ) {
							assertThat( tenantIdAnnotationUsage ).isNull();
						}
						else {
							assertThat( tenantIdAnnotationUsage ).isNotNull();
							assertThat( tenantIdAnnotationUsage.getString( "name" ) ).isEqualTo( "tenantId" );

							final FieldDetails fieldDetails = root.getClassDetails().findFieldByName( "tenantId" );

							final AnnotationUsage<Basic> basicAnnotationUsage = fieldDetails.getAnnotationUsage( Basic.class );
							assertThat( basicAnnotationUsage ).isNotNull();
							assertThat( basicAnnotationUsage.<FetchType>getAttributeValue( "fetch" ) )
									.isEqualTo( FetchType.EAGER );
							assertThat( basicAnnotationUsage.getBoolean( "optional" ) ).isTrue();

							final AnnotationUsage<Column> columnAnnotationUsage = fieldDetails.
									getAnnotationUsage( Column.class );
							assertThat( basicAnnotationUsage ).isNotNull();
							assertThat( columnAnnotationUsage.getString( "name" ) ).isEqualTo( "TENANT_ID" );
							assertThat( columnAnnotationUsage.getBoolean( "insertable" ) ).isFalse();

						}
					}
			);
		}
	}
}
