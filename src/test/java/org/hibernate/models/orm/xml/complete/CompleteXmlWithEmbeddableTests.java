/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.complete;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;

import org.junit.jupiter.api.Test;

import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.AttributeNature.BASIC;
import static org.hibernate.boot.models.AttributeNature.EMBEDDED;
import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

/**
 * @author Steve Ebersole
 */
public class CompleteXmlWithEmbeddableTests {
	@Test
	void testIt() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/complete/simple-person.xml" );
			final AvailableResources availableResources = AvailableResources.from(
					persistenceConfiguration,
					metadataBuildingContext
			);
			final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
					availableResources,
					metadataBuildingContext
			);

			assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 1 );

			final EntityHierarchy hierarchy = categorizedDomainModel.getEntityHierarchies().iterator().next();
			final EntityTypeMetadata personMetadata = hierarchy.getRoot();
			assertThat( personMetadata.getAccessType() ).isEqualTo( AccessType.FIELD );

			assertThat( personMetadata.getAttributes() ).hasSize( 2 );

			final AttributeMetadata idAttribute = personMetadata.findAttribute( "id" );
			assertThat( idAttribute.getNature() ).isEqualTo( BASIC );
			assertThat( idAttribute.getMember().getDirectAnnotationUsage( Basic.class ) ).isNotNull();
			assertThat( idAttribute.getMember().getDirectAnnotationUsage( Id.class ) ).isNotNull();

			final AttributeMetadata nameAttribute = personMetadata.findAttribute( "name" );
			assertThat( nameAttribute.getNature() ).isEqualTo( EMBEDDED );
			assertThat( nameAttribute.getMember().getDirectAnnotationUsage( Embedded.class ) ).isNotNull();
		}
	}
}
