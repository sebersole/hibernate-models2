package org.hibernate.models.orm.xml.dynamic;

import java.util.Set;

import org.hibernate.annotations.RowId;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.process.ManagedResourcesImpl;
import org.hibernate.models.spi.AnnotationUsage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

public class RowIdTest {
	@Test
	void testSimpleDynamicModel() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/dynamic/dynamic-rowid.xml" )
				.build();
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
					serviceRegistry,
					new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
			);
			final CategorizedDomainModel categorizedDomainModel = processManagedResources(
					managedResources,
					bootstrapContext
			);

			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( entityHierarchies ).hasSize( 3 );

			entityHierarchies.forEach(
					entityHierarchy -> {
						final EntityTypeMetadata root = entityHierarchy.getRoot();
						final AnnotationUsage<RowId> rowIdAnnotationUsage = root.getClassDetails().getAnnotationUsage(
								RowId.class );
						final String entityName = root.getEntityName();
						if ( entityName.equals( "EntityWithoutRowId" ) ) {
							assertThat( rowIdAnnotationUsage ).isNull();
						}
						else {
							assertThat( rowIdAnnotationUsage ).isNotNull();
							final String value = rowIdAnnotationUsage.getString( "value" );
							if ( entityName.equals( "EntityWithRowIdNoValue" ) ) {
								assertThat( value ).isNull();
							}
							else {
								assertThat( value ).isEqualTo( "ROW_ID" );
							}
						}
					}
			);
		}
	}
}
