/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.complete;

import java.util.List;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.models.orm.spi.AttributeMetadata;
import org.hibernate.models.orm.spi.CategorizedDomainModel;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.orm.spi.EntityTypeMetadata;
import org.hibernate.models.orm.xml.SimpleEntity;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Steve Ebersole
 */
public class SimpleCompleteXmlTests {
	@Test
	void testSimpleCompleteEntity() {

		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/complete/simple-complete.xml" );
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
			assertThat( idAttribute.getMember().getAnnotationUsage( Basic.class ) ).isNotNull();
			assertThat( idAttribute.getMember().getAnnotationUsage( Id.class ) ).isNotNull();
			final AnnotationUsage<Column> idColumnAnn = idAttribute.getMember().getAnnotationUsage( Column.class );
			assertThat( idColumnAnn ).isNotNull();
			assertThat( idColumnAnn.<String>getAttributeValue( "name" ) ).isEqualTo( "pk" );

			final AttributeMetadata nameAttribute = root.findAttribute( "name" );
			assertThat( nameAttribute.getNature() ).isEqualTo( AttributeMetadata.AttributeNature.BASIC );
			assertThat( nameAttribute.getMember().getAnnotationUsage( Basic.class ) ).isNotNull();
			final AnnotationUsage<Column> nameColumnAnn = nameAttribute.getMember().getAnnotationUsage( Column.class );
			assertThat( nameColumnAnn ).isNotNull();
			assertThat( nameColumnAnn.<String>getAttributeValue( "name" ) ).isEqualTo( "description" );

			validateFilterUsage( root.getClassDetails().getAnnotationUsage( Filter.class ) );
		}
	}

	private void validateFilterUsage(AnnotationUsage<Filter> filter) {
		assertThat( filter ).isNotNull();
		assertThat( filter.<String>getAttributeValue( "name" ) ).isEqualTo( "name_filter" );
		assertThat( filter.<String>getAttributeValue( "condition" ) ).isEqualTo( "{t}.name = :name" );
		final List<AnnotationUsage<SqlFragmentAlias>> aliases = filter.getAttributeValue( "aliases" );
		assertThat( aliases ).hasSize( 1 );
		assertThat( aliases.get( 0 ).<String>getAttributeValue( "alias" ) ).isEqualTo( "t" );
		assertThat( aliases.get( 0 ).<String>getAttributeValue( "table" ) ).isEqualTo( "SimpleEntity" );
		assertThat( aliases.get( 0 )
							.<ClassDetails>getAttributeValue( "entity" )
							.getName() ).isEqualTo( SimpleEntity.class.getName() );
	}
}
