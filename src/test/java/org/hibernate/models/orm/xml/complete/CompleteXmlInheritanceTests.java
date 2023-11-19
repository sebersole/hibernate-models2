/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.complete;

import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.orm.BootstrapContextTesting;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.SourceModelTestHelper;

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.Id;

import static jakarta.persistence.InheritanceType.JOINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.SimpleClassLoading.SIMPLE_CLASS_LOADING;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

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

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final BootstrapContextTesting bootstrapContext = new BootstrapContextTesting(
					jandexIndex,
					serviceRegistry,
					new MetadataBuildingOptionsImpl( serviceRegistry )
			);
			final CategorizedDomainModel categorizedDomainModel = processManagedResources( managedResources, bootstrapContext );

			assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 1 );
			final EntityHierarchy hierarchy = categorizedDomainModel.getEntityHierarchies().iterator().next();
			assertThat( hierarchy.getInheritanceType() ).isEqualTo( JOINED );

			final EntityTypeMetadata rootMetadata = hierarchy.getRoot();
			assertThat( rootMetadata.getClassDetails().getClassName() ).isEqualTo( Root.class.getName() );
			final AttributeMetadata idAttr = rootMetadata.findAttribute( "id" );
			assertThat( idAttr.getMember().getAnnotationUsage( Id.class ) ).isNotNull();
		}
	}
}
