/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.util.function.Supplier;

import org.hibernate.mapping.Column;
import org.hibernate.models.orm.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.source.spi.AnnotationUsage;

import static org.hibernate.internal.util.NullnessHelper.nullif;

/**
 * @author Steve Ebersole
 */
public class ColumnBinder {
	public static Column bindColumn(
			AnnotationUsage<?> annotationUsage,
			Supplier<String> defaultNameSupplier,
			ModelCategorizationContext modelBuildingContext) {
		return bindColumn(
				annotationUsage,
				defaultNameSupplier,
				false,
				true,
				255,
				0,
				0,
				modelBuildingContext
		);
	}

	public static Column bindColumn(
			AnnotationUsage<?> annotationUsage,
			Supplier<String> defaultNameSupplier,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			long lengthByDefault,
			int precisionByDefault,
			int scaleByDefault,
			ModelCategorizationContext modelBuildingContext) {
		final Column result = new Column();
		result.setName( columnName( annotationUsage, defaultNameSupplier, modelBuildingContext ) );

		result.setUnique( annotationUsage.getBoolean( "unique", uniqueByDefault ) );
		result.setNullable( annotationUsage.getBoolean( "nullable", nullableByDefault ) );
		result.setSqlType( annotationUsage.getString( "columnDefinition" ) );
		result.setLength( annotationUsage.getLong( "length", lengthByDefault ) );
		result.setPrecision( annotationUsage.getInteger( "precision", precisionByDefault ) );
		result.setScale( annotationUsage.getInteger( "scale", scaleByDefault ) );
		return result;
	}


	private static String columnName(
			AnnotationUsage<?> columnAnnotation,
			Supplier<String> defaultNameSupplier,
			ModelCategorizationContext modelBuildingContext) {
		return nullif( columnAnnotation.getAttributeValue( "name" ), defaultNameSupplier );
	}

	private ColumnBinder() {
	}
}
