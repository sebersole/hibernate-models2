/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SourceModelTestHelperSmokeTests {
	@Test
	void testIt() {
		final ModelsContext buildingContext = SourceModelTestHelper.createBuildingContext( AnEntity.class );

		final AnnotationDescriptor<Entity> entityAnnDescriptor = buildingContext
				.getAnnotationDescriptorRegistry()
				.getDescriptor( Entity.class );
		assertThat( entityAnnDescriptor ).isNotNull();

		final ClassDetails classDetails = buildingContext
				.getClassDetailsRegistry()
				.findClassDetails( AnEntity.class.getName() );
		assertThat( classDetails ).isNotNull();

		final Entity entityAnnotation = classDetails.getDirectAnnotationUsage( Entity.class );
		assertThat( entityAnnotation ).isNotNull();
		assertThat( entityAnnotation.name() ).isEqualTo( "AnEntity" );

		final Table tableAnnotation = classDetails.getDirectAnnotationUsage( Table.class );
		assertThat( tableAnnotation ).isNotNull();
		assertThat( tableAnnotation.name() ).isEqualTo( "the_table" );

		final Inheritance inheritanceAnnotation = classDetails.getDirectAnnotationUsage( Inheritance.class );
		assertThat( inheritanceAnnotation ).isNull();

		final FieldDetails idField = classDetails.findFieldByName( "id" );
		assertThat( idField ).isNotNull();
		assertThat( idField.getDirectAnnotationUsage( Id.class ) ).isNotNull();

		final FieldDetails nameField = classDetails.findFieldByName( "name" );
		assertThat( nameField ).isNotNull();
		final Column nameColumnAnnotation = nameField.getDirectAnnotationUsage( Column.class );
		assertThat( nameColumnAnnotation ).isNotNull();

		final NamedQuery[] repeatedUsages = classDetails.getRepeatedAnnotationUsages( NamedQuery.class, buildingContext );
		assertThat( repeatedUsages ).hasSize( 2 );

		final NamedQuery queryOne = classDetails.getNamedAnnotationUsage( NamedQuery.class, "one", buildingContext );
		assertThat( queryOne ).isNotNull();

		final NamedQuery queryTwo = classDetails.getNamedAnnotationUsage( NamedQuery.class, "two", "name", buildingContext );
		assertThat( queryTwo ).isNotNull();
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
