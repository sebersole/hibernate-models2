/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.dynamic;

import java.util.List;
import java.util.SortedSet;

import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.spi.FieldDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class DynamicModelTests {
	@Test
	void testSimpleDynamicModel() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/dynamic/dynamic-simple.xml" );
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
			final EntityTypeMetadata rootEntity = hierarchy.getRoot();
			assertThat( rootEntity.getClassDetails().getClassName() ).isNull();
			assertThat( rootEntity.getClassDetails().getName() ).isEqualTo( "SimpleEntity" );

			final FieldDetails idField = rootEntity.getClassDetails().findFieldByName( "id" );
			assertThat( idField.getType().determineRawClass().getName() ).isEqualTo( Integer.class.getName() );

			final FieldDetails nameField = rootEntity.getClassDetails().findFieldByName( "name" );
			assertThat( nameField.getType().determineRawClass().getName() ).isEqualTo( String.class.getName() );
			assertThat( nameField.getDirectAnnotationUsage( JavaType.class ) ).isNotNull();

			final FieldDetails qtyField = rootEntity.getClassDetails().findFieldByName( "quantity" );
			assertThat( qtyField.getType().determineRawClass().getName() ).isEqualTo( int.class.getName() );
		}
	}

	@Test
	void testSemiSimpleDynamicModel() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/dynamic/dynamic-semi-simple.xml" );
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
			final EntityTypeMetadata rootEntity = hierarchy.getRoot();
			assertThat( rootEntity.getClassDetails().getClassName() ).isNull();
			assertThat( rootEntity.getClassDetails().getName() ).isEqualTo( "Contact" );

			final FieldDetails idField = rootEntity.getClassDetails().findFieldByName( "id" );
			assertThat( idField.getType().determineRawClass().getName() ).isEqualTo( Integer.class.getName() );

			final FieldDetails nameField = rootEntity.getClassDetails().findFieldByName( "name" );
			assertThat( nameField.getType().determineRawClass().getClassName() ).isNull();
			assertThat( nameField.getType().getName() ).isEqualTo( "Name" );
			assertThat( nameField.getDirectAnnotationUsage( Target.class ) ).isNotNull();
			assertThat( nameField.getDirectAnnotationUsage( Target.class ).value() ).isEqualTo( "Name" );

			assertThat( nameField.getType().determineRawClass().getFields() ).hasSize( 2 );

			final FieldDetails labels = rootEntity.getClassDetails().findFieldByName( "labels" );
			assertThat( labels.getType().determineRawClass().getName() ).isEqualTo( SortedSet.class.getName() );
			final ElementCollection elementCollection = labels.getDirectAnnotationUsage( ElementCollection.class );
			assertThat( elementCollection.targetClass().getName() ).isEqualTo( String.class.getName() );
			final CollectionClassification collectionClassification = labels.getDirectAnnotationUsage( CollectionClassification.class );
			assertThat( collectionClassification.value() ).isEqualTo( LimitedCollectionClassification.SET );
			final CollectionTable collectionTable = labels.getDirectAnnotationUsage( CollectionTable.class );
			assertThat( collectionTable.name() ).isEqualTo( "labels" );
			assertThat( labels.getDirectAnnotationUsage( SortNatural.class ) ).isNotNull();
			final JoinColumn[] joinColumns = collectionTable.joinColumns();
			assertThat( joinColumns ).hasSize( 1 );
			assertThat( joinColumns[0].name() ).isEqualTo( "contact_fk" );
		}
	}

	@Test
	void testIdClass() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/dynamic/dynamic-id-class.xml" );
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
			final EntityTypeMetadata rootEntity = hierarchy.getRoot();
			assertThat( rootEntity.getClassDetails().getName() ).isEqualTo( Employee.class.getName() );

			final IdClass idClass = rootEntity.getClassDetails().getDirectAnnotationUsage( IdClass.class );
			assertThat( idClass ).isNotNull();
			assertThat( idClass.value().getName() ).isEqualTo( EmployeePK.class.getName() );
		}
	}

	@Test
	void testOneToMany() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/dynamic/dynamic-plurals.xml" );
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
			final EntityTypeMetadata rootEntity = categorizedDomainModel.getEntityHierarchies().iterator().next().getRoot();
			assertThat( rootEntity.getClassDetails().getName() ).isEqualTo( Employee.class.getName() );

			final FieldDetails oneToMany = rootEntity.getClassDetails().findFieldByName( "oneToMany" );
			assertThat( oneToMany.getType().determineRawClass().getName() ).isEqualTo( List.class.getName() );
			final OneToMany oneToManyAnn = oneToMany.getDirectAnnotationUsage( OneToMany.class );
			assertThat( oneToManyAnn.fetch() ).isEqualTo( FetchType.EAGER );
			assertThat( oneToMany.getDirectAnnotationUsage( NotFound.class ).action() ).isEqualTo( NotFoundAction.IGNORE );
			assertThat( oneToMany.getDirectAnnotationUsage( OnDelete.class ).action() ).isEqualTo( OnDeleteAction.CASCADE );
			final JoinColumn[] joinColumns = oneToMany.getRepeatedAnnotationUsages(
					JoinColumn.class,
					metadataBuildingContext.getBootstrapContext().getModelsContext()
			);
			assertThat( joinColumns ).hasSize( 1 );
			final JoinColumn joinColumn = joinColumns[0];
			assertThat( joinColumn.name() ).isEqualTo( "employee_id" );
			assertThat( joinColumn.insertable() ).isFalse();
			assertThat( joinColumn.updatable() ).isFalse();
			final ForeignKey foreignKey = joinColumn.foreignKey();
			assertThat( foreignKey.name() ).isEqualTo( "employee_fk" );
			assertThat( foreignKey.value() ).isEqualTo( ConstraintMode.NO_CONSTRAINT );
			final CheckConstraint[] checkConstraints = joinColumn.check();
			assertThat( checkConstraints ).hasSize( 1 );
			assertThat( checkConstraints[0].name() ).isEqualTo( "employee_id_nn" );
			assertThat( checkConstraints[0].constraint() ).isEqualTo( "employee_id is not null" );
			assertThat( oneToMany.getDirectAnnotationUsage( OneToMany.class ).cascade() )
					.contains( CascadeType.PERSIST, CascadeType.REMOVE );
		}
	}
}
