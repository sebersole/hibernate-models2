/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jandex;

import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.source.UnknownClassException;
import org.hibernate.models.source.spi.ClassDetailsBuilder;
import org.hibernate.models.source.spi.MethodDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

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
	public JandexClassDetails buildClassDetails(String name, SourceModelBuildingContext buildingContext) {
		return buildClassDetailsStatic( name, buildingContext.getJandexIndex(), buildingContext );
	}

	public static JandexClassDetails buildClassDetailsStatic(
			String name,
			SourceModelBuildingContext processingContext) {
		return buildClassDetailsStatic( name, processingContext.getJandexIndex(), processingContext );
	}

	public static JandexClassDetails buildClassDetailsStatic(
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

		if ( "double".equals( className ) ) {
			return Double.class;
		}

		if ( "float".equals( className ) ) {
			return Float.class;
		}

		return null;
	}

	public static JandexMethodDetails buildMethodDetails(
			MethodInfo method,
			SourceModelBuildingContext buildingContext) {

		if ( method.parametersCount() == 0 ) {
			// could be a getter
			final Type returnType = method.returnType();
			if ( returnType.kind() != Type.Kind.VOID ) {
				final String methodName = method.name();
				if ( methodName.startsWith( "get" ) ) {
					return new JandexMethodDetails(
							method,
							MethodDetails.MethodKind.GETTER,
							buildingContext.getClassDetailsRegistry().resolveClassDetails( returnType.name().toString() ),
							buildingContext
					);
				}
				else if ( isBoolean( returnType ) && ( methodName.startsWith( "is" )
						|| methodName.startsWith( "has" )
						|| methodName.startsWith( "was" ) ) ) {
					return new JandexMethodDetails(
							method,
							MethodDetails.MethodKind.GETTER,
							buildingContext.getClassDetailsRegistry().resolveClassDetails( returnType.name().toString() ),
							buildingContext
					);
				}
			}
		}

		if ( method.parametersCount() == 1
				&& method.returnType().kind() == Type.Kind.VOID
				&& method.name().startsWith( "set" ) ) {
			return new JandexMethodDetails(
					method,
					MethodDetails.MethodKind.GETTER,
					buildingContext.getClassDetailsRegistry().resolveClassDetails( method.parameterType( 0 ).name().toString() ),
					buildingContext
			);
		}

		return new JandexMethodDetails(
				method,
				MethodDetails.MethodKind.OTHER,
				null,
				buildingContext
		);
	}

	private static boolean isBoolean(Type type) {
		if ( type.kind() == Type.Kind.PRIMITIVE ) {
			return type.name().toString().equals( "boolean" );
		}
		return type.name().toString().equals( "java.lang.Boolean" );
	}
}
