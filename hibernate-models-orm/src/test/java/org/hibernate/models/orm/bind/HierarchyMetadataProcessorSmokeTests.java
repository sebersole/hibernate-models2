/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import java.util.List;
import java.util.Set;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.orm.bind.internal.AggregatedIdMappingImpl;
import org.hibernate.models.orm.bind.internal.BasicIdMapping;
import org.hibernate.models.orm.bind.internal.BindingContextImpl;
import org.hibernate.models.orm.bind.internal.HierarchyMetadataProcessor;
import org.hibernate.models.orm.bind.internal.NonAggregatedIdMappingImpl;
import org.hibernate.models.orm.bind.spi.HierarchyMetadata;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.ManagedResourcesProcessor;
import org.hibernate.models.orm.process.ManagedResourcesImpl;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class HierarchyMetadataProcessorSmokeTests {
	@Test
	void testSimpleId() {
		final List<HierarchyMetadata> hierarchyMetadataList = buildHierarchyMetadata( SimpleIdEntity.class );

		hierarchyMetadataList.forEach( (hierarchyMetadata) -> {
			final Object collectedIdAttributes = hierarchyMetadata.getCollectedIdAttributes();
			assertThat( collectedIdAttributes ).isInstanceOf( AttributeMetadata.class );
			final AttributeMetadata idAttribute = (AttributeMetadata) collectedIdAttributes;
			assertThat( idAttribute.getMember().getAnnotationUsage( Id.class ) ).isNotNull();
			assertThat( idAttribute.getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();

			assertThat( hierarchyMetadata.getIdMapping() ).isNotNull();
			assertThat( hierarchyMetadata.getIdMapping() ).isInstanceOf( BasicIdMapping.class );
			assertThat( ( (BasicIdMapping) hierarchyMetadata.getIdMapping() ).getAttribute().getName() ).isEqualTo( "id" );

			assertThat( hierarchyMetadata.getVersionAttribute() ).isNotNull();
			assertThat( hierarchyMetadata.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

			assertThat( hierarchyMetadata.getTenantIdAttribute() ).isNotNull();
			assertThat( hierarchyMetadata.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
		} );
	}

	@Test
	void testAggregatedId() {
		final List<HierarchyMetadata> hierarchyMetadataList = buildHierarchyMetadata( AggregatedIdEntity.class );

		hierarchyMetadataList.forEach( (hierarchyMetadata) -> {
			final Object collectedIdAttributes = hierarchyMetadata.getCollectedIdAttributes();
			assertThat( collectedIdAttributes ).isInstanceOf( AttributeMetadata.class );
			final AttributeMetadata idAttribute = (AttributeMetadata) collectedIdAttributes;
			assertThat( idAttribute.getMember().getAnnotationUsage( Id.class ) ).isNull();
			assertThat( idAttribute.getMember().getAnnotationUsage( EmbeddedId.class ) ).isNotNull();

			assertThat( hierarchyMetadata.getIdMapping() ).isNotNull();
			assertThat( hierarchyMetadata.getIdMapping() ).isInstanceOf( AggregatedIdMappingImpl.class );
			assertThat( ( (AggregatedIdMappingImpl) hierarchyMetadata.getIdMapping() ).getAttribute().getName() ).isEqualTo( "id" );

			assertThat( hierarchyMetadata.getVersionAttribute() ).isNotNull();
			assertThat( hierarchyMetadata.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

			assertThat( hierarchyMetadata.getTenantIdAttribute() ).isNotNull();
			assertThat( hierarchyMetadata.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
		} );
	}

	@Test
	void testNonAggregatedId() {
		final List<HierarchyMetadata> hierarchyMetadataList = buildHierarchyMetadata( NonAggregatedIdEntity.class );

		hierarchyMetadataList.forEach( (hierarchyMetadata) -> {
			final Object collectedIdAttributes = hierarchyMetadata.getCollectedIdAttributes();
			assertThat( collectedIdAttributes ).isInstanceOf( List.class );
			//noinspection unchecked
			final List<AttributeMetadata> idAttributes = (List<AttributeMetadata>) collectedIdAttributes;
			idAttributes.forEach( (idAttribute) -> {
				assertThat( idAttribute.getMember().getAnnotationUsage( Id.class ) ).isNotNull();
				assertThat( idAttribute.getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();
			} );

			assertThat( hierarchyMetadata.getIdMapping() ).isNotNull();
			assertThat( hierarchyMetadata.getIdMapping() ).isInstanceOf( NonAggregatedIdMappingImpl.class );

			assertThat( hierarchyMetadata.getVersionAttribute() ).isNotNull();
			assertThat( hierarchyMetadata.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

			assertThat( hierarchyMetadata.getTenantIdAttribute() ).isNotNull();
			assertThat( hierarchyMetadata.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
		} );
	}

	private static List<HierarchyMetadata> buildHierarchyMetadata(Class<?>... classes) {
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

			final BindingContextImpl ormModelBuildingContext = new BindingContextImpl(
					categorizedDomainModel.getClassDetailsRegistry(),
					categorizedDomainModel.getAnnotationDescriptorRegistry(),
					bootstrapContext.getClassmateContext()
			);
			final List<HierarchyMetadata> hierarchyMetadata = HierarchyMetadataProcessor.preBindHierarchyAttributes(
					categorizedDomainModel,
					ormModelBuildingContext
			);

			// at this point, `hierarchyAttributeDescriptors` contains one entry per hierarchy in the same iteration order
			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( hierarchyMetadata.size() ).isEqualTo( entityHierarchies.size() );
			return hierarchyMetadata;
		}
	}

}
