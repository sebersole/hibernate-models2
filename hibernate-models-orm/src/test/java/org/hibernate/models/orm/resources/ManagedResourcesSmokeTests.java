/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.resources;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.orm.bind.internal.BindingContextImpl;
import org.hibernate.models.orm.bind.internal.HierarchyMetadataProcessor;
import org.hibernate.models.orm.bind.spi.HierarchyMetadata;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ManagedResourcesProcessor;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.FieldDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.spi.PersistenceUnitInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.categorize.internal.EntityHierarchyBuilder.createEntityHierarchies;

/**
 * {@linkplain MetadataBuildingProcess#prepare} produces a {@linkplain ManagedResources} which
 * represents the complete set of classes and xml mappings.
 * <p>
 * The known XML mappings combine explicit mappings, plus any discovered during scanning.
 * <p>
 * The known classes combine explicit values, plus any discovered during scanning.  It also
 * already excludes classes as per JPA's {@linkplain PersistenceUnitInfo#excludeUnlistedClasses()}
 * handling.
 * <p/>
 * The "known classes" + all classes named in "known XML mappings" represents the complete
 * set of "managed classes" (JPA's term) which Hibernate will process for annotations -
 * {@code @Entity}, {@code @Converter}, ...
 * <p/>
 * And is the likely place that hibernate-models-source would hook in to.  Let's see how that might work...
 *
 * @author Steve Ebersole
 */
public class ManagedResourcesSmokeTests {
	@Test
	void testProcessor() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataSources metadataSources = new MetadataSources( serviceRegistry ).addAnnotatedClass( Person.class );
			final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );
			final ManagedResources managedResources = MetadataBuildingProcess.prepare( metadataSources, bootstrapContext );

			final CategorizedDomainModel categorizedDomainModel = ManagedResourcesProcessor.processManagedResources(
					managedResources,
					bootstrapContext
			);

			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( entityHierarchies ).hasSize( 1 );
			final EntityTypeMetadata entityDescriptor = entityHierarchies.iterator().next().getRoot();
			final ClassDetails entityClassDetails = entityDescriptor.getClassDetails();
			assertThat( entityClassDetails.getClassName() ).isEqualTo( Person.class.getName() );
			assertThat( entityDescriptor.getAttributes() ).hasSize( 2 );

			final Iterator<AttributeMetadata> attributes = entityDescriptor.getAttributes().iterator();
			final AttributeMetadata firstAttribute = attributes.next();
			final AttributeMetadata secondAttribute = attributes.next();

			assertThat( firstAttribute.getMember() ).isInstanceOf( FieldDetails.class );
			assertThat( secondAttribute.getMember() ).isInstanceOf( FieldDetails.class );



			final BindingContextImpl mappingBuildingContext = new BindingContextImpl( categorizedDomainModel, bootstrapContext );

			final List<HierarchyMetadata> hierarchyMetadata = HierarchyMetadataProcessor.preBindHierarchyAttributes(
					categorizedDomainModel,
					mappingBuildingContext
			);

			assertThat( hierarchyMetadata ).hasSize( 1 );
			assertThat( hierarchyMetadata.get( 0 ).getCollectedIdAttributes() ).isInstanceOf( AttributeMetadata.class );
			final AttributeMetadata idAttr = (AttributeMetadata) hierarchyMetadata.get( 0 ).getCollectedIdAttributes();
			assertThat( idAttr.getName() ).isEqualTo( "id" );
			assertThat( idAttr.getMember() ).isInstanceOf( FieldDetails.class );
		}
	}

	@Entity(name="Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Integer id;
		private String name;
	}
}
