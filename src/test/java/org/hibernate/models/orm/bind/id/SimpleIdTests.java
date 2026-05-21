/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.id;

import java.util.Set;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.models.categorize.spi.AggregatedKeyMapping;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.NonAggregatedKeyMapping;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.bind.BindingTestingHelper.buildHierarchyMetadata;
import static org.hibernate.models.orm.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class SimpleIdTests {
	@Test
	void testSimpleId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( BasicIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final BasicKeyMapping idMapping = (BasicKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getDirectAnnotationUsage( Id.class ) ).isNotNull();
		assertThat( idMapping.getAttribute().getMember().getDirectAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		final BasicKeyMapping naturalIdMapping = (BasicKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( NaturalId.class ) ).isNotNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( Id.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getDirectAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getDirectAnnotationUsage( TenantId.class ) ).isNotNull();
	}

	@Test
	void testAggregatedId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( AggregatedIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final AggregatedKeyMapping idMapping = (AggregatedKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getDirectAnnotationUsage( Id.class ) ).isNull();
		assertThat( idMapping.getAttribute().getMember().getDirectAnnotationUsage( EmbeddedId.class ) ).isNotNull();

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		assertThat( entityHierarchy.getNaturalIdMapping() ).isInstanceOf( AggregatedKeyMapping.class );
		final AggregatedKeyMapping naturalIdMapping = (AggregatedKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( Id.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( EmbeddedId.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getDirectAnnotationUsage( NaturalId.class ) ).isNotNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getDirectAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getDirectAnnotationUsage( TenantId.class ) ).isNotNull();
	}

	@Test
	@ServiceRegistry
	void testAggregatedIdBinding(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
						final RootClass entityBinding = (RootClass) context.getMetadataCollector()
								.getEntityBinding( AggregatedIdOnlyEntity.class.getName() );
					assertThat( entityBinding.getIdentifier() ).isInstanceOf( Component.class );
					assertThat( entityBinding.hasEmbeddedIdentifier() ).isTrue();
					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
				},
				scope.getRegistry(),
				AggregatedIdOnlyEntity.class
		);
	}

	@Test
	void testNonAggregatedId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( NonAggregatedIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final NonAggregatedKeyMapping idMapping = (NonAggregatedKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getIdAttributes() ).hasSize( 2 );
		assertThat( idMapping.getIdAttributes().stream().map( AttributeMetadata::getName ) ).containsExactly( "id1", "id2" );
		assertThat( idMapping.getIdClassType().getClassName() ).isEqualTo( NonAggregatedIdEntity.Pk.class.getName() );

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		final NonAggregatedKeyMapping naturalIdMapping = (NonAggregatedKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getIdAttributes() ).hasSize( 2 );
		assertThat( naturalIdMapping.getIdAttributes().stream().map( AttributeMetadata::getName ) ).containsExactly( "naturalKey1", "naturalKey2" );

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getDirectAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getDirectAnnotationUsage( TenantId.class ) ).isNotNull();

		assertThat( entityHierarchy.getCacheRegion() ).isNotNull();
		assertThat( entityHierarchy.getCacheRegion().getAccessType() ).isEqualTo( AccessType.TRANSACTIONAL );

		assertThat( entityHierarchy.getInheritanceType() ).isNotNull();
		assertThat( entityHierarchy.getInheritanceType() ).isEqualTo( InheritanceType.TABLE_PER_CLASS );
	}

	@Test
	@ServiceRegistry
	void testNonAggregatedIdBinding(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( NonAggregatedIdEntity.class.getName() );
					assertThat( entityBinding.getIdentifier() ).isInstanceOf( Component.class );
					assertThat( entityBinding.hasEmbeddedIdentifier() ).isFalse();
					assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "id1", "id2" );
				},
				scope.getRegistry(),
				NonAggregatedIdEntity.class
		);
	}

	@Entity
	@Table(name = "agg_id_only_entities")
	public static class AggregatedIdOnlyEntity {
		@EmbeddedId
		private Pk id;

		@Embeddable
		public static class Pk {
			private Integer id1;
			private Integer id2;
		}
	}
}
