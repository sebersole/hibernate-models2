/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jandex;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.models.source.internal.AnnotationUsageHelper;
import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import static org.hibernate.models.source.internal.jandex.AnnotationUsageBuilder.collectUsages;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAnnotationTarget implements AnnotationTarget {
	private final SourceModelBuildingContext buildingContext;

	private Map<Class<? extends Annotation>, AnnotationUsage<?>> usagesMap;

	public AbstractAnnotationTarget(SourceModelBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
	}

	/**
	 * The Jandex AnnotationTarget we can use to read the AnnotationInstance from
	 * which to build the {@linkplain #getUsagesMap() AnnotationUsage map}
	 */
	protected abstract org.jboss.jandex.AnnotationTarget getJandexAnnotationTarget();

	protected Map<Class<? extends Annotation>, AnnotationUsage<?>> getUsagesMap() {
		if ( usagesMap == null ) {
			usagesMap = collectUsages( getJandexAnnotationTarget(), this, buildingContext );
		}
		return usagesMap;
	}

	protected SourceModelBuildingContext getBuildingContext() {
		return buildingContext;
	}

	@Override
	public <A extends Annotation> AnnotationUsage<A> getUsage(AnnotationDescriptor<A> descriptor) {
		return AnnotationUsageHelper.getUsage( descriptor, getUsagesMap() );
	}

	@Override
	public <A extends Annotation> AnnotationUsage<A> getUsage(Class<A> type) {
		return getUsage( buildingContext.getAnnotationDescriptorRegistry().getDescriptor( type ) );
	}

	@Override
	public <A extends Annotation> List<AnnotationUsage<A>> getRepeatedUsages(AnnotationDescriptor<A> type) {
		return AnnotationUsageHelper.getRepeatedUsages( type, getUsagesMap() );
	}

	@Override
	public <A extends Annotation> List<AnnotationUsage<A>> getRepeatedUsages(Class<A> type) {
		return getRepeatedUsages( buildingContext.getAnnotationDescriptorRegistry().getDescriptor( type ) );
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
		final List<AnnotationUsage<A>> annotations = getRepeatedUsages( type );
		if ( annotations == null ) {
			return;
		}
		annotations.forEach( consumer );
	}

	@Override
	public <A extends Annotation> AnnotationUsage<A> getNamedUsage(
			AnnotationDescriptor<A> type,
			String matchValue,
			String attributeToMatch) {
		return AnnotationUsageHelper.getNamedAnnotation( type, matchValue, attributeToMatch, getUsagesMap() );
	}
	@Override
	public <A extends Annotation> AnnotationUsage<A> getNamedUsage(
			Class<A> type,
			String matchValue,
			String attributeToMatch) {
		return getNamedUsage(
				buildingContext.getAnnotationDescriptorRegistry().getDescriptor( type ),
				matchValue,
				attributeToMatch
		);
	}

	/**
	 * Delayed collection of AnnotationUsages for a particular target
	 */
	@FunctionalInterface
	public static interface AnnotationUsageCollector {
		Map<Class<? extends Annotation>, AnnotationUsage<?>> collectUsages(
				AnnotationTarget target,
				SourceModelBuildingContext buildingContext);
	}
}
