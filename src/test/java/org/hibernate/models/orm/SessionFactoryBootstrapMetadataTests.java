/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.orm.bind.GlobalRegistrationBindingTests;
import org.hibernate.models.orm.bind.SimpleEntity;
import org.hibernate.models.orm.bind.callbacks.HierarchyRoot;
import org.hibernate.models.orm.bind.callbacks.HierarchySuper;
import org.hibernate.models.orm.bind.collections.ElementCollectionBindingTests;
import org.hibernate.models.orm.bind.embeddable.EmbeddableBindingTests;
import org.hibernate.models.orm.bind.joined.SimpleJoinedTests;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class SessionFactoryBootstrapMetadataTests {
	@Test
	void buildsSecondaryTableMetadata(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = buildMetadata( registryScope, SimpleEntity.class );

		final RootClass entityBinding = (RootClass) metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertThat( entityBinding ).isNotNull();
		assertThat( entityBinding.getTable() ).isNotNull();
		assertThat( entityBinding.getJoins() ).hasSize( 1 );
		assertThat( entityBinding.getProperty( "data" ).getValue().getTable() )
				.isSameAs( entityBinding.getJoins().get( 0 ).getTable() );
	}

	@Test
	void buildsJoinedInheritanceMetadata(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = buildMetadata(
				registryScope,
				SimpleJoinedTests.Root.class,
				SimpleJoinedTests.Sub.class
		);

		final RootClass rootBinding = (RootClass) metadata.getEntityBinding( SimpleJoinedTests.Root.class.getName() );
		final JoinedSubclass subBinding = (JoinedSubclass) metadata.getEntityBinding( SimpleJoinedTests.Sub.class.getName() );
		assertThat( rootBinding ).isNotNull();
		assertThat( subBinding ).isNotNull();
		assertThat( subBinding.getSuperclass() ).isSameAs( rootBinding );
		assertThat( subBinding.getTable() ).isNotSameAs( rootBinding.getTable() );
		assertThat( rootBinding.getTable().getPrimaryKey() ).isNotNull();
		assertThat( subBinding.getTable().getPrimaryKey() ).isNotNull();
	}

	@Test
	void buildsCompositeEmbeddedIdMetadata(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = buildMetadata(
				registryScope,
				SimpleJoinedTests.CompositeRoot.class,
				SimpleJoinedTests.CompositeSub.class
		);

		final RootClass rootBinding = (RootClass) metadata.getEntityBinding( SimpleJoinedTests.CompositeRoot.class.getName() );
		final JoinedSubclass subBinding = (JoinedSubclass) metadata.getEntityBinding( SimpleJoinedTests.CompositeSub.class.getName() );
		assertThat( rootBinding ).isNotNull();
		assertThat( subBinding ).isNotNull();
		assertThat( rootBinding.getTable().getPrimaryKey().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "id1", "id2" );
		assertThat( subBinding.getKey().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "id1", "id2" );
		assertThat( subBinding.getTable().getPrimaryKey().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "id1", "id2" );
	}

	@Test
	void buildsCompositeIdClassMetadata(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = buildMetadata(
				registryScope,
				SimpleJoinedTests.IdClassRoot.class,
				SimpleJoinedTests.IdClassSub.class
		);

		final RootClass rootBinding = (RootClass) metadata.getEntityBinding( SimpleJoinedTests.IdClassRoot.class.getName() );
		final JoinedSubclass subBinding = (JoinedSubclass) metadata.getEntityBinding( SimpleJoinedTests.IdClassSub.class.getName() );
		assertThat( rootBinding ).isNotNull();
		assertThat( subBinding ).isNotNull();
		assertThat( rootBinding.getTable().getPrimaryKey().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "id1", "id2" );
		assertThat( subBinding.getKey().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "id1", "id2" );
		assertThat( subBinding.getTable().getPrimaryKey().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "id1", "id2" );
	}

	@Test
	void buildsMappedSuperclassMetadata(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = buildMetadata(
				registryScope,
				HierarchyRoot.class,
				HierarchySuper.class
		);

		final PersistentClass rootBinding = metadata.getEntityBinding( HierarchyRoot.class.getName() );
		assertThat( rootBinding ).isNotNull();
		assertThat( rootBinding.getSuperMappedSuperclass() ).isNotNull();
		assertThat( rootBinding.getIdentifierProperty() ).isNotNull();
		assertThat( rootBinding.getTable() ).isNotNull();
	}

	@Test
	void buildsElementCollectionMetadata(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = buildMetadata(
				registryScope,
				ElementCollectionBindingTests.SetOwner.class
		);

		final PersistentClass ownerBinding = metadata.getEntityBinding( ElementCollectionBindingTests.SetOwner.class.getName() );
		final Collection collection = (Collection) ownerBinding.getProperty( "labels" ).getValue();
		assertThat( collection ).isNotNull();
		assertThat( metadata.getCollectionBinding( collection.getRole() ) ).isSameAs( collection );
		assertThat( collection.getCollectionTable() ).isNotNull();
		assertThat( collection.getKey() ).isNotNull();
		assertThat( collection.getElement() ).isInstanceOf( BasicValue.class );
	}

	@Test
	void buildsNestedEmbeddedComponentMetadata(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = buildMetadata(
				registryScope,
				EmbeddableBindingTests.NestedEmbeddedEntity.class
		);

		final PersistentClass entityBinding = metadata.getEntityBinding( EmbeddableBindingTests.NestedEmbeddedEntity.class.getName() );
		final Component address = (Component) entityBinding.getProperty( "address" ).getValue();
		final Component location = (Component) address.getProperty( "location" ).getValue();
		assertThat( address.getProperties() )
				.extracting( org.hibernate.mapping.Property::getName )
				.containsExactly( "line1", "zipCode", "location" );
		assertThat( location.getComponentClassName() )
				.isEqualTo( EmbeddableBindingTests.Location.class.getName() );
		assertThat( address.getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "line1", "zipCode", "city", "country" );
	}

	@Test
	void buildsEmbeddableElementCollectionMetadata(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = buildMetadata(
				registryScope,
				ElementCollectionBindingTests.EmbeddableElementOwner.class
		);

		final PersistentClass ownerBinding = metadata.getEntityBinding( ElementCollectionBindingTests.EmbeddableElementOwner.class.getName() );
		final Collection collection = (Collection) ownerBinding.getProperty( "addresses" ).getValue();
		final Component element = (Component) collection.getElement();
		assertThat( metadata.getCollectionBinding( collection.getRole() ) ).isSameAs( collection );
		assertThat( collection.getCollectionTable().getName() ).isEqualTo( "owner_addresses" );
		assertThat( collection.getKey().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "owner_id" );
		assertThat( element.getComponentClassName() )
				.isEqualTo( ElementCollectionBindingTests.Address.class.getName() );
		assertThat( element.getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "line1", "zipCode" );
	}

	@Test
	void buildsXmlCompleteMetadata(ServiceRegistryScope registryScope) {
		final var metadataBuildingContext = new MetadataBuildingContextTestingImpl( registryScope.getRegistry() );
		final var persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		persistenceConfiguration.mappingFile( "mappings/complete/simple-complete.xml" );

		final MetadataImplementor metadata = TestBootModelProducer.buildMetadata(
				metadataBuildingContext,
				persistenceConfiguration
		);

		final PersistentClass entityBinding = metadata.getEntityBinding( org.hibernate.models.orm.xml.SimpleEntity.class.getName() );
		assertThat( entityBinding ).isNotNull();
		assertThat( entityBinding.getIdentifierProperty().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "pk" );
		assertThat( entityBinding.getProperty( "name" ).getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "description" );
	}

	@Test
	void buildsGlobalRegistrationMetadata(ServiceRegistryScope registryScope) {
		final MetadataImplementor metadata = buildMetadata(
				registryScope,
				GlobalRegistrationBindingTests.GlobalRegistrationEntity.class
		);

		assertThat( metadata.getEntityBinding( GlobalRegistrationBindingTests.GlobalRegistrationEntity.class.getName() ) )
				.isNotNull();
		assertThat( metadata.getNamedHqlQueryMapping( "globalJpaQuery" ).getHqlString() )
				.isEqualTo( "from GlobalRegistrationEntity" );
		assertThat( metadata.getNamedNativeQueryMapping( "globalNativeQuery" ).getSqlQueryString() )
				.isEqualTo( "select * from global_registration_entities" );
		assertThat( metadata.getNamedEntityGraph( "globalGraph" ).entityName() )
				.isEqualTo( "GlobalRegistrationEntity" );
	}

	private static MetadataImplementor buildMetadata(ServiceRegistryScope registryScope, Class<?>... domainClasses) {
		return TestBootModelProducer.buildMetadata(
				new MetadataBuildingContextTestingImpl( registryScope.getRegistry() ),
				domainClasses
		);
	}
}
