/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jandex;

import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.source.UnknownClassException;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsBuilder;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

/**
 * Jandex based ClassDetailsBuilder
 *
 * @author Steve Ebersole
 */
public class JandexBuilders implements ClassDetailsBuilder {
	public static final JandexBuilders DEFAULT_BUILDER = new JandexBuilders();

	public JandexBuilders() {
	}

	@Override
	public ClassDetails buildClassDetails(String name, SourceModelBuildingContext buildingContext) {
		return buildClassDetailsStatic( name, buildingContext.getJandexIndex(), buildingContext );
	}

	public static ClassDetails buildClassDetailsStatic(String name, SourceModelBuildingContext processingContext) {
		return buildClassDetailsStatic( name, processingContext.getJandexIndex(), processingContext );
	}

	public static ClassDetails buildClassDetailsStatic(
			String name,
			IndexView jandexIndex,
			SourceModelBuildingContext processingContext) {
		if ( "void".equals( name ) ) {
			name = Void.class.getName();
		}
		final ClassInfo classInfo = jandexIndex.getClassByName( name );
		if ( StringHelper.isNotEmpty( name ) && classInfo == null ) {
			// potentially handle primitives
			final Class<?> primitiveWrapperClass = resolveMatchingPrimitiveWrapper( name );
			if ( primitiveWrapperClass != null ) {
				final ClassInfo wrapperClassInfo = jandexIndex.getClassByName( primitiveWrapperClass.getName() );
				return new JandexClassDetails( wrapperClassInfo, processingContext );
			}

			throw new UnknownClassException( "Could not find class [" + name + "] in Jandex index" );
		}
		return new JandexClassDetails( classInfo, processingContext );
	}

	public static Class<?> resolveMatchingPrimitiveWrapper(String className) {
		if ( "boolean".equals( className ) ) {
			return Boolean.class;
		}

		if ( "byte".equals( className ) ) {
			return Byte.class;
		}

		if ( "short".equals( className ) ) {
			return Short.class;
		}

		if ( "int".equals( className ) ) {
			return Integer.class;
		}

		if ( "long".equals( className ) ) {
			return Long.class;
		}

		if ( "double".equals(  className ) ) {
			return Double.class;
		}

		if ( "float".equals( className ) ) {
			return Float.class;
		}

		return null;
	}
}
