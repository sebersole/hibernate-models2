/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.attr;

import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.FieldDetails;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.categorize.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class AnyTests {
	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testSimpleAnyAttribute(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addLoadedClasses( Entity1.class )
				.addLoadedClasses( Entity2.class )
				.addXmlMappings( "mappings/attr/any/simple.xml" )
				.build();

		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
				serviceRegistry,
				new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
		);
		final CategorizedDomainModel categorizedDomainModel = processManagedResources(
				managedResources,
				bootstrapContext
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
		assertThat( associationField.getAnnotationUsage( Any.class ) ).isNotNull();

		final AnnotationUsage<AnyDiscriminator> discrimAnn = associationField.getAnnotationUsage( AnyDiscriminator.class );
		assertThat( discrimAnn ).isNotNull();
		assertThat( discrimAnn.<DiscriminatorType>getEnum( "value" ) ).isEqualTo( DiscriminatorType.INTEGER );

		final List<AnnotationUsage<AnyDiscriminatorValue>> discriminatorMappings = associationField.getRepeatedAnnotationUsages( AnyDiscriminatorValue.class );
		assertThat( discriminatorMappings ).hasSize( 2 );

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
