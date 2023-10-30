/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.categorize.spi.AggregatedIdMapping;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.BasicIdMapping;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.GlobalRegistrations;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.JpaEventListener;
import org.hibernate.models.orm.categorize.spi.JpaEventListenerStyle;
import org.hibernate.models.orm.categorize.spi.ManagedResourcesProcessor;
import org.hibernate.models.orm.categorize.spi.NonAggregatedIdMapping;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.Id;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SharedCacheMode;
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
		final BasicIdMapping idMapping = (BasicIdMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNotNull();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();

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
		final AggregatedIdMapping idMapping = (AggregatedIdMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( Id.class ) ).isNull();
		assertThat( idMapping.getAttribute().getMember().getAnnotationUsage( EmbeddedId.class ) ).isNotNull();

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
	}

	@Test
	void testHierarchy() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( HierarchyRoot.class, HierarchySuper.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final BasicIdMapping idMapping = (BasicIdMapping) entityHierarchy.getIdMapping();
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

	@Test
	void testNonAggregatedId() {
		final Set<EntityHierarchy> entityHierarchies = buildHierarchyMetadata( NonAggregatedIdEntity.class );
		assertThat( entityHierarchies ).hasSize( 1 );
		final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();

		assertThat( entityHierarchy.getIdMapping() ).isNotNull();
		final NonAggregatedIdMapping idMapping = (NonAggregatedIdMapping) entityHierarchy.getIdMapping();
		assertThat( idMapping.getIdAttributes() ).hasSize( 2 );
		assertThat( idMapping.getIdAttributes().stream().map( AttributeMetadata::getName ) ).containsExactly( "id1", "id2" );
		assertThat( idMapping.getIdClassType().getClassName() ).isEqualTo( NonAggregatedIdEntity.Pk.class.getName() );

		assertThat( entityHierarchy.getVersionAttribute() ).isNotNull();
		assertThat( entityHierarchy.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

		assertThat( entityHierarchy.getTenantIdAttribute() ).isNotNull();
		assertThat( entityHierarchy.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();

		assertThat( entityHierarchy.getCacheRegion() ).isNotNull();
		assertThat( entityHierarchy.getCacheRegion().getAccessType() ).isEqualTo( AccessType.TRANSACTIONAL );

		assertThat( entityHierarchy.getInheritanceType() ).isNotNull();
		assertThat( entityHierarchy.getInheritanceType() ).isEqualTo( InheritanceType.TABLE_PER_CLASS );
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

		final BindingContext bindingContext = new BindingContext() {
			@Override
			public ClassDetailsRegistry getClassDetailsRegistry() {
				return categorizedDomainModel.getClassDetailsRegistry();
			}

			@Override
			public AnnotationDescriptorRegistry getAnnotationDescriptorRegistry() {
				return categorizedDomainModel.getAnnotationDescriptorRegistry();
			}

			@Override
			public GlobalRegistrations getGlobalRegistrations() {
				return categorizedDomainModel.getGlobalRegistrations();
			}

			@Override
			public ClassmateContext getClassmateContext() {
				return null;
			}

			@Override
			public SharedCacheMode getSharedCacheMode() {
				return null;
			}
		};

		final List<JpaEventListener> rootJpaEventListeners = resolveJpaEventListenerList(
				hierarchy.getRoot(),
				categorizedDomainModel.getGlobalRegistrations(),
				bindingContext
		);
		assertThat( rootJpaEventListeners ).hasSize( 2 );


		final List<JpaEventListener> superJpaEventListeners = resolveJpaEventListenerList(
				hierarchy.getRoot().getSuperType(),
				categorizedDomainModel.getGlobalRegistrations(),
				bindingContext
		);
		assertThat( superJpaEventListeners ).hasSize( 1 );
	}

	private List<JpaEventListener> resolveJpaEventListenerList(
			IdentifiableTypeMetadata typeMetadata,
			GlobalRegistrations globalRegistrations,
			BindingContext bindingContext) {
		final List<JpaEventListener> result;
		if ( typeMetadata.getClassDetails().getAnnotationUsage( ExcludeDefaultListeners.class ) == null ) {
			result = new ArrayList<>( globalRegistrations.getEntityListenerRegistrations() );
		}
		else {
			result = new ArrayList<>();
		}

		collectListeners( typeMetadata, result, bindingContext );

		return result;
	}

	private void collectListeners(
			IdentifiableTypeMetadata typeMetadata,
			List<JpaEventListener> result,
			BindingContext bindingContext) {
		if ( typeMetadata.getClassDetails().getAnnotationUsage( ExcludeSuperclassListeners.class ) == null ) {
			if ( typeMetadata.getSuperType() != null ) {
				// ideally these would be cached...
				collectListeners( typeMetadata.getSuperType(), result, bindingContext );
			}
		}

		final AnnotationUsage<EntityListeners> localListenersAnnotation = typeMetadata.getClassDetails().getAnnotationUsage( EntityListeners.class );
		final List<ClassDetails> localListenersDetails = localListenersAnnotation.getAttributeValue( "value" );
		localListenersDetails.forEach( (localListenerDetails) -> {
			final JpaEventListener jpaEventListener = JpaEventListener.from( JpaEventListenerStyle.LISTENER, localListenerDetails );
			result.add( jpaEventListener );
		} );

		final JpaEventListener jpaEventListener = JpaEventListener.tryAsCallback( typeMetadata.getClassDetails() );
		if ( jpaEventListener != null ) {
			result.add( jpaEventListener );
		}
	}


}
