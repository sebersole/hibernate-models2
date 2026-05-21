/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.associations;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
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

	@Embeddable
	public static class Pk {
		private Integer id1;
		private Integer id2;
	}
}
