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
import org.hibernate.models.source.SourceModelTestHelper;
import org.hibernate.models.source.internal.SourceModelBuildingContextImpl;

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.Id;

import static jakarta.persistence.InheritanceType.JOINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * @author Steve Ebersole
 */
public class CompleteXmlInheritanceTests {
	@Test
	void testIt() {

		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder.addXmlMappings( "mappings/complete/simple-inherited.xml" );
		final ManagedResources managedResources = managedResourcesBuilder.build();

		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				Root.class,
				Sub.class
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
		assertThat( hierarchy.getInheritanceType() ).isEqualTo( JOINED );

		final EntityTypeMetadata rootMetadata = hierarchy.getRoot();
		assertThat( rootMetadata.getClassDetails().getClassName() ).isEqualTo( Root.class.getName() );
		final AttributeMetadata idAttr = rootMetadata.findAttribute( "id" );
		assertThat( idAttr.getMember().getAnnotationUsage( Id.class ) ).isNotNull();
	}
}
