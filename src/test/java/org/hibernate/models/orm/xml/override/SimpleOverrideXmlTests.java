/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.override;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.xml.SimpleEntity;
import org.hibernate.models.spi.AnnotationUsage;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Steve Ebersole
 */
public class SimpleOverrideXmlTests {
	@Test
	void testSimpleCompleteEntity() {

		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/override/simple-override.xml" );
		final ManagedResources managedResources = managedResourcesBuilder.build();

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

			final EntityHierarchy hierarchy = categorizedDomainModel.getEntityHierarchies().iterator().next();
			final EntityTypeMetadata root = hierarchy.getRoot();
			assertThat( root.getClassDetails().getClassName() ).isEqualTo( SimpleEntity.class.getName() );
			assertThat( root.getNumberOfAttributes() ).isEqualTo( 2 );

			final AttributeMetadata idAttribute = root.findAttribute( "id" );
			assertThat( idAttribute.getNature() ).isEqualTo( AttributeMetadata.AttributeNature.BASIC );
			assertThat( idAttribute.getMember().getAnnotationUsage( Id.class ) ).isNotNull();

			final AttributeMetadata nameAttribute = root.findAttribute( "name" );
			assertThat( nameAttribute.getNature() ).isEqualTo( AttributeMetadata.AttributeNature.BASIC );
			assertThat( nameAttribute.getMember().getAnnotationUsage( Basic.class ) ).isNotNull();
			final AnnotationUsage<Column> nameColumnAnn = nameAttribute.getMember().getAnnotationUsage( Column.class );
			assertThat( nameColumnAnn ).isNotNull();
			assertThat( nameColumnAnn.<String>getAttributeValue( "name" ) ).isEqualTo( "description" );
			assertThat( nameColumnAnn.<String>getAttributeValue( "columnDefinition" ) ).isEqualTo( "nvarchar(512)" );
		}
	}
}
