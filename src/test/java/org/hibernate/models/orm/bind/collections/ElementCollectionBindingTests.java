/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.collections;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;

import org.hibernate.annotations.Bag;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.TemporalType;
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
	void testBasicListElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ListOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) property.getValue();
					final BasicValue element = (BasicValue) collection.getElement();
					final BasicValue index = (BasicValue) collection.getIndex();

					assertThat( collection.getRole() ).isEqualTo( ListOwner.class.getName() + ".labels" );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "list_owner_labels" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "position" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
				},
				scope.getRegistry(),
				ListOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testImplicitOrderColumnListElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ImplicitListOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) property.getValue();
					final BasicValue index = (BasicValue) collection.getIndex();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "implicit_list_owner_labels" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "idx" );
				},
				scope.getRegistry(),
				ImplicitListOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testBagListElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( BagListOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );

					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Bag.class );
				},
				scope.getRegistry(),
				BagListOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testBasicMapElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( MapOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Map.class );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) property.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();
					final BasicValue element = (BasicValue) collection.getElement();

					assertThat( collection.getRole() ).isEqualTo( MapOwner.class.getName() + ".labels" );
					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "map_owner_labels" );
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label_key" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
				},
				scope.getRegistry(),
				MapOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testImplicitMapKeyColumnElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ImplicitMapOwner.class.getName() );
					final Property property = entityBinding.getProperty( "labels" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Map.class );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) property.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "implicit_map_owner_labels" );
					assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id" );
				},
				scope.getRegistry(),
				ImplicitMapOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEnumMapKeyElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EnumMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();

					assertThat( key.getEnumerationStyle() ).isEqualTo( EnumType.STRING );
					assertThat( key.resolve().getDomainJavaType().getJavaType() ).isEqualTo( LabelKind.class );
				},
				scope.getRegistry(),
				EnumMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testMapKeyClassElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( MapKeyClassOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();

					assertThat( key.getEnumerationStyle() ).isEqualTo( EnumType.STRING );
					assertThat( key.resolve().getDomainJavaType().getJavaType() ).isEqualTo( LabelKind.class );
				},
				scope.getRegistry(),
				MapKeyClassOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testTemporalMapKeyElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( TemporalMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();

					assertThat( key.getTemporalPrecision() ).isEqualTo( TemporalType.DATE );
				},
				scope.getRegistry(),
				TemporalMapKeyOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testConvertedMapKeyElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( ConvertedMapKeyOwner.class.getName() );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) entityBinding.getProperty( "labels" )
							.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();

					assertThat( key.getJpaAttributeConverterDescriptor() ).isNotNull();
				},
				scope.getRegistry(),
				ConvertedMapKeyOwner.class
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
	void testEmbeddableListElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmbeddableListOwner.class.getName() );
					final Property property = entityBinding.getProperty( "addresses" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.List.class );
					final org.hibernate.mapping.List collection = (org.hibernate.mapping.List) property.getValue();
					final Component element = (Component) collection.getElement();
					final BasicValue index = (BasicValue) collection.getIndex();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "list_owner_addresses" );
					assertThat( index.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "address_position" );
					assertThat( element.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				EmbeddableListOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testEmbeddableMapElementCollection(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( EmbeddableMapOwner.class.getName() );
					final Property property = entityBinding.getProperty( "addresses" );
					assertThat( property.getValue() ).isInstanceOf( org.hibernate.mapping.Map.class );
					final org.hibernate.mapping.Map collection = (org.hibernate.mapping.Map) property.getValue();
					final BasicValue key = (BasicValue) collection.getIndex();
					final Component element = (Component) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "map_owner_addresses" );
					assertThat( key.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "address_key" );
					assertThat( element.getComponentClassName() ).isEqualTo( Address.class.getName() );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode" );
				},
				scope.getRegistry(),
				EmbeddableMapOwner.class
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

	@Test
	@ServiceRegistry
	void testNestedEmbeddableElementCollectionAttributeOverride(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedEmbeddableElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "addresses" ).getValue();
					final Component element = (Component) collection.getElement();
					final Component location = (Component) element.getProperty( "location" ).getValue();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "nested_owner_addresses" );
					assertThat( location.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "home_city", "home_country" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "line1", "zipCode", "home_city", "home_country" );
				},
				scope.getRegistry(),
				NestedEmbeddableElementOwner.class
		);
	}

	@Test
	@ServiceRegistry
	void testNestedEmbeddableElementCollectionConvert(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( NestedConvertedEmbeddableElementOwner.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "addresses" ).getValue();
					final Component element = (Component) collection.getElement();
					final Component location = (Component) element.getProperty( "location" ).getValue();
					final BasicValue city = (BasicValue) location.getProperty( "city" ).getValue();
					final BasicValue country = (BasicValue) location.getProperty( "country" ).getValue();

					assertThat( city.getJpaAttributeConverterDescriptor() ).isNotNull();
					assertThat( country.getJpaAttributeConverterDescriptor() ).isNotNull();
				},
				scope.getRegistry(),
				NestedConvertedEmbeddableElementOwner.class
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

	@Entity(name="ListOwner")
	@Table(name="list_owners")
	public static class ListOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "list_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@OrderColumn(name = "position")
		@Column(name = "label")
		private List<String> labels;
	}

	@Entity(name="ImplicitListOwner")
	@Table(name="implicit_list_owners")
	public static class ImplicitListOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "implicit_list_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Column(name = "label")
		private List<String> labels;
	}

	@Entity(name="BagListOwner")
	@Table(name="bag_list_owners")
	public static class BagListOwner {
		@Id
		private Integer id;
		@ElementCollection
		@Bag
		@CollectionTable(
				name = "bag_list_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Column(name = "label")
		private List<String> labels;
	}

	@Entity(name="MapOwner")
	@Table(name="map_owners")
	public static class MapOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "map_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_key")
		@Column(name = "label")
		private Map<String, String> labels;
	}

	@Entity(name="ImplicitMapOwner")
	@Table(name="implicit_map_owners")
	public static class ImplicitMapOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "implicit_map_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Column(name = "label")
		private Map<String, String> labels;
	}

	@Entity(name="EnumMapKeyOwner")
	@Table(name="enum_map_key_owners")
	public static class EnumMapKeyOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "enum_map_key_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_kind")
		@MapKeyEnumerated(EnumType.STRING)
		@Column(name = "label")
		private Map<LabelKind, String> labels;
	}

	@Entity(name="MapKeyClassOwner")
	@Table(name="map_key_class_owners")
	public static class MapKeyClassOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "map_key_class_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_kind")
		@MapKeyClass(LabelKind.class)
		@MapKeyEnumerated(EnumType.STRING)
		@Column(name = "label")
		private Map<Object, String> labels;
	}

	@Entity(name="TemporalMapKeyOwner")
	@Table(name="temporal_map_key_owners")
	public static class TemporalMapKeyOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "temporal_map_key_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_date")
		@MapKeyTemporal(TemporalType.DATE)
		@Column(name = "label")
		private Map<Date, String> labels;
	}

	@Entity(name="ConvertedMapKeyOwner")
	@Table(name="converted_map_key_owners")
	public static class ConvertedMapKeyOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "converted_map_key_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "label_code")
		@Convert(attributeName = "key", converter = LabelCodeConverter.class)
		@Column(name = "label")
		private Map<LabelCode, String> labels;
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

	@Entity(name="EmbeddableListOwner")
	@Table(name="embeddable_list_owners")
	public static class EmbeddableListOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "list_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@OrderColumn(name = "address_position")
		private List<Address> addresses;
	}

	@Entity(name="EmbeddableMapOwner")
	@Table(name="embeddable_map_owners")
	public static class EmbeddableMapOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "map_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@MapKeyColumn(name = "address_key")
		private Map<String, Address> addresses;
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

	@Entity(name="NestedEmbeddableElementOwner")
	@Table(name="nested_embeddable_element_owners")
	public static class NestedEmbeddableElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "nested_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@AttributeOverride(name = "location.city", column = @Column(name = "home_city"))
		@AttributeOverride(name = "location.country", column = @Column(name = "home_country"))
		private Set<AddressWithLocation> addresses;
	}

	@Entity(name="NestedConvertedEmbeddableElementOwner")
	@Table(name="nested_converted_embeddable_element_owners")
	public static class NestedConvertedEmbeddableElementOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "nested_converted_owner_addresses",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
		)
		@Convert(attributeName = "location.city", converter = CityConverter.class)
		private Set<AddressWithConvertedLocation> addresses;
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

	@Embeddable
	public static class AddressWithLocation {
		private String line1;
		private String zipCode;
		private Location location;
	}

	@Embeddable
	public static class Location {
		private String city;
		private String country;
	}

	@Embeddable
	public static class AddressWithConvertedLocation {
		private String line1;
		private String zipCode;
		private ConvertedLocation location;
	}

	@Embeddable
	public static class ConvertedLocation {
		private String city;
		@Convert(converter = CountryConverter.class)
		private String country;
	}

	public enum LabelKind {
		PRIMARY,
		SECONDARY
	}

	public record LabelCode(String code) {
	}

	public static class LabelCodeConverter implements AttributeConverter<LabelCode, String> {
		@Override
		public String convertToDatabaseColumn(LabelCode attribute) {
			return attribute == null ? null : attribute.code();
		}

		@Override
		public LabelCode convertToEntityAttribute(String dbData) {
			return dbData == null ? null : new LabelCode( dbData );
		}
	}

	public static class CityConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData;
		}
	}

	public static class CountryConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData;
		}
	}
}
