/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.attr;

import java.util.Arrays;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.models.spi.FieldDetails;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class AnyTests {
	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testSimpleAnyAttribute(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );

		final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		persistenceConfiguration.managedClass( Entity1.class );
		persistenceConfiguration.managedClass( Entity2.class );
		persistenceConfiguration.mappingFile( "mappings/attr/any/simple.xml" );
		final AvailableResources availableResources = AvailableResources.from(
				persistenceConfiguration,
				metadataBuildingContext
		);
		final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
				availableResources,
				metadataBuildingContext
		);

		assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 3 );
		final EntityHierarchy entity3Hierarchy = categorizedDomainModel.getEntityHierarchies()
				.stream()
				.filter( hierarchy -> hierarchy.getRoot().getEntityName().endsWith( "Entity3" ) )
				.findFirst()
				.orElse( null );
		assertThat( entity3Hierarchy ).isNotNull();

		final FieldDetails associationField = entity3Hierarchy.getRoot().getClassDetails().findFieldByName( "association" );
		assertThat( associationField ).isNotNull();
		assertThat( associationField.getDirectAnnotationUsage( Any.class ) ).isNotNull();

		final AnyDiscriminator discrimAnn = associationField.getDirectAnnotationUsage( AnyDiscriminator.class );
		assertThat( discrimAnn ).isNotNull();
		assertThat( discrimAnn.value() ).isEqualTo( DiscriminatorType.INTEGER );

		final AnyDiscriminatorValue[] discriminatorMappings = associationField.getRepeatedAnnotationUsages(
				AnyDiscriminatorValue.class,
				metadataBuildingContext.getBootstrapContext().getModelsContext()
		);
		assertThat( discriminatorMappings ).hasSize( 2 );

		final String[] mappedEntityNames = Arrays.stream( discriminatorMappings )
				.map( (valueAnn) -> valueAnn.entity().getName() )
				.toArray( String[]::new );
		assertThat( mappedEntityNames ).containsExactly( Entity1.class.getName(), Entity2.class.getName() );

		final AnyKeyType keyTypeAnn = associationField.getDirectAnnotationUsage( AnyKeyType.class );
		assertThat( keyTypeAnn ).isNotNull();
		assertThat( keyTypeAnn.value() ).isEqualTo( "integer" );

		final JoinColumn[] keyColumns = associationField.getRepeatedAnnotationUsages(
				JoinColumn.class,
				metadataBuildingContext.getBootstrapContext().getModelsContext()
		);
		assertThat( keyColumns ).hasSize( 1 );
		assertThat( keyColumns[0].name() ).isEqualTo( "association_fk" );
	}

	@Entity(name="Entity1")
	@Table(name="Entity1")
	public static class Entity1 {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Entity2")
	@Table(name="Entity2")
	public static class Entity2 {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Entity3")
	@Table(name="Entity3")
	public static class Entity3 {
		@Id
		private Integer id;
		private String name;

		@Any
		@AnyDiscriminator(DiscriminatorType.INTEGER)
		@AnyDiscriminatorValue( discriminator = "1", entity = Entity1.class )
		@AnyDiscriminatorValue( discriminator = "2", entity = Entity2.class )
		private Object association;

	}
}
