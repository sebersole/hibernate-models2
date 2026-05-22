/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.collections;

import java.util.Set;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

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
					assertThat( collection.getKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "owner_id" );
					assertThat( element.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "label" );
					assertThat( collection.getCollectionTable().getForeignKeyCollection() ).hasSize( 1 );
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

	@Entity(name="SetOwner")
	@Table(name="set_owners")
	public static class SetOwner {
		@Id
		private Integer id;
		@ElementCollection
		@CollectionTable(
				name = "set_owner_labels",
				joinColumns = @JoinColumn(name = "owner_id", referencedColumnName = "id")
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

	@Embeddable
	public static class Pk {
		private Integer id1;
		private Integer id2;
	}
}
