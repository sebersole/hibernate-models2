/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.associations;

import org.hibernate.MappingException;
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

	@Entity(name="CompositeParent")
	@Table(name="composite_parents")
	public static class CompositeParent {
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
