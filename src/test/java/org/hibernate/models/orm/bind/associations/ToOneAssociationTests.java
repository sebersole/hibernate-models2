/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.associations;

import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.models.orm.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class ToOneAssociationTests {
	@Test
	@ServiceRegistry
	void testImplicitManyToOne(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToOneOwner.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "parent" );
					assertThat( property.getValue() ).isInstanceOf( ManyToOne.class );
					final ManyToOne value = (ManyToOne) property.getValue();

					assertThat( value.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningOneToOne(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToOneOwner.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "parent" );
					assertThat( property.getValue() ).isInstanceOf( ManyToOne.class );
					final ManyToOne value = (ManyToOne) property.getValue();

					assertThat( value.isLogicalOneToOne() ).isTrue();
					assertThat( value.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk" );
					assertThat( value.getColumns().get( 0 ).isUnique() ).isTrue();
					assertThat( value.getColumns().get( 0 ).isNullable() ).isFalse();
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				Parent.class,
				OneToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToOneMappedBy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByOneToOneParent.class.getName() );
					final org.hibernate.mapping.Property property = inverseEntityBinding.getProperty( "child" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.OneToOne.class );
					final org.hibernate.mapping.OneToOne value = (org.hibernate.mapping.OneToOne) property.getValue();

					assertThat( value.getReferencedEntityName() ).isEqualTo( MappedByOneToOneChild.class.getName() );
					assertThat( value.getMappedByProperty() ).isEqualTo( "parent" );
					assertThat( value.getReferencedPropertyName() ).isEqualTo( "parent" );
					assertThat( value.isReferenceToPrimaryKey() ).isFalse();
					assertThat( value.getForeignKeyType() ).isEqualTo( org.hibernate.type.ForeignKeyDirection.TO_PARENT );
					assertThat( value.getTable().getName() ).isEqualTo( "mapped_by_one_to_one_parents" );
					assertThat( value.getColumns() ).isEmpty();
				},
				scope.getRegistry(),
				MappedByOneToOneParent.class,
				MappedByOneToOneChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToOneMappedByJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByJoinTableOneToOneParent.class.getName() );
					final Join inverseJoin = inverseEntityBinding.getJoins().get( 0 );
					final org.hibernate.mapping.Property property = inverseJoin.getProperties().get( 0 );
					assertThat( property.getValue() ).isInstanceOf( ManyToOne.class );
					final ManyToOne value = (ManyToOne) property.getValue();

					assertThat( inverseJoin.isInverse() ).isTrue();
					assertThat( inverseJoin.getTable().getName() ).isEqualTo( "mapped_by_one_to_one_links" );
					assertThat( inverseJoin.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( value.isLogicalOneToOne() ).isTrue();
					assertThat( value.getReferencedEntityName() ).isEqualTo( MappedByJoinTableOneToOneChild.class.getName() );
					assertThat( value.getReferencedPropertyName() ).isEqualTo( "parent" );
					assertThat( value.isReferenceToPrimaryKey() ).isFalse();
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
				},
				scope.getRegistry(),
				MappedByJoinTableOneToOneParent.class,
				MappedByJoinTableOneToOneChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToOneMappedByManyToOneFails(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				InvalidMappedByOneToOneParent.class,
				InvalidMappedByManyToOneChild.class
		) ).isInstanceOf( MappingException.class )
				.hasMessageContaining( "mappedBy did not name an owning one-to-one attribute" );
	}

	@Test
	@ServiceRegistry
	void testCompositeManyToOne(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeManyToOneOwner.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "parent" );
					assertThat( property.getValue() ).isInstanceOf( ManyToOne.class );
					final ManyToOne value = (ManyToOne) property.getValue();

					assertThat( value.getReferencedEntityName() ).isEqualTo( CompositeParent.class.getName() );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id1", "parent_id2" );
					assertThat( entityBinding.getTable().getForeignKeyCollection() ).hasSize( 1 );
				},
				scope.getRegistry(),
				CompositeParent.class,
				CompositeManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testCompositeManyToOneWithReferencedColumnNames(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ExplicitCompositeManyToOneOwner.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "parent" );
					final ManyToOne value = (ManyToOne) property.getValue();

					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk1", "parent_fk2" );
				},
				scope.getRegistry(),
				CompositeParent.class,
				ExplicitCompositeManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testMixedJoinColumnTables(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				CompositeParent.class,
				MixedJoinColumnTablesOwner.class
		) ).isInstanceOf( MappingException.class )
					.hasMessageContaining( "To-one join columns cannot span multiple tables" );
	}

	@Test
	@ServiceRegistry
	void testManyToOneJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( JoinTableManyToOneOwner.class.getName() );
					final Join join = entityBinding.getJoins().get( 0 );
					final ManyToOne value = (ManyToOne) join.getProperties().get( 0 ).getValue();

					assertThat( join.getTable().getName() ).isEqualTo( "owner_parent_links" );
					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( join.getTable().getForeignKeyCollection() ).hasSize( 2 );
				},
				scope.getRegistry(),
				Parent.class,
				JoinTableManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testImplicitManyToOneJoinTableName(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ImplicitJoinTableManyToOneOwner.class.getName() );
					final Join join = entityBinding.getJoins().get( 0 );

					assertThat( join.getTable().getName() ).isEqualTo( "implicit_join_table_many_to_one_owners_parents" );
					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
				},
				scope.getRegistry(),
				Parent.class,
				ImplicitJoinTableManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection ).isInstanceOf( org.hibernate.mapping.Set.class );
					assertThat( collection.getRole() ).isEqualTo( ManyToManyOwner.class.getName() + ".parents" );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_parent_sets" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 2 );
					assertThat( context.getMetadataCollector().getCollectionBinding( collection.getRole() ) ).isSameAs( collection );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyImplicitJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyImplicitJoinTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() )
							.isEqualTo( "many_to_many_implicit_join_table_owners_parents" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parents_id" );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 2 );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyImplicitJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyMapJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyMapOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "parents" ).getValue();
					final BasicValue index = (BasicValue) collection.getIndex();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_parent_maps" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_key" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyMapOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyEntityMapKeyJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyEntityMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne index = (ManyToOne) collection.getIndex();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_parent_entity_key_maps" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( index.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_key_id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				Child.class,
				ManyToManyEntityMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOwningManyToManyPropertyMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ManyToManyPropertyMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_parent_property_maps" );
					assertThat( collection.hasMapKeyProperty() ).isTrue();
					assertThat( collection.getMapKeyPropertyName() ).isEqualTo( "name" );
					assertThat( collection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "name" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				ManyToManyPropertyMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseManyToManyMappedBy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByManyToManyParent.class.getName() );
					final Collection inverseCollection = (Collection) inverseEntityBinding.getProperty( "owners" ).getValue();
					final ManyToOne inverseElement = (ManyToOne) inverseCollection.getElement();

					assertThat( inverseCollection ).isInstanceOf( org.hibernate.mapping.Set.class );
					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parents" );
					assertThat( inverseCollection.getCollectionTable().getName() ).isEqualTo( "mapped_by_owner_parent_sets" );
					assertThat( inverseCollection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
					assertThat( inverseElement.getReferencedEntityName() )
							.isEqualTo( MappedByManyToManyOwner.class.getName() );
					assertThat( inverseElement.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( context.getMetadataCollector().getCollectionBinding( inverseCollection.getRole() ) )
							.isSameAs( inverseCollection );
				},
				scope.getRegistry(),
				MappedByManyToManyParent.class,
				MappedByManyToManyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToManyMappedBy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( MappedByOneToManyParent.class.getName() );
					final Collection inverseCollection = (Collection) inverseEntityBinding.getProperty( "children" ).getValue();

					assertThat( inverseCollection ).isInstanceOf( org.hibernate.mapping.Set.class );
					assertThat( inverseCollection.isInverse() ).isTrue();
					assertThat( inverseCollection.isOneToMany() ).isTrue();
					assertThat( inverseCollection.getMappedByProperty() ).isEqualTo( "parent" );
					assertThat( inverseCollection.getCollectionTable().getName() ).isEqualTo( "mapped_by_children" );
					assertThat( inverseCollection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk" );
					assertThat( inverseCollection.getElement() ).isInstanceOf( org.hibernate.mapping.OneToMany.class );
					assertThat( ( (org.hibernate.mapping.OneToMany) inverseCollection.getElement() ).getReferencedEntityName() )
							.isEqualTo( MappedByChild.class.getName() );
					assertThat( context.getMetadataCollector().getCollectionBinding( inverseCollection.getRole() ) )
							.isSameAs( inverseCollection );
				},
				scope.getRegistry(),
				MappedByOneToManyParent.class,
				MappedByChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testInverseOneToManyMappedByCompositeOwner(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass inverseEntityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeMappedByOneToManyParent.class.getName() );
					final Collection inverseCollection = (Collection) inverseEntityBinding.getProperty( "children" ).getValue();

					assertThat( inverseCollection.getCollectionTable().getName() ).isEqualTo( "composite_mapped_by_children" );
					assertThat( inverseCollection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk1", "parent_fk2" );
					assertThat( inverseCollection.getElement() ).isInstanceOf( org.hibernate.mapping.OneToMany.class );
				},
				scope.getRegistry(),
				CompositeMappedByOneToManyParent.class,
				CompositeMappedByChild.class
		);
	}

	@Test
	@ServiceRegistry
	void testUnidirectionalOneToManyJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToManyJoinTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection ).isInstanceOf( org.hibernate.mapping.Set.class );
					assertThat( collection.getRole() ).isEqualTo( OneToManyJoinTableOwner.class.getName() + ".children" );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_child_links" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 2 );
				},
				scope.getRegistry(),
				Child.class,
				OneToManyJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testUnidirectionalOneToManyImplicitJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToManyImplicitJoinTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() )
							.isEqualTo( "one_to_many_implicit_join_table_owners_children" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "children_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 2 );
				},
				scope.getRegistry(),
				Child.class,
				OneToManyImplicitJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testUnidirectionalOneToManyMapJoinTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToManyMapJoinTableOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "children" ).getValue();
					final BasicValue index = (BasicValue) collection.getIndex();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_child_maps" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_key" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
				},
				scope.getRegistry(),
				Child.class,
				OneToManyMapJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testUnidirectionalOneToManyPropertyMapKey(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OneToManyPropertyMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_child_property_maps" );
					assertThat( collection.hasMapKeyProperty() ).isTrue();
					assertThat( collection.getMapKeyPropertyName() ).isEqualTo( "name" );
					assertThat( collection.getIndex().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "name" );
					assertThat( element.getReferencedEntityName() ).isEqualTo( Child.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
				},
				scope.getRegistry(),
				Child.class,
				OneToManyPropertyMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToManyJoinTableWithCompositeOwner(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeOwnerManyToManyOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "composite_owner_parent_sets" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_fk1", "owner_fk2" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				CompositeOwnerManyToManyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToManyJoinTableWithCompositeTarget(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeTargetManyToManyOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "parents" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_composite_parent_sets" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk1", "parent_fk2" );
				},
				scope.getRegistry(),
				CompositeParent.class,
				CompositeTargetManyToManyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOneToManyJoinTableWithCompositeOwner(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeOwnerOneToManyJoinTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "composite_owner_child_links" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_fk1", "owner_fk2" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_id" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
				},
				scope.getRegistry(),
				Child.class,
				CompositeOwnerOneToManyJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testOneToManyJoinTableWithCompositeTarget(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeTargetOneToManyJoinTableOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "children" ).getValue();
					final ManyToOne element = (ManyToOne) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_composite_child_links" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "child_fk1", "child_fk2" );
					assertThat( element.getColumns().get( 0 ).isUnique() ).isTrue();
					assertThat( element.getColumns().get( 1 ).isUnique() ).isTrue();
				},
				scope.getRegistry(),
				CompositeChild.class,
				CompositeTargetOneToManyJoinTableOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testCompositeManyToOneJoinTableWithReferencedColumnNames(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( JoinTableCompositeManyToOneOwner.class.getName() );
					final Join join = entityBinding.getJoins().get( 0 );
					final ManyToOne value = (ManyToOne) join.getProperties().get( 0 ).getValue();

					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_fk1", "parent_fk2" );
				},
				scope.getRegistry(),
				CompositeParent.class,
				JoinTableCompositeManyToOneOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testCompositeOwnerManyToOneJoinTableWithReferencedColumnNames(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeOwnerJoinTableManyToOneOwner.class.getName() );
					final Join join = entityBinding.getJoins().get( 0 );
					final ManyToOne value = (ManyToOne) join.getProperties().get( 0 ).getValue();

					assertThat( join.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_fk1", "owner_fk2" );
					assertThat( value.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "parent_id" );
				},
				scope.getRegistry(),
				Parent.class,
				CompositeOwnerJoinTableManyToOneOwner.class
		);
	}

	@Entity(name="Parent")
	@Table(name="parents")
	public static class Parent {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Child")
	@Table(name="children")
	public static class Child {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="ManyToOneOwner")
	@Table(name="many_to_one_owners")
	public static class ManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		private Parent parent;
	}

	@Entity(name="OneToOneOwner")
	@Table(name="one_to_one_owners")
	public static class OneToOneOwner {
		@Id
		private Integer id;
		@OneToOne(optional = false)
		@JoinColumn(name = "parent_fk")
		private Parent parent;
	}

	@Entity(name="MappedByOneToOneParent")
	@Table(name="mapped_by_one_to_one_parents")
	public static class MappedByOneToOneParent {
		@Id
		private Integer id;
		@OneToOne(mappedBy = "parent")
		private MappedByOneToOneChild child;
	}

	@Entity(name="MappedByOneToOneChild")
	@Table(name="mapped_by_one_to_one_children")
	public static class MappedByOneToOneChild {
		@Id
		private Integer id;
		@OneToOne
		@JoinColumn(name = "parent_fk", referencedColumnName = "id")
		private MappedByOneToOneParent parent;
	}

	@Entity(name="MappedByJoinTableOneToOneParent")
	@Table(name="mapped_by_join_table_one_to_one_parents")
	public static class MappedByJoinTableOneToOneParent {
		@Id
		private Integer id;
		@OneToOne(mappedBy = "parent")
		private MappedByJoinTableOneToOneChild child;
	}

	@Entity(name="MappedByJoinTableOneToOneChild")
	@Table(name="mapped_by_join_table_one_to_one_children")
	public static class MappedByJoinTableOneToOneChild {
		@Id
		private Integer id;
		@OneToOne
		@JoinTable(
				name = "mapped_by_one_to_one_links",
				joinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private MappedByJoinTableOneToOneParent parent;
	}

	@Entity(name="InvalidMappedByOneToOneParent")
	@Table(name="invalid_mapped_by_one_to_one_parents")
	public static class InvalidMappedByOneToOneParent {
		@Id
		private Integer id;
		@OneToOne(mappedBy = "parent")
		private InvalidMappedByManyToOneChild child;
	}

	@Entity(name="InvalidMappedByManyToOneChild")
	@Table(name="invalid_mapped_by_many_to_one_children")
	public static class InvalidMappedByManyToOneChild {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_fk", referencedColumnName = "id")
		private InvalidMappedByOneToOneParent parent;
	}

	@Entity(name="CompositeParent")
	@Table(name="composite_parents")
	public static class CompositeParent {
		@EmbeddedId
		private Pk id;
		private String name;
	}

	@Entity(name="CompositeChild")
	@Table(name="composite_children")
	public static class CompositeChild {
		@EmbeddedId
		private Pk id;
		private String name;
	}

	@Entity(name="CompositeManyToOneOwner")
	@Table(name="composite_many_to_one_owners")
	public static class CompositeManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		private CompositeParent parent;
	}

	@Entity(name="ExplicitCompositeManyToOneOwner")
	@Table(name="explicit_composite_many_to_one_owners")
	public static class ExplicitCompositeManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumns({
				@JoinColumn(name = "parent_fk2", referencedColumnName = "id2"),
				@JoinColumn(name = "parent_fk1", referencedColumnName = "id1")
		})
		private CompositeParent parent;
	}

	@Entity(name="MixedJoinColumnTablesOwner")
	@Table(name="mixed_join_column_tables")
	@SecondaryTable(name="mixed_join_column_tables_details")
	public static class MixedJoinColumnTablesOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumns({
				@JoinColumn(name = "parent_fk1", referencedColumnName = "id1"),
				@JoinColumn(name = "parent_fk2", referencedColumnName = "id2", table = "mixed_join_column_tables_details")
		})
		private CompositeParent parent;
	}

	@Entity(name="JoinTableManyToOneOwner")
	@Table(name="join_table_many_to_one_owners")
	public static class JoinTableManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinTable(
				name = "owner_parent_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Parent parent;
	}

	@Entity(name="ImplicitJoinTableManyToOneOwner")
	@Table(name="implicit_join_table_many_to_one_owners")
	public static class ImplicitJoinTableManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinTable(
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Parent parent;
	}

	@Entity(name="ManyToManyOwner")
	@Table(name="many_to_many_owners")
	public static class ManyToManyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_sets",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Set<Parent> parents;
	}

	@Entity(name="ManyToManyImplicitJoinTableOwner")
	@Table(name="many_to_many_implicit_join_table_owners")
	public static class ManyToManyImplicitJoinTableOwner {
		@Id
		private Integer id;
		@ManyToMany
		private Set<Parent> parents;
	}

	@Entity(name="ManyToManyMapOwner")
	@Table(name="many_to_many_map_owners")
	public static class ManyToManyMapOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_key")
		private Map<String, Parent> parents;
	}

	@Entity(name="ManyToManyEntityMapKeyOwner")
	@Table(name="many_to_many_entity_map_key_owners")
	public static class ManyToManyEntityMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_entity_key_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKeyJoinColumn(name = "child_key_id", referencedColumnName = "id")
		private Map<Child, Parent> parents;
	}

	@Entity(name="ManyToManyPropertyMapKeyOwner")
	@Table(name="many_to_many_property_map_key_owners")
	public static class ManyToManyPropertyMapKeyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_parent_property_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		@MapKey(name = "name")
		private Map<String, Parent> parents;
	}

	@Entity(name="MappedByManyToManyParent")
	@Table(name="mapped_by_many_to_many_parents")
	public static class MappedByManyToManyParent {
		@Id
		private Integer id;
		@ManyToMany(mappedBy = "parents")
		private Set<MappedByManyToManyOwner> owners;
	}

	@Entity(name="MappedByManyToManyOwner")
	@Table(name="mapped_by_many_to_many_owners")
	public static class MappedByManyToManyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "mapped_by_owner_parent_sets",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Set<MappedByManyToManyParent> parents;
	}

	@Entity(name="MappedByOneToManyParent")
	@Table(name="mapped_by_one_to_many_parents")
	public static class MappedByOneToManyParent {
		@Id
		private Integer id;
		@OneToMany(mappedBy = "parent")
		private Set<MappedByChild> children;
	}

	@Entity(name="MappedByChild")
	@Table(name="mapped_by_children")
	public static class MappedByChild {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_fk", referencedColumnName = "id")
		private MappedByOneToManyParent parent;
	}

	@Entity(name="CompositeMappedByOneToManyParent")
	@Table(name="composite_mapped_by_one_to_many_parents")
	public static class CompositeMappedByOneToManyParent {
		@EmbeddedId
		private Pk id;
		@OneToMany(mappedBy = "parent")
		private Set<CompositeMappedByChild> children;
	}

	@Entity(name="CompositeMappedByChild")
	@Table(name="composite_mapped_by_children")
	public static class CompositeMappedByChild {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinColumns({
				@JoinColumn(name = "parent_fk2", referencedColumnName = "id2"),
				@JoinColumn(name = "parent_fk1", referencedColumnName = "id1")
		})
		private CompositeMappedByOneToManyParent parent;
	}

	@Entity(name="OneToManyJoinTableOwner")
	@Table(name="one_to_many_join_table_owners")
	public static class OneToManyJoinTableOwner {
		@Id
		private Integer id;
		@OneToMany
		@JoinTable(
				name = "owner_child_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id")
		)
		private Set<Child> children;
	}

	@Entity(name="OneToManyImplicitJoinTableOwner")
	@Table(name="one_to_many_implicit_join_table_owners")
	public static class OneToManyImplicitJoinTableOwner {
		@Id
		private Integer id;
		@OneToMany
		private Set<Child> children;
	}

	@Entity(name="OneToManyMapJoinTableOwner")
	@Table(name="one_to_many_map_join_table_owners")
	public static class OneToManyMapJoinTableOwner {
		@Id
		private Integer id;
		@OneToMany
		@JoinTable(
				name = "owner_child_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "child_key")
		private Map<String, Child> children;
	}

	@Entity(name="OneToManyPropertyMapKeyOwner")
	@Table(name="one_to_many_property_map_key_owners")
	public static class OneToManyPropertyMapKeyOwner {
		@Id
		private Integer id;
		@OneToMany
		@JoinTable(
				name = "owner_child_property_maps",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id")
		)
		@MapKey(name = "name")
		private Map<String, Child> children;
	}

	@Entity(name="CompositeOwnerManyToManyOwner")
	@Table(name="composite_owner_many_to_many_owners")
	public static class CompositeOwnerManyToManyOwner {
		@EmbeddedId
		private Pk id;
		@ManyToMany
		@JoinTable(
				name = "composite_owner_parent_sets",
				joinColumns = {
						@JoinColumn(name = "owner_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "owner_fk1", referencedColumnName = "id1")
				},
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Set<Parent> parents;
	}

	@Entity(name="CompositeTargetManyToManyOwner")
	@Table(name="composite_target_many_to_many_owners")
	public static class CompositeTargetManyToManyOwner {
		@Id
		private Integer id;
		@ManyToMany
		@JoinTable(
				name = "owner_composite_parent_sets",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = {
						@JoinColumn(name = "parent_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "parent_fk1", referencedColumnName = "id1")
				}
		)
		private Set<CompositeParent> parents;
	}

	@Entity(name="CompositeOwnerOneToManyJoinTableOwner")
	@Table(name="composite_owner_one_to_many_join_table_owners")
	public static class CompositeOwnerOneToManyJoinTableOwner {
		@EmbeddedId
		private Pk id;
		@OneToMany
		@JoinTable(
				name = "composite_owner_child_links",
				joinColumns = {
						@JoinColumn(name = "owner_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "owner_fk1", referencedColumnName = "id1")
				},
				inverseJoinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id")
		)
		private Set<Child> children;
	}

	@Entity(name="CompositeTargetOneToManyJoinTableOwner")
	@Table(name="composite_target_one_to_many_join_table_owners")
	public static class CompositeTargetOneToManyJoinTableOwner {
		@Id
		private Integer id;
		@OneToMany
		@JoinTable(
				name = "owner_composite_child_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = {
						@JoinColumn(name = "child_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "child_fk1", referencedColumnName = "id1")
				}
		)
		private Set<CompositeChild> children;
	}

	@Entity(name="JoinTableCompositeManyToOneOwner")
	@Table(name="join_table_composite_many_to_one_owners")
	public static class JoinTableCompositeManyToOneOwner {
		@Id
		private Integer id;
		@jakarta.persistence.ManyToOne
		@JoinTable(
				name = "owner_composite_parent_links",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				inverseJoinColumns = {
						@JoinColumn(name = "parent_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "parent_fk1", referencedColumnName = "id1")
				}
		)
		private CompositeParent parent;
	}

	@Entity(name="CompositeOwnerJoinTableManyToOneOwner")
	@Table(name="composite_owner_join_table_many_to_one_owners")
	public static class CompositeOwnerJoinTableManyToOneOwner {
		@EmbeddedId
		private Pk id;
		@jakarta.persistence.ManyToOne
		@JoinTable(
				name = "composite_owner_parent_links",
				joinColumns = {
						@JoinColumn(name = "owner_fk2", referencedColumnName = "id2"),
						@JoinColumn(name = "owner_fk1", referencedColumnName = "id1")
				},
				inverseJoinColumns = @JoinColumn(name = "parent_id", referencedColumnName = "id")
		)
		private Parent parent;
	}

	@Embeddable
	public static class Pk {
		private Integer id1;
		private Integer id2;
	}
}
