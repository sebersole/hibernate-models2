/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.embeddable;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class EmbeddableBindingTests {
	@Test
	@ServiceRegistry
	void testExplicitEmbedded(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ExplicitEmbeddedEntity.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "address" );
					assertThat( property.getValue() ).isInstanceOf( Component.class );
					final Component component = (Component) property.getValue();

					assertThat( component.isEmbedded() ).isTrue();
					assertThat( component.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( component.getProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.containsExactly( "line1", "zipCode" );
					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				ExplicitEmbeddedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testImplicitEmbedded(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ImplicitEmbeddedEntity.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "address" );
					assertThat( property.getValue() ).isInstanceOf( Component.class );
					final Component component = (Component) property.getValue();

					assertThat( component.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				ImplicitEmbeddedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testAttributeOverride(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OverrideEmbeddedEntity.class.getName() );
					final org.hibernate.mapping.Property property = entityBinding.getProperty( "address" );
					final Component component = (Component) property.getValue();

					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "street", "postal_code" );
				},
				scope.getRegistry(),
				OverrideEmbeddedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddedOnSecondaryTable(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( SecondaryTableEmbeddedEntity.class.getName() );
					assertThat( entityBinding.getJoins() ).hasSize( 1 );
					final Join join = entityBinding.getJoins().get( 0 );
					assertThat( entityBinding.getUnjoinedProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.containsExactly( "id" );
					assertThat( join.getProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.containsExactly( "address" );

					final Component component = (Component) join.getProperties().get( 0 ).getValue();
					assertThat( component.getTable() ).isSameAs( join.getTable() );
					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "street", "postal_code" );
				},
				scope.getRegistry(),
				SecondaryTableEmbeddedEntity.class
		);
	}

	@Entity(name="ExplicitEmbeddedEntity")
	@Table(name="explicit_embedded")
	public static class ExplicitEmbeddedEntity {
		@Id
		private Integer id;
		@Embedded
		private Address address;
	}

	@Entity(name="ImplicitEmbeddedEntity")
	@Table(name="implicit_embedded")
	public static class ImplicitEmbeddedEntity {
		@Id
		private Integer id;
		private Address address;
	}

	@Entity(name="OverrideEmbeddedEntity")
	@Table(name="override_embedded")
	public static class OverrideEmbeddedEntity {
		@Id
		private Integer id;
		@Embedded
		@AttributeOverride(name = "line1", column = @Column(name = "street"))
		@AttributeOverride(name = "zipCode", column = @Column(name = "postal_code"))
		private Address address;
	}

	@Entity(name="SecondaryTableEmbeddedEntity")
	@Table(name="secondary_table_embedded")
	@SecondaryTable(name="secondary_table_embedded_details")
	public static class SecondaryTableEmbeddedEntity {
		@Id
		private Integer id;
		@Embedded
		@AttributeOverride(name = "line1", column = @Column(name = "street", table = "secondary_table_embedded_details"))
		@AttributeOverride(name = "zipCode", column = @Column(name = "postal_code", table = "secondary_table_embedded_details"))
		private Address address;
	}

	@Embeddable
	public static class Address {
		private String line1;
		private String zipCode;
	}
}
