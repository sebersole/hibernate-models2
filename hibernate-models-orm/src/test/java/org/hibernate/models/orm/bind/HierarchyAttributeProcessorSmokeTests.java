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
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.models.orm.bind.internal.HierarchyAttributeProcessor;
import org.hibernate.models.orm.bind.internal.HierarchyAttributeProcessor.HierarchyAttributeDescriptor;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.models.orm.resources.ManagedResourcesSmokeTests;
import org.hibernate.models.orm.spi.AttributeMetadata;
import org.hibernate.models.orm.spi.CategorizedDomainModel;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.orm.spi.ManagedResourcesProcessor;
import org.hibernate.models.orm.util.OrmModelBuildingContextTesting;
import org.hibernate.models.source.SourceModelTestHelper;
import org.hibernate.models.source.internal.SourceModelBuildingContextImpl;

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * @author Steve Ebersole
 */
public class HierarchyAttributeProcessorSmokeTests {
	@Test
	void testSimpleId() {
		final List<HierarchyAttributeDescriptor> hierarchyAttributeDescriptors = buildHierarchyAttributes( SimpleIdEntity.class );

		hierarchyAttributeDescriptors.forEach( (attributes) -> {
			final Object collectedIdAttributes = attributes.getCollectedIdAttributes();
			assertThat( collectedIdAttributes ).isInstanceOf( AttributeMetadata.class );
			final AttributeMetadata idAttribute = (AttributeMetadata) collectedIdAttributes;
			assertThat( idAttribute.getMember().getAnnotationUsage( Id.class ) ).isNotNull();
			assertThat( idAttribute.getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();

			assertThat( attributes.getVersionAttribute() ).isNotNull();
			assertThat( attributes.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

			assertThat( attributes.getTenantIdAttribute() ).isNotNull();
			assertThat( attributes.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
		} );
	}

	@Test
	void testAggregatedId() {
		final List<HierarchyAttributeDescriptor> hierarchyAttributeDescriptors = buildHierarchyAttributes( AggregatedIdEntity.class );

		hierarchyAttributeDescriptors.forEach( (attributes) -> {
			final Object collectedIdAttributes = attributes.getCollectedIdAttributes();
			assertThat( collectedIdAttributes ).isInstanceOf( AttributeMetadata.class );
			final AttributeMetadata idAttribute = (AttributeMetadata) collectedIdAttributes;
			assertThat( idAttribute.getMember().getAnnotationUsage( Id.class ) ).isNull();
			assertThat( idAttribute.getMember().getAnnotationUsage( EmbeddedId.class ) ).isNotNull();

			assertThat( attributes.getVersionAttribute() ).isNotNull();
			assertThat( attributes.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

			assertThat( attributes.getTenantIdAttribute() ).isNotNull();
			assertThat( attributes.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
		} );
	}

	@Test
	void testNonAggregatedId() {
		final List<HierarchyAttributeDescriptor> hierarchyAttributeDescriptors = buildHierarchyAttributes( NonAggregatedIdEntity.class );

		hierarchyAttributeDescriptors.forEach( (attributes) -> {
			final Object collectedIdAttributes = attributes.getCollectedIdAttributes();
			assertThat( collectedIdAttributes ).isInstanceOf( List.class );
			//noinspection unchecked
			final List<AttributeMetadata> idAttributes = (List<AttributeMetadata>) collectedIdAttributes;
			idAttributes.forEach( (idAttribute) -> {
				assertThat( idAttribute.getMember().getAnnotationUsage( Id.class ) ).isNotNull();
				assertThat( idAttribute.getMember().getAnnotationUsage( EmbeddedId.class ) ).isNull();
			} );

			assertThat( attributes.getVersionAttribute() ).isNotNull();
			assertThat( attributes.getVersionAttribute().getMember().getAnnotationUsage( Version.class ) ).isNotNull();

			assertThat( attributes.getTenantIdAttribute() ).isNotNull();
			assertThat( attributes.getTenantIdAttribute().getMember().getAnnotationUsage( TenantId.class ) ).isNotNull();
		} );
	}

	private static List<HierarchyAttributeDescriptor> buildHierarchyAttributes(Class<?>... classes) {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addLoadedClasses(classes)
				.build();

		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(classes);
		final SourceModelBuildingContextImpl buildingContext = SourceModelTestHelper.createBuildingContext(
				jandexIndex,
				SIMPLE_CLASS_LOADING
		);

		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataSources metadataSources = new MetadataSources( serviceRegistry ).addAnnotatedClass( ManagedResourcesSmokeTests.Person.class );
			final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );

			final CategorizedDomainModel categorizedDomainModel = ManagedResourcesProcessor.processManagedResources(
					managedResources,
					bootstrapContext
			);

			final OrmModelBuildingContextTesting ormModelBuildingContext = new OrmModelBuildingContextTesting( buildingContext );
			final List<HierarchyAttributeDescriptor> hierarchyAttributeDescriptors = HierarchyAttributeProcessor.preBindHierarchyAttributes(
					categorizedDomainModel,
					ormModelBuildingContext
			);

			// at this point, `hierarchyAttributeDescriptors` contains one entry per hierarchy in the same iteration order
			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( hierarchyAttributeDescriptors.size() ).isEqualTo( entityHierarchies.size() );
			return hierarchyAttributeDescriptors;
		}
	}

}
