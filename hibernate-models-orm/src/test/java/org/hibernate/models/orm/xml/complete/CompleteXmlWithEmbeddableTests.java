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

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.AccessType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;
import static org.hibernate.models.orm.spi.AttributeMetadata.AttributeNature.BASIC;
import static org.hibernate.models.orm.spi.AttributeMetadata.AttributeNature.EMBEDDED;

/**
 * @author Steve Ebersole
 */
public class CompleteXmlWithEmbeddableTests {
	@Test
	void testIt() {
		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/complete/simple-person.xml" );
		final ManagedResources managedResources = managedResourcesBuilder.build();

		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				SimplePerson.class,
				Name.class
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
		final EntityTypeMetadata personMetadata = hierarchy.getRoot();
		assertThat( personMetadata.getAccessType() ).isEqualTo( AccessType.FIELD );

		assertThat( personMetadata.getAttributes() ).hasSize( 2 );

		final AttributeMetadata idAttribute = personMetadata.findAttribute( "id" );
		assertThat( idAttribute.getNature() ).isEqualTo( BASIC );

		final AttributeMetadata nameAttribute = personMetadata.findAttribute( "name" );
		assertThat( nameAttribute.getNature() ).isEqualTo( EMBEDDED );
	}
}
