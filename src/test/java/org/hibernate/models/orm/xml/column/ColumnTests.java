/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.column;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.models.spi.FieldDetails;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class ColumnTests {
	@Test
	void testCompleteColumn(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
		final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		persistenceConfiguration.mappingFile( "mappings/column/complete.xml" );
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
		final EntityTypeMetadata root = hierarchy.getRoot();
		assertThat( root.getClassDetails().getClassName() ).isEqualTo( AnEntity.class.getName() );
		assertThat( root.getNumberOfAttributes() ).isEqualTo( 2 );
		final FieldDetails nameField = root.getClassDetails().findFieldByName( "name" );
		assertThat( nameField ).isNotNull();
		final Column annotationUsage = nameField.getDirectAnnotationUsage( Column.class );
		assertThat( annotationUsage.name() ).isEqualTo( "nombre" );
		assertThat( annotationUsage.length() ).isEqualTo( 256 );
		assertThat( annotationUsage.comment() ).isEqualTo( "The name column" );
		assertThat( annotationUsage.table() ).isEqualTo( "tbl" );
		assertThat( annotationUsage.options() ).isEqualTo( "the options" );
		assertThat( annotationUsage.check() ).isNotEmpty();
	}

	@Test
	void testOverrideColumn(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
		final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		persistenceConfiguration.mappingFile( "mappings/column/override.xml" );
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
		final EntityTypeMetadata root = hierarchy.getRoot();
		assertThat( root.getClassDetails().getClassName() ).isEqualTo( AnEntity.class.getName() );

		assertThat( root.getNumberOfAttributes() ).isEqualTo( 2 );
		final FieldDetails nameField = root.getClassDetails().findFieldByName( "name" );
		assertThat( nameField ).isNotNull();
		final Column columnAnn = nameField.getDirectAnnotationUsage( Column.class );
		assertThat( columnAnn ).isNotNull();
		assertThat( columnAnn.name() ).isEqualTo( "nombre" );
	}
}
