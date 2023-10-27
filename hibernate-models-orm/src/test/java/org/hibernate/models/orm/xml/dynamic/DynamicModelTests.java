/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.dynamic;

import java.util.Set;

import org.hibernate.annotations.JavaType;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.models.orm.spi.CategorizedDomainModel;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.orm.spi.EntityTypeMetadata;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.FieldDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.IdClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.spi.ManagedResourcesProcessor.processManagedResources;

/**
 * @author Steve Ebersole
 */
public class DynamicModelTests {
	@Test
	void testSimpleDynamicModel() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/dynamic/dynamic-simple.xml" )
				.build();
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
			final EntityTypeMetadata rootEntity = hierarchy.getRoot();
			assertThat( rootEntity.getClassDetails().getClassName() ).isNull();
			assertThat( rootEntity.getClassDetails().getName() ).isEqualTo( "SimpleEntity" );

			final FieldDetails idField = rootEntity.getClassDetails().findFieldByName( "id" );
			assertThat( idField.getType().getClassName() ).isEqualTo( Integer.class.getName() );

			final FieldDetails nameField = rootEntity.getClassDetails().findFieldByName( "name" );
			assertThat( nameField.getType().getClassName() ).isEqualTo( Object.class.getName() );
			assertThat( nameField.getAnnotationUsage( JavaType.class ) ).isNotNull();

			final FieldDetails qtyField = rootEntity.getClassDetails().findFieldByName( "quantity" );
			assertThat( qtyField.getType().getClassName() ).isEqualTo( int.class.getName() );
		}
	}

	@Test
	void testSemiSimpleDynamicModel() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/dynamic/dynamic-semi-simple.xml" )
				.build();
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
			final EntityTypeMetadata rootEntity = hierarchy.getRoot();
			assertThat( rootEntity.getClassDetails().getClassName() ).isNull();
			assertThat( rootEntity.getClassDetails().getName() ).isEqualTo( "Contact" );

			final FieldDetails idField = rootEntity.getClassDetails().findFieldByName( "id" );
			assertThat( idField.getType().getClassName() ).isEqualTo( Integer.class.getName() );

			final FieldDetails nameField = rootEntity.getClassDetails().findFieldByName( "name" );
			assertThat( nameField.getType().getClassName() ).isNull();
			assertThat( nameField.getType().getName() ).isEqualTo( "Name" );
			assertThat( nameField.getAnnotationUsage( Target.class ) ).isNotNull();
			assertThat( nameField.getAnnotationUsage( Target.class ).getString( "value" ) ).isEqualTo( "Name" );

			assertThat( nameField.getType().getFields() ).hasSize( 2 );

			final FieldDetails labelsField = rootEntity.getClassDetails().findFieldByName( "labels" );
			assertThat( labelsField.getType().getClassName() ).isEqualTo( Set.class.getName() );
		}
	}

	@Test
	void testIdClass() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/dynamic/dynamic-id-class.xml" )
				.build();
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
			final EntityTypeMetadata rootEntity = hierarchy.getRoot();
			assertThat( rootEntity.getClassDetails().getName() ).isEqualTo( Employee.class.getName() );

			final AnnotationUsage<IdClass> idClass = rootEntity.getClassDetails().getAnnotationUsage( IdClass.class );
			assertThat( idClass ).isNotNull();
			assertThat( idClass.<ClassDetails>getAttributeValue( "value" )
								.getName() ).isEqualTo( EmployeePK.class.getName() );
		}
	}
}
