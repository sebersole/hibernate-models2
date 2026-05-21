/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.complete;

import java.util.Set;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;

import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;

import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;

import static org.assertj.core.api.Assertions.assertThat;

public class DiscriminatorValueTest {
	@Test
	void testDiscriminatorValue() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/complete/discriminator-value.xml" );
			final AvailableResources availableResources = AvailableResources.from(
					persistenceConfiguration,
					metadataBuildingContext
			);
			final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
					availableResources,
					metadataBuildingContext
			);

			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( entityHierarchies ).hasSize( 3 );

			for ( EntityHierarchy entityHierarchy : entityHierarchies ) {
				final EntityTypeMetadata root = entityHierarchy.getRoot();

				final String entityName = root.getClassDetails().getName();
				if ( entityName.equals( "org.hibernate.models.orm.xml.complete.Root" ) ) {

					final DiscriminatorValue rootDiscriminatorValueAnnotationUsage = root.getClassDetails()
							.getDirectAnnotationUsage( DiscriminatorValue.class );
					assertThat( rootDiscriminatorValueAnnotationUsage ).isNull();

					final DiscriminatorColumn discriminatorColumnAnnotationUsage = root.getClassDetails()
							.getDirectAnnotationUsage( DiscriminatorColumn.class );

					assertThat( discriminatorColumnAnnotationUsage ).isNotNull();

					final String discriminatorColumName = discriminatorColumnAnnotationUsage.name();
					assertThat( discriminatorColumName ).isEqualTo( "TYPE_COLUMN" );

					final DiscriminatorType discriminatorColumnType = discriminatorColumnAnnotationUsage.discriminatorType();
					assertThat( discriminatorColumnType ).isEqualTo( DiscriminatorType.INTEGER );

					final Iterable<IdentifiableTypeMetadata> subTypes = root.getSubTypes();
					assertThat( subTypes ).hasSize( 1 );

					final IdentifiableTypeMetadata subType = subTypes.iterator().next();
					final DiscriminatorValue subTypeDiscriminatorValueAnnotationUsage = subType.getClassDetails()
							.getDirectAnnotationUsage( DiscriminatorValue.class );
					assertThat( subTypeDiscriminatorValueAnnotationUsage ).isNotNull();
					String discriminatorValue = subTypeDiscriminatorValueAnnotationUsage.value();
					assertThat( discriminatorValue ).isEqualTo( "R" );

					final DiscriminatorFormula discriminatorFortmulaAnnotationUsage = root.getClassDetails()
							.getDirectAnnotationUsage( DiscriminatorFormula.class );
					assertThat( discriminatorFortmulaAnnotationUsage ).isNull();
				}
				else if ( entityName.equals( "org.hibernate.models.orm.xml.complete.SimplePerson" ) ) {
					final DiscriminatorValue rootDiscriminatorValueAnnotationUsage = root.getClassDetails()
							.getDirectAnnotationUsage( DiscriminatorValue.class );
					assertThat( rootDiscriminatorValueAnnotationUsage ).isNull();

					final DiscriminatorColumn discriminatorColumnAnnotationUsage = root.getClassDetails()
							.getDirectAnnotationUsage( DiscriminatorColumn.class );
					assertThat( discriminatorColumnAnnotationUsage ).isNotNull();

					final String discriminatorColumName = discriminatorColumnAnnotationUsage.name();
					assertThat( discriminatorColumName ).isEqualTo( "DTYPE" );

					final DiscriminatorType discriminatorColumnType = discriminatorColumnAnnotationUsage.discriminatorType();
					assertThat( discriminatorColumnType ).isEqualTo( DiscriminatorType.STRING );

					final DiscriminatorFormula discriminatorFortmulaAnnotationUsage = root.getClassDetails()
							.getDirectAnnotationUsage( DiscriminatorFormula.class );
					assertThat( discriminatorFortmulaAnnotationUsage ).isNull();
				}
				else {
					assertThat( entityName ).isEqualTo( "org.hibernate.models.orm.xml.SimpleEntity" );

					final DiscriminatorValue rootDiscriminatorValueAnnotationUsage = root.getClassDetails()
							.getDirectAnnotationUsage( DiscriminatorValue.class );
					assertThat( rootDiscriminatorValueAnnotationUsage ).isNull();

					final DiscriminatorColumn discriminatorColumnAnnotationUsage = root.getClassDetails()
							.getDirectAnnotationUsage( DiscriminatorColumn.class );
					assertThat( discriminatorColumnAnnotationUsage ).isNull();

					final DiscriminatorFormula discriminatorFortmulaAnnotationUsage = root.getClassDetails()
							.getDirectAnnotationUsage( DiscriminatorFormula.class );
					assertThat( discriminatorFortmulaAnnotationUsage ).isNotNull();

					final String formula = discriminatorFortmulaAnnotationUsage.value();
					assertThat( formula ).isEqualTo( "CASE WHEN VALUE1 IS NOT NULL THEN 1 WHEN VALUE2 IS NOT NULL THEN 2 END" );
				}
			}

		}
	}
}
