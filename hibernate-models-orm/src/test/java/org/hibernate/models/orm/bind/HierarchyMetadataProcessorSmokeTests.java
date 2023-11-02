/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.models.orm.categorize.spi.AggregatedKeyMapping;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.BasicKeyMapping;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ManagedResourcesProcessor;
import org.hibernate.models.orm.categorize.spi.NonAggregatedKeyMapping;
import org.hibernate.models.orm.process.ManagedResourcesImpl;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class HierarchyMetadataProcessorSmokeTests {
	@Test
	void testSimpleId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( SimpleIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final BasicKeyMapping idMapping = (BasicKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNotNull();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		final BasicKeyMapping naturalIdMapping = (BasicKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( NaturalId.class ) ).isNotNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
	}

	@Test
	void testAggregatedId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( AggregatedIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final AggregatedKeyMapping idMapping = (AggregatedKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNull();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNotNull();

		assertThat( entityHierarchy.getNaturalIdMapping() ).isNotNull();
		assertThat( entityHierarchy.getNaturalIdMapping() ).isInstanceOf( AggregatedKeyMapping.class );
		final AggregatedKeyMapping naturalIdMapping = (AggregatedKeyMapping) entityHierarchy.getNaturalIdMapping();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();
		assertThat( naturalIdMapping.getAttribute().getMember().getAnnotationUsage( NaturalId.class ) ).isNotNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
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
		assertThat( entityHierarchy.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();

		assertThat( entityHierarchy.getCacheRegion() ).isNotNull();
		assertThat( entityHierarchy.getCacheRegion().getAccessType() ).isEqualTo( AccessType.TRANSACTIONAL );

		assertThat( entityHierarchy.getInheritanceType() ).isNotNull();
		assertThat( entityHierarchy.getInheritanceType() ).isEqualTo( InheritanceType.TABLE_PER_CLASS );
	}

	@Test
	void testHierarchy() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( HierarchyRoot.class, HierarchySuper.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final BasicKeyMapping idMapping = (BasicKeyMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNotNull();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();

		assertThat( entityHierarchy.getCacheRegion() ).isNotNull();
		assertThat( entityHierarchy.getCacheRegion().getAccessType() ).isEqualTo( AccessType.READ_ONLY );

		assertThat( entityHierarchy.getInheritanceType() ).isNotNull();
		assertThat( entityHierarchy.getInheritanceType() ).isEqualTo( InheritanceType.JOINED );
	}

	private static Set<EntityHierarchy> buildHierarchyMetadata(Class<?>... classes) {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addLoadedClasses(classes)
				.build();

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );

			final CategorizedDomainModel categorizedDomainModel = ManagedResourcesProcessor.processManagedResources(
					managedResources,
					bootstrapContext
			);

			return categorizedDomainModel.getEntityHierarchies();
		}
	}

	private static CategorizedDomainModel buildCategorizedDomainModel(Class<?>... classes) {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addLoadedClasses(classes)
				.build();

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );

			return ManagedResourcesProcessor.processManagedResources( managedResources, bootstrapContext );
		}
	}


	@Test
	void testSimpleEventListenerResolution() {
		final CategorizedDomainModel categorizedDomainModel = buildCategorizedDomainModel(
				HierarchyRoot.class,
				HierarchySuper.class
		);
		final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
		final EntityHierarchy hierarchy = entityHierarchies.iterator().next();

		final EntityTypeMetadata rootMapping = hierarchy.getRoot();
		assertThat( rootMapping.getHierarchyJpaEventListeners() ).hasSize( 3 );
		final List<String> listenerClassNames = rootMapping.getHierarchyJpaEventListeners()
				.stream()
				.map( listener -> listener.getCallbackClass().getClassName() )
				.collect( Collectors.toList() );
		assertThat( listenerClassNames ).containsExactly(
				Listener1.class.getName(),
				Listener2.class.getName(),
				HierarchyRoot.class.getName()
		);

		final IdentifiableTypeMetadata superMapping = rootMapping.getSuperType();
		assertThat( superMapping.getHierarchyJpaEventListeners() ).hasSize( 1 );
		final String callbackClassName = superMapping.getHierarchyJpaEventListeners()
				.get( 0 )
				.getCallbackClass()
				.getClassName();
		assertThat( callbackClassName ).isEqualTo( Listener1.class.getName() );
	}

}
