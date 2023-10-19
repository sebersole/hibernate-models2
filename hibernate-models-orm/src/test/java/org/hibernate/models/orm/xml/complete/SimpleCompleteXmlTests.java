/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.complete;

import org.hibernate.models.orm.internal.ManagedResourcesImpl;
import org.hibernate.models.orm.spi.AttributeMetadata;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.orm.spi.EntityTypeMetadata;
import org.hibernate.models.orm.spi.ManagedResources;
import org.hibernate.models.orm.spi.ProcessResult;
import org.hibernate.models.orm.spi.Processor;
import org.hibernate.models.orm.xml.SimpleEntity;
import org.hibernate.models.source.SourceModelTestHelper;
import org.hibernate.models.source.internal.SourceModelBuildingContextImpl;
import org.hibernate.models.source.spi.AnnotationUsage;

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.Column;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * @author Steve Ebersole
 */
public class SimpleCompleteXmlTests {
	@Test
	void testSimpleCompleteEntity() {

		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/complete/simple-complete.xml" );
		final ManagedResources managedResources = managedResourcesBuilder.build();

		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				SimpleEntity.class
		);
		final SourceModelBuildingContextImpl buildingContext = SourceModelTestHelper.createBuildingContext(
				jandexIndex,
				SIMPLE_CLASS_LOADING
		);

		final ProcessResult processResult = Processor.process(
				managedResources,
				null,
				new Processor.Options() {
					@Override
					public boolean areGeneratorsGlobal() {
						return false;
					}

					@Override
					public boolean shouldIgnoreUnlistedClasses() {
						return false;
					}
				},
				buildingContext
		);

		assertThat( processResult.getEntityHierarchies() ).hasSize( 1 );

		final EntityHierarchy hierarchy = processResult.getEntityHierarchies().iterator().next();
		final EntityTypeMetadata root = hierarchy.getRoot();
		assertThat( root.getClassDetails().getClassName() ).isEqualTo( SimpleEntity.class.getName() );
		assertThat( root.getNumberOfAttributes() ).isEqualTo( 2 );

		final AttributeMetadata idAttribute = root.findAttribute( "id" );
		assertThat( idAttribute.getNature() ).isEqualTo( AttributeMetadata.AttributeNature.BASIC );
		final AnnotationUsage<Column> idColumnAnn = idAttribute.getMember().getAnnotationUsage( Column.class );
		assertThat( idColumnAnn ).isNotNull();
		assertThat( idColumnAnn.<String>getAttributeValue( "name" ) ).isEqualTo( "pk" );

		final AttributeMetadata nameAttribute = root.findAttribute( "name" );
		assertThat( nameAttribute.getNature() ).isEqualTo( AttributeMetadata.AttributeNature.BASIC );
		final AnnotationUsage<Column> nameColumnAnn = nameAttribute.getMember().getAnnotationUsage( Column.class );
		assertThat( nameColumnAnn ).isNotNull();
		assertThat( nameColumnAnn.<String>getAttributeValue( "name" ) ).isEqualTo( "description" );
	}
}
