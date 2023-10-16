/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jdk;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.models.source.internal.AnnotationDescriptorRegistryImpl;
import org.hibernate.models.source.internal.values.TypeDescriptors;
import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.AttributeDescriptor;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsBuilder;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.source.spi.ValueTypeDescriptor;

/**
 * ClassDetailsBuilder implementation based on {@link Class}
 *
 * @author Steve Ebersole
 */
public class JdkBuilders implements ClassDetailsBuilder {
	/**
	 * Singleton access
	 */
	public static final JdkBuilders DEFAULT_BUILDER = new JdkBuilders();

	@Override
	public ClassDetails buildClassDetails(String name, SourceModelBuildingContext buildingContext) {
		return buildClassDetailsStatic( name, buildingContext );
	}

	public static ClassDetails buildClassDetailsStatic(String name, SourceModelBuildingContext buildingContext) {
		return buildClassDetailsStatic(
				buildingContext.getClassLoadingAccess().classForName( name ),
				buildingContext
		);
	}

	public static ClassDetails buildClassDetailsStatic(Class<?> javaClass, SourceModelBuildingContext buildingContext) {
		return new JdkClassDetails( javaClass, buildingContext );
	}

	public static <A extends Annotation> AnnotationDescriptorImpl<A> buildAnnotationDescriptor(
			Class<A> annotationType,
			AnnotationDescriptorRegistry descriptorRegistry) {
		return buildAnnotationDescriptor(
				annotationType,
				resolveRepeatableContainerDescriptor( annotationType, descriptorRegistry )
		);
	}

	public static <A extends Annotation, C extends Annotation> AnnotationDescriptor<C> resolveRepeatableContainerDescriptor(
			Class<A> annotationType,
			AnnotationDescriptorRegistry descriptorRegistry) {
		final Repeatable repeatableAnnotation = annotationType.getAnnotation( Repeatable.class );
		if ( repeatableAnnotation == null ) {
			return null;
		}
		//noinspection unchecked
		final AnnotationDescriptor<C> containerDescriptor = (AnnotationDescriptor<C>) descriptorRegistry.getDescriptor( repeatableAnnotation.value() );
		( (AnnotationDescriptorRegistryImpl) descriptorRegistry ).register( containerDescriptor );
		return containerDescriptor;
	}

	public static <A extends Annotation> AnnotationDescriptorImpl<A> buildAnnotationDescriptor(
			Class<A> annotationType,
			AnnotationDescriptor<?> repeatableContainer) {
		return new AnnotationDescriptorImpl<>( annotationType, repeatableContainer );
	}

	public static <A extends Annotation> List<AttributeDescriptor<?>> extractAttributeDescriptors(
			AnnotationDescriptor<A> annotationDescriptor,
			Class<A> annotationType) {
		final Method[] methods = annotationType.getDeclaredMethods();
		final List<AttributeDescriptor<?>> attributeDescriptors = new ArrayList<>( methods.length );
		for ( Method method : methods ) {
			attributeDescriptors.add( createAttributeDescriptor( annotationDescriptor, method ) );
		}
		return attributeDescriptors;
	}

	private static <X, A extends Annotation> AttributeDescriptor<X> createAttributeDescriptor(
			AnnotationDescriptor<A> annotationDescriptor,
			Method method) {
		//noinspection unchecked
		final Class<X> attributeType = (Class<X>) method.getReturnType();

		final ValueTypeDescriptor<X> typeDescriptor = TypeDescriptors.resolveTypeDescriptor( attributeType );
		return typeDescriptor.createAttributeDescriptor( annotationDescriptor, method.getName() );
	}
}
