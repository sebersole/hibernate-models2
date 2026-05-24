/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.attr;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.models.spi.FieldDetails;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class ManyToOneTests {
	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testSimpleManyToOne(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
		final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
		persistenceConfiguration.mappingFile( "mappings/attr/many-to-one/simple.xml" );
		final AvailableResources availableResources = AvailableResources.from(
				persistenceConfiguration,
				metadataBuildingContext
		);

		final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
				availableResources,
				metadataBuildingContext
		);

		assertThat( categorizedDomainModel.getEntityHierarchies() ).hasSize( 1 );

		final EntityHierarchy hierarchy = categorizedDomainModel.getEntityHierarchies().iterator().next();
		final EntityTypeMetadata root = hierarchy.getRoot();

		final FieldDetails parentField = root.getClassDetails().findFieldByName( "parent" );
		final ManyToOne manyToOneAnn = parentField.getDirectAnnotationUsage( ManyToOne.class );
		assertThat( manyToOneAnn ).isNotNull();
		final JoinColumnsOrFormulas joinColumnsOrFormulas = parentField.getDirectAnnotationUsage( JoinColumnsOrFormulas.class );
		assertThat( joinColumnsOrFormulas ).isNotNull();
		assertThat( joinColumnsOrFormulas.value() ).hasSize( 1 );
		final JoinColumn joinColumnAnn = joinColumnsOrFormulas.value()[0].column();
		assertThat( joinColumnAnn.name() ).isEqualTo( "parent_fk" );

		final NotFound notFoundAnn = parentField.getDirectAnnotationUsage( NotFound.class );
		assertThat( notFoundAnn ).isNotNull();
		assertThat( notFoundAnn.action() ).isEqualTo( NotFoundAction.IGNORE );

		final OnDelete onDeleteAnn = parentField.getDirectAnnotationUsage( OnDelete.class );
		assertThat( onDeleteAnn ).isNotNull();
		assertThat( onDeleteAnn.action() ).isEqualTo( OnDeleteAction.CASCADE );

		final Fetch fetchAnn = parentField.getDirectAnnotationUsage( Fetch.class );
		assertThat( fetchAnn ).isNotNull();
		assertThat( fetchAnn.value() ).isEqualTo( FetchMode.SELECT );

		final OptimisticLock optLockAnn = parentField.getDirectAnnotationUsage( OptimisticLock.class );
		assertThat( optLockAnn ).isNotNull();
		assertThat( optLockAnn.excluded() ).isTrue();

		final Target targetAnn = parentField.getDirectAnnotationUsage( Target.class );
		assertThat( targetAnn ).isNotNull();
		assertThat( targetAnn.value() ).isEqualTo( "org.hibernate.models.orm.xml.attr.ManyToOneTests$SimpleEntity" );

		final ManyToOne cascadeAnn = parentField.getDirectAnnotationUsage( ManyToOne.class );
		final CascadeType[] cascadeTypes = cascadeAnn.cascade();
		assertThat( cascadeTypes ).isNotEmpty();
		assertThat( cascadeTypes ).containsOnly( CascadeType.ALL );
	}

	@SuppressWarnings("unused")
	@Entity(name="SimpleEntity")
	@Table(name="SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;
		private String name;
		private SimpleEntity parent;
	}
}
