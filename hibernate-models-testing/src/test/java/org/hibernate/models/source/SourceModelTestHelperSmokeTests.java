/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source;

import java.util.List;

import org.hibernate.models.source.internal.jandex.JandexClassDetails;
import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.FieldDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class SourceModelTestHelperSmokeTests {
	@Test
	void testIt() {
		final SourceModelBuildingContext buildingContext = SourceModelTestHelper.createBuildingContext( AnEntity.class );

		final AnnotationDescriptor<Entity> entityAnnDescriptor = buildingContext
				.getAnnotationDescriptorRegistry()
				.getDescriptor( Entity.class );
		assertThat( entityAnnDescriptor ).isNotNull();

		final ClassDetails classDetails = buildingContext
				.getClassDetailsRegistry()
				.findClassDetails( AnEntity.class.getName() );
		assertThat( classDetails ).isNotNull();
		assertThat( classDetails ).isInstanceOf( JandexClassDetails.class );

		final AnnotationUsage<Entity> entityAnnotation = classDetails.getAnnotationUsage( Entity.class );
		assertThat( entityAnnotation ).isNotNull();
		assertThat( entityAnnotation.<String>getAttributeValue( "name" ) ).isEqualTo( "AnEntity" );

		final AnnotationUsage<Table> tableAnnotation = classDetails.getAnnotationUsage( Table.class );
		assertThat( tableAnnotation ).isNotNull();
		assertThat( tableAnnotation.<String>getAttributeValue( "name" ) ).isEqualTo( "the_table" );

		final AnnotationUsage<Inheritance> inheritanceAnnotation = classDetails.getAnnotationUsage( Inheritance.class );
		assertThat( inheritanceAnnotation ).isNull();

		final FieldDetails idField = classDetails.findFieldByName( "id" );
		assertThat( idField ).isNotNull();
		final AnnotationUsage<Id> idAnnotation = idField.getAnnotationUsage( Id.class );
		assertThat( idAnnotation ).isNotNull();

		final FieldDetails nameField = classDetails.findFieldByName( "name" );
		assertThat( nameField ).isNotNull();
		final AnnotationUsage<Column> nameColumnAnnotation = nameField.getAnnotationUsage( Column.class );
		assertThat( nameColumnAnnotation ).isNotNull();

		try {
			classDetails.getAnnotationUsage( NamedQuery.class );
			fail( "Expecting failure" );
		}
		catch (AnnotationAccessException expected) {
		}

		final List<AnnotationUsage<NamedQuery>> repeatedUsages = classDetails.getRepeatedAnnotationUsages( NamedQuery.class );
		assertThat( repeatedUsages ).hasSize( 2 );

		final AnnotationUsage<NamedQuery> queryOne = classDetails.getNamedAnnotationUsage( NamedQuery.class, "one" );
		assertThat( queryOne ).isNotNull();

		final AnnotationUsage<NamedQuery> queryTwo = classDetails.getNamedAnnotationUsage( NamedQuery.class, "two", "name" );
		assertThat( queryTwo ).isNotNull();

		assertThat( classDetails.getRepeatedAnnotationUsages( NamedQuery.class ) ).hasSize( 2 );
	}

	@Entity(name="AnEntity")
	@Table(name="the_table")
	@NamedQuery(name = "one", query = "from AnEntity")
	@NamedQuery(name = "two", query = "from AnEntity")
	public static class AnEntity {
		@Id
		private Integer id;
		@Column(name="the_column")
		private String name;
	}
}
