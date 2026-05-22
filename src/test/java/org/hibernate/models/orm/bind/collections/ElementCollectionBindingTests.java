/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.collections;

import java.util.Set;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.UniqueConstraint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class ElementCollectionBindingTests {
	@Test
	@ServiceRegistry
	void testBasicSetElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( SetOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Set.class );
					final Collection collection = (Collection) property.getValue();
					final BasicValue element = (BasicValue) collection.getElement();

					assertThat( collection.getRole() ).isEqualTo( SetOwner.class.getName() + ".labels" );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "set_owner_labels" );
					assertThat( collection.getCollectionTable().getOptions() ).isEqualTo( "collection table options" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 1 );
					final org.hibernate.mapping.ForeignKey foreignKey = collection.getCollectionTable()
							.getForeignKeyCollection()
							.iterator()
							.next();
					assertThat( foreignKey.getName() ).isEqualTo( "fk_set_owner_labels_owner" );
					final org.hibernate.mapping.UniqueKey uniqueKey = collection.getCollectionTable()
							.getUniqueKey( "uk_set_owner_labels_owner_label" );
					assertThat( uniqueKey ).isNotNull();
					assertThat( uniqueKey.getOptions() ).isEqualTo( "unique options" );
					assertThat( uniqueKey.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id", "label" );
					final org.hibernate.mapping.Index index = collection.getCollectionTable()
							.getIndex( "idx_set_owner_labels_label" );
					assertThat( index ).isNotNull();
					assertThat( index.getOptions() ).isEqualTo( "index options" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
					assertThat( context.getMetadataCollector().getCollectionBinding( collection.getRole() ) ).isSameAs( collection );
				},
				scope.getRegistry(),
				SetOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testCompositeOwnerElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( CompositeSetOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "labels" ).getValue();
					final BasicValue element = (BasicValue) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "composite_set_owner_labels" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id1", "owner_id2" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
				},
				scope.getRegistry(),
				CompositeSetOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddableElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmbeddableElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "addresses" ).getValue();
					final Component element = (Component) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_addresses" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				EmbeddableElementOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddableElementCollectionAttributeOverride(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( OverrideEmbeddableElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "addresses" ).getValue();
					final Component element = (Component) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "override_owner_addresses" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "street", "postal_code" );
				},
				scope.getRegistry(),
				OverrideEmbeddableElementOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddedElementCollectionIntent(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmbeddedIntentElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "addresses" ).getValue();
					final Component element = (Component) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "embedded_intent_owner_addresses" );
					assertThat( element.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				EmbeddedIntentElementOwner.class
		);
	}

	@Entity(name="SetOwner")
	@Table(name="set_owners")
	public static class SetOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "set_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id"),
				foreignKey = @ForeignKey(name = "fk_set_owner_labels_owner"),
				uniqueConstraints = @UniqueConstraint(
						name = "uk_set_owner_labels_owner_label",
						columnNames = { "owner_id", "label" },
						options = "unique options"
				),
				indexes = @Index(
						name = "idx_set_owner_labels_label",
						columnList = "label",
						options = "index options"
				),
				options = "collection table options"
		)
		@Column(name = "label")
		private Set<String> labels;
	}

	@Entity(name="CompositeSetOwner")
	@Table(name="composite_set_owners")
	public static class CompositeSetOwner {
		@EmbeddedId
		private Pk id;
		@ElementCollection
		@CollectionTable(
				name = "composite_set_owner_labels",
				joinColumns = {
						@JoinColumn(name = "owner_id2", referencedColumnName = "id2"),
						@JoinColumn(name = "owner_id1", referencedColumnName = "id1")
				}
		)
		@Column(name = "label")
		private Set<String> labels;
	}

	@Entity(name="EmbeddableElementOwner")
	@Table(name="embeddable_element_owners")
	public static class EmbeddableElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		private Set<Address> addresses;
	}

	@Entity(name="OverrideEmbeddableElementOwner")
	@Table(name="override_embeddable_element_owners")
	public static class OverrideEmbeddableElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "override_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@AttributeOverride(name = "line1", column = @Column(name = "street"))
		@AttributeOverride(name = "zipCode", column = @Column(name = "postal_code"))
		private Set<Address> addresses;
	}

	@Entity(name="EmbeddedIntentElementOwner")
	@Table(name="embedded_intent_element_owners")
	public static class EmbeddedIntentElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@Embedded
		@CollectionTable(
				name = "embedded_intent_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		private Set<Address> addresses;
	}

	@Embeddable
	public static class Pk {
		private Integer id1;
		private Integer id2;
	}

	@Embeddable
	public static class Address {
		private String line1;
		private String zipCode;
	}
}
