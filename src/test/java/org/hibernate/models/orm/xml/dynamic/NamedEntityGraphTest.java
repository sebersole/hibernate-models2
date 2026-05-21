/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.dynamic;

import java.util.Set;

import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;

import org.junit.jupiter.api.Test;

import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

import static org.assertj.core.api.Assertions.assertThat;

public class NamedEntityGraphTest {
	@Test
	void testNamedEntityGraph() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/dynamic/dynamic-named-entity-graph.xml" );
			final AvailableResources availableResources = AvailableResources.from(
					persistenceConfiguration,
					metadataBuildingContext
			);
			final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
					availableResources,
					metadataBuildingContext
			);

			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( entityHierarchies ).hasSize( 1 );

			entityHierarchies.forEach(
					entityHierarchy -> {
						final EntityTypeMetadata root = entityHierarchy.getRoot();
						final String entityName = root.getEntityName();

						final NamedEntityGraph[] namedEntityGraphAnnotationUsages = root.getClassDetails()
								.getRepeatedAnnotationUsages(
										NamedEntityGraph.class,
										metadataBuildingContext.getBootstrapContext().getModelsContext()
								);

						if ( entityName.equals( "Address" ) ) {
							assertThat( namedEntityGraphAnnotationUsages ).isEmpty();
						}
						else {
							assertThat( namedEntityGraphAnnotationUsages ).hasSize( 1 );
							final NamedEntityGraph namedEntityGraphAnnotationUsage = namedEntityGraphAnnotationUsages[0];
							assertThat( namedEntityGraphAnnotationUsage ).isNotNull();

							final String graphName = namedEntityGraphAnnotationUsage.name();
							assertThat( graphName ).isEqualTo( "employee" );

							assertThat( namedEntityGraphAnnotationUsage.includeAllAttributes() ).isTrue();

							NamedAttributeNode[] namedAttributeNodeUsage = namedEntityGraphAnnotationUsage.attributeNodes();
							assertThat( namedAttributeNodeUsage ).hasSize( 2 );

							// check NamedEntityGraph attributeNodes

							NamedAttributeNode firstAttributeNode = namedAttributeNodeUsage[0];
							checkAttributeNode( firstAttributeNode, "name", "", "" );

							NamedAttributeNode secondAttributeNode = namedAttributeNodeUsage[1];
							checkAttributeNode( secondAttributeNode, "address", "employee.address", "" );

							// check NamedEntityGraph subgraphs
							final NamedSubgraph[] subgraphUsages = namedEntityGraphAnnotationUsage.subgraphs();
							assertThat( subgraphUsages ).hasSize( 2 );

							NamedSubgraph firstSubgraph = subgraphUsages[0];
							assertThat( firstSubgraph.name() ).isEqualTo( "first.subgraph" );
							assertThat( firstSubgraph.type() ).isEqualTo( void.class );

							// check first NamedSubgraph attributeNodes

							namedAttributeNodeUsage = firstSubgraph.attributeNodes();
							assertThat( namedAttributeNodeUsage ).hasSize( 1 );

							checkAttributeNode( namedAttributeNodeUsage[0], "city", "", "" );

							NamedSubgraph secondSubgraph = subgraphUsages[1];
							assertThat( secondSubgraph.name() ).isEqualTo( "second.subgraph" );
							assertThat( secondSubgraph.type() ).isEqualTo( String.class );

							namedAttributeNodeUsage = secondSubgraph.attributeNodes();
							assertThat( namedAttributeNodeUsage ).hasSize( 3 );

							// check second NamedSubgraph attributeNodes
							checkAttributeNode( namedAttributeNodeUsage[0], "city", "sub1", "" );
							checkAttributeNode( namedAttributeNodeUsage[1], "name", "sub", "" );
							checkAttributeNode( namedAttributeNodeUsage[2], "surname", "", "" );


							assertThat( namedEntityGraphAnnotationUsage.subclassSubgraphs() ).isEmpty();

						}
					}
			);
		}
	}

	private static void checkAttributeNode(
			NamedAttributeNode firstAttributeNode,
			String expectedValueName,
			String expectedSubgraph,
			String expectedKeySubgraph) {
		assertThat( firstAttributeNode.value() ).isEqualTo( expectedValueName );
		assertThat( firstAttributeNode.subgraph() ).isEqualTo( expectedSubgraph );
		assertThat( firstAttributeNode.keySubgraph() ).isEqualTo( expectedKeySubgraph );
	}
}
