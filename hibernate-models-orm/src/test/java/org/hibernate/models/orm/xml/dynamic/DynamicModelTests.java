/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.dynamic;

import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.FieldDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.categorize.spi.ManagedResourcesProcessor.processManagedResources;

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

			final FieldDetails labels = rootEntity.getClassDetails().findFieldByName( "labels" );
			assertThat( labels.getType().getClassName() ).isEqualTo( Set.class.getName() );
			final AnnotationUsage<ElementCollection> elementCollection = labels.getAnnotationUsage( ElementCollection.class );
			assertThat( elementCollection.<ClassDetails>getAttributeValue( "targetClass" ).getName() ).isEqualTo( String.class.getName() );
			final AnnotationUsage<CollectionClassification> collectionClassification = labels.getAnnotationUsage( CollectionClassification.class );
			assertThat( collectionClassification.<LimitedCollectionClassification>getAttributeValue( "value" ) ).isEqualTo( LimitedCollectionClassification.SET );
			final AnnotationUsage<CollectionTable> collectionTable = labels.getAnnotationUsage( CollectionTable.class );
			assertThat( collectionTable.<String>getAttributeValue( "name" ) ).isEqualTo( "labels" );
			assertThat( labels.getAnnotationUsage( SortNatural.class ) ).isNotNull();
			final AnnotationUsage<JoinColumn>[] joinColumns = collectionTable.getAttributeValue( "joinColumns" );
			assertThat( joinColumns ).hasSize( 1 );
			assertThat( joinColumns[0].<String>getAttributeValue( "name" ) ).isEqualTo( "contact_fk" );
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

	@Test
	void testOneToMany() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/dynamic/dynamic-plurals.xml" )
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
			final EntityTypeMetadata rootEntity = categorizedDomainModel.getEntityHierarchies().iterator().next().getRoot();
			assertThat( rootEntity.getClassDetails().getName() ).isEqualTo( Employee.class.getName() );

			final FieldDetails oneToMany = rootEntity.getClassDetails().findFieldByName( "oneToMany" );
			assertThat( oneToMany.getType().getClassName() ).isEqualTo( List.class.getName() );
			final AnnotationUsage<OneToMany> oneToManyAnn = oneToMany.getAnnotationUsage( OneToMany.class );
			assertThat( oneToManyAnn.<FetchType>getAttributeValue( "fetch" ) ).isEqualTo( FetchType.EAGER );
			assertThat( oneToMany.getAnnotationUsage( NotFound.class )
								.<NotFoundAction>getAttributeValue( "action" ) ).isEqualTo( NotFoundAction.IGNORE );
			assertThat( oneToMany.getAnnotationUsage( OnDelete.class )
								.<OnDeleteAction>getAttributeValue( "action" ) ).isEqualTo( OnDeleteAction.CASCADE );
			final AnnotationUsage<JoinColumn> joinColumn = oneToMany.getAnnotationUsage( JoinColumn.class );
			assertThat( joinColumn.<String>getAttributeValue( "name" ) ).isEqualTo( "employee_id" );
			assertThat( joinColumn.<Boolean>getAttributeValue( "insertable" ) ).isEqualTo( Boolean.FALSE );
			assertThat( joinColumn.<Boolean>getAttributeValue( "updatable" ) ).isEqualTo( Boolean.FALSE );
			final AnnotationUsage<ForeignKey> foreignKey = joinColumn.getAttributeValue( "foreignKey" );
			assertThat( foreignKey.<String>getAttributeValue( "name" ) ).isEqualTo( "employee_fk" );
			assertThat( foreignKey.<ConstraintMode>getAttributeValue( "value" ) ).isEqualTo( ConstraintMode.NO_CONSTRAINT );
			final AnnotationUsage<CheckConstraint>[] checkConstraints = joinColumn.getAttributeValue( "check" );
			assertThat( checkConstraints ).hasSize( 1 );
			assertThat( checkConstraints[0].<String>getAttributeValue( "name" ) ).isEqualTo( "employee_id_nn" );
			assertThat( checkConstraints[0].<String>getAttributeValue( "constraint" ) ).isEqualTo( "employee_id is not null" );
			assertThat( oneToMany.getAnnotationUsage( Cascade.class ).<CascadeType[]>getAttributeValue( "value" ) )
					.contains( CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.LOCK );
		}
	}
}
