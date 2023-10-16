/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jdk;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.models.source.internal.AnnotationUsageHelper;
import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

/**
 * AnnotationTarget where we know the annotations up front, but
 * want to delay processing them until (unless!) they are needed
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAnnotationTarget implements AnnotationTarget {
	private final Supplier<Annotation[]> annotationSupplier;
	private final SourceModelBuildingContext buildingContext;

	private Map<Class<? extends Annotation>, AnnotationUsage<?>> usagesMap;

	public AbstractAnnotationTarget(
			Supplier<Annotation[]> annotationSupplier,
			SourceModelBuildingContext buildingContext) {
		this.annotationSupplier = annotationSupplier;
		this.buildingContext = buildingContext;
	}

	protected SourceModelBuildingContext getBuildingContext() {
		return buildingContext;
	}

	@Override
	public <A extends Annotation> AnnotationUsage<A> getUsage(AnnotationDescriptor<A> type) {
		return AnnotationUsageHelper.getUsage( type, resolveUsagesMap() );
	}

	@Override
	public <A extends Annotation> AnnotationUsage<A> getUsage(Class<A> type) {
		final AnnotationDescriptor<A> descriptor = buildingContext
				.getAnnotationDescriptorRegistry()
				.getDescriptor( type );
		return getUsage( descriptor );
	}

	@Override
	public <A extends Annotation> List<AnnotationUsage<A>> getRepeatedUsages(AnnotationDescriptor<A> type) {
		return AnnotationUsageHelper.getRepeatedUsages( type, resolveUsagesMap() );
	}

	@Override
	public <A extends Annotation> List<AnnotationUsage<A>> getRepeatedUsages(Class<A> type) {
		final AnnotationDescriptor<A> descriptor = buildingContext
				.getAnnotationDescriptorRegistry()
				.getDescriptor( type );
		return getRepeatedUsages( descriptor );
	}

	@Override
	public <A extends Annotation> void forEachUsage(AnnotationDescriptor<A> type, Consumer<AnnotationUsage<A>> consumer) {
		final List<AnnotationUsage<A>> annotations = getRepeatedUsages( type );
		if ( annotations == null ) {
			return;
		}
		annotations.forEach( consumer );
	}

	@Override
	public <A extends Annotation> void forEachUsage(Class<A> type, Consumer<AnnotationUsage<A>> consumer) {
		final AnnotationDescriptor<A> descriptor = buildingContext
				.getAnnotationDescriptorRegistry()
				.getDescriptor( type );
		forEachUsage( descriptor, consumer );
	}

	@Override
	public <A extends Annotation> AnnotationUsage<A> getNamedUsage(
			AnnotationDescriptor<A> type,
			String matchValue,
			String attributeToMatch) {
		return AnnotationUsageHelper.getNamedAnnotation( type, matchValue, attributeToMatch, resolveUsagesMap() );
	}

	@Override
	public <A extends Annotation> AnnotationUsage<A> getNamedUsage(
			Class<A> type,
			String matchName,
			String attributeToMatch) {
		final AnnotationDescriptor<A> descriptor = buildingContext
				.getAnnotationDescriptorRegistry()
				.getDescriptor( type );
		return getNamedUsage( descriptor, matchName, attributeToMatch );
	}

	private Map<Class<? extends Annotation>, AnnotationUsage<?>> resolveUsagesMap() {
		if ( usagesMap == null ) {
			usagesMap = buildUsagesMap();
		}
		return usagesMap;
	}

	private Map<Class<? extends Annotation>, AnnotationUsage<?>> buildUsagesMap() {
		final Map<Class<? extends Annotation>, AnnotationUsage<?>> result = new HashMap<>();
		AnnotationUsageBuilder.processAnnotations(
				annotationSupplier.get(),
				this,
				result::put,
				buildingContext
		);
		return result;
	}
}
