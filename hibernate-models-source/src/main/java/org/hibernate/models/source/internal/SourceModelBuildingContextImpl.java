/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.models.source.internal.jdk.JdkBuilders;
import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.RegistryPrimer;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.ClassLoading;

import org.jboss.jandex.IndexView;

/**
 * @author Steve Ebersole
 */
public class SourceModelBuildingContextImpl implements SourceModelBuildingContext {
	private final ClassLoading classLoadingAccess;
	private final IndexView jandexIndex;

	private final AnnotationDescriptorRegistryImpl descriptorRegistry;
	private final ClassDetailsRegistryImpl classDetailsRegistry;

	public SourceModelBuildingContextImpl(ClassLoading classLoadingAccess, IndexView jandexIndex) {
		this( classLoadingAccess, jandexIndex, null );
	}

	public SourceModelBuildingContextImpl(
			ClassLoading classLoadingAccess,
			IndexView jandexIndex,
			RegistryPrimer registryPrimer) {
		this.classLoadingAccess = classLoadingAccess;
		this.jandexIndex = jandexIndex;

		this.descriptorRegistry = new AnnotationDescriptorRegistryImpl();
		this.classDetailsRegistry = new ClassDetailsRegistryImpl( this );

		primeRegistries( registryPrimer );
	}

	@Override
	public AnnotationDescriptorRegistry getAnnotationDescriptorRegistry() {
		return descriptorRegistry;
	}

	@Override
	public ClassDetailsRegistry getClassDetailsRegistry() {
		return classDetailsRegistry;
	}

	@Override
	public ClassLoading getClassLoadingAccess() {
		return classLoadingAccess;
	}

	@Override
	public IndexView getJandexIndex() {
		return jandexIndex;
	}

	@Override
	public <A extends Annotation> List<AnnotationUsage<A>> getAllUsages(Class<A> annotationType) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public <A extends Annotation> void forEachUsage(Class<A> annotationType, Consumer<AnnotationUsage<A>> consumer) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}



	private void primeRegistries(RegistryPrimer registryPrimer) {
		BaseLineJavaTypes.forEachJavaType( this::primeClassDetails );
		AnnotationHelper.forEachOrmAnnotation( this::primeAnnotation );

		if ( registryPrimer != null ) {
			registryPrimer.primeRegistries( new RegistryContributions(), this );
		}
	}

	private void primeClassDetails(Class<?> javaType) {
		// Since we have a Class reference already, it is safe to directly use
		// the reflection
		classDetailsRegistry.resolveClassDetails(
				javaType.getName(),
				() -> JdkBuilders.buildClassDetailsStatic( javaType, this )
		);
	}

	private <A extends Annotation> void primeAnnotation(AnnotationDescriptor<A> descriptor) {
		descriptorRegistry.register( descriptor );
		primeClassDetails( descriptor.getAnnotationType() );
	}

	private class RegistryContributions implements RegistryPrimer.Contributions {
		@Override
		public <A extends Annotation> void registerAnnotation(AnnotationDescriptor<A> descriptor) {
			primeAnnotation( descriptor );
		}

		@Override
		public void registerClass(ClassDetails details) {
			classDetailsRegistry.addClassDetails( details );
		}
	}
}
