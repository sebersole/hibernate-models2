/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.complete;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.xml.SimpleEntity;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;
import org.hibernate.jdbc.Expectation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleCompleteXmlTests {
	@Test
	void testSimpleCompleteEntity() {

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/complete/simple-complete.xml" );
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

			assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 1 );

			final EntityHierarchy hierarchy = categorizedDomainModel.getEntityHierarchies().iterator().next();
			final EntityTypeMetadata root = hierarchy.getRoot();
			assertThat( root.getClassDetails().getClassName() ).isEqualTo( SimpleEntity.class.getName() );
			assertThat( root.getNumberOfAttributes() ).isEqualTo( 2 );

			final AttributeMetadata idAttribute = root.findAttribute( "id" );
			assertThat( idAttribute.getNature() ).isEqualTo( AttributeNature.BASIC );
			assertThat( idAttribute.getMember().getDirectAnnotationUsage( Basic.class ) ).isNotNull();
			assertThat( idAttribute.getMember().getDirectAnnotationUsage( Id.class ) ).isNotNull();
			final Column idColumnAnn = idAttribute.getMember().getDirectAnnotationUsage( Column.class );
			assertThat( idColumnAnn ).isNotNull();
			assertThat( idColumnAnn.name() ).isEqualTo( "pk" );

			final AttributeMetadata nameAttribute = root.findAttribute( "name" );
			assertThat( nameAttribute.getNature() ).isEqualTo( AttributeNature.BASIC );
			assertThat( nameAttribute.getMember().getDirectAnnotationUsage( Basic.class ) ).isNotNull();
			final Column nameColumnAnn = nameAttribute.getMember().getDirectAnnotationUsage( Column.class );
			assertThat( nameColumnAnn ).isNotNull();
			assertThat( nameColumnAnn.name() ).isEqualTo( "description" );

			final SQLRestriction sqlRestriction = root.getClassDetails().getDirectAnnotationUsage( SQLRestriction.class );
			assertThat( sqlRestriction ).isNotNull();
			assertThat( sqlRestriction.value() ).isEqualTo( "name is not null" );

			validateSqlInsert( root.getClassDetails().getDirectAnnotationUsage( SQLInsert.class ));

			final Filter[] filters = root.getClassDetails().getRepeatedAnnotationUsages(
					Filter.class,
					metadataBuildingContext.getBootstrapContext().getModelsContext()
			);
			assertThat( filters ).hasSize( 1 );
			validateFilterUsage( filters[0] );
		}
	}

	private void validateFilterUsage(Filter filter) {
		assertThat( filter ).isNotNull();
		assertThat( filter.name() ).isEqualTo( "name_filter" );
		assertThat( filter.condition() ).isEqualTo( "{t}.name = :name" );
		assertThat( filter.aliases() ).hasSize( 1 );
		assertThat( filter.aliases()[0].alias() ).isEqualTo( "t" );
		assertThat( filter.aliases()[0].table() ).isEqualTo( "SimpleEntity" );
		assertThat( filter.aliases()[0].entity().getName() ).isEqualTo( SimpleEntity.class.getName() );
	}

	private void validateSqlInsert(SQLInsert sqlInsert) {
		assertThat( sqlInsert ).isNotNull();
		assertThat( sqlInsert.sql() ).isEqualTo( "insert into SimpleEntity(name) values(?)" );
		assertThat( sqlInsert.callable() ).isTrue();
		assertThat( sqlInsert.verify() ).isEqualTo( Expectation.RowCount.class );
		assertThat( sqlInsert.table() ).isEqualTo( "SimpleEntity" );
	}
}
