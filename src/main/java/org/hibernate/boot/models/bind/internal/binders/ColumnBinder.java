/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.function.Supplier;

import org.hibernate.boot.models.bind.internal.BindingHelper;
import org.hibernate.mapping.Column;
import org.hibernate.models.spi.AnnotationUsage;

import static org.hibernate.internal.util.NullnessHelper.nullif;

/**
 * @author Steve Ebersole
 */
public class ColumnBinder {
	public static Column bindColumn(
			AnnotationUsage<?> annotationUsage,
			Supplier<String> defaultNameSupplier) {
		return bindColumn(
				annotationUsage,
				defaultNameSupplier,
				false,
				true,
				255,
				0,
				0
		);
	}

	public static Column bindColumn(
			AnnotationUsage<?> annotationUsage,
			Supplier<String> defaultNameSupplier,
			boolean uniqueByDefault,
			boolean nullableByDefault) {
		return bindColumn(
				annotationUsage,
				defaultNameSupplier,
				uniqueByDefault,
				nullableByDefault,
				255,
				0,
				0
		);
	}

	public static Column bindColumn(
			AnnotationUsage<?> annotationUsage,
			Supplier<String> defaultNameSupplier,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			int lengthByDefault,
			int precisionByDefault,
			int scaleByDefault) {
		final Column result = new Column();
		result.setName( columnName( annotationUsage, defaultNameSupplier ) );

		result.setUnique( BindingHelper.getValue( annotationUsage, "unique", uniqueByDefault ) );
		result.setNullable( BindingHelper.getValue( annotationUsage, "nullable", nullableByDefault ) );
		result.setSqlType( BindingHelper.getValue( annotationUsage, "columnDefinition", null ) );
		result.setLength( BindingHelper.getValue( annotationUsage, "length", lengthByDefault ) );
		result.setPrecision( BindingHelper.getValue( annotationUsage, "precision", precisionByDefault ) );
		result.setScale( BindingHelper.getValue( annotationUsage, "scale", scaleByDefault ) );
		return result;
	}


	private static String columnName(
			AnnotationUsage<?> columnAnnotation,
			Supplier<String> defaultNameSupplier) {
		if ( columnAnnotation == null ) {
			return defaultNameSupplier.get();
		}

		return nullif( columnAnnotation.getAttributeValue( "name" ), defaultNameSupplier );
	}

	private ColumnBinder() {
	}
}
