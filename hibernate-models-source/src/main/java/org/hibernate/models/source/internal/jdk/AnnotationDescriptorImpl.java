/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jdk;

import java.lang.annotation.Annotation;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.models.source.UnknownAnnotationAttributeException;
import org.hibernate.models.source.internal.AnnotationHelper;
import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.AttributeDescriptor;

import static org.hibernate.models.source.internal.jdk.JdkBuilders.extractAttributeDescriptors;

/**
 * AnnotationDescriptor built from the Annotation's Class reference
 *
 * @author Steve Ebersole
 */
public class AnnotationDescriptorImpl<A extends Annotation> implements AnnotationDescriptor<A> {
	private final Class<A> annotationType;
	private final EnumSet<Kind> allowableTargets;

	private final boolean inherited;
	private final AnnotationDescriptor<?> repeatableContainer;

	private final List<AttributeDescriptor<?>> attributeDescriptors;

	public AnnotationDescriptorImpl(Class<A> annotationType) {
		this( annotationType, null );
	}

	public AnnotationDescriptorImpl(Class<A> annotationType, AnnotationDescriptor<?> repeatableContainer) {
		this.annotationType = annotationType;
		this.repeatableContainer = repeatableContainer;

		this.inherited = AnnotationHelper.isInherited( annotationType );
		this.allowableTargets = AnnotationHelper.extractTargets( annotationType );

		this.attributeDescriptors = extractAttributeDescriptors( this, annotationType );
	}

	public AnnotationDescriptorImpl(
			Class<A> annotationType,
			AnnotationDescriptor<?> repeatableContainer,
			List<AttributeDescriptor<?>> attributeDescriptors) {
		this.annotationType = annotationType;
		this.repeatableContainer = repeatableContainer;
		this.attributeDescriptors = attributeDescriptors;

		this.inherited = AnnotationHelper.isInherited( annotationType );
		this.allowableTargets = AnnotationHelper.extractTargets( annotationType );
	}

	@Override
	public Class<A> getAnnotationType() {
		return annotationType;
	}

	@Override
	public EnumSet<Kind> getAllowableTargets() {
		return allowableTargets;
	}

	@Override
	public boolean isInherited() {
		return inherited;
	}

	@Override
	public AnnotationDescriptor<?> getRepeatableContainer() {
		return repeatableContainer;
	}

	@Override
	public List<AttributeDescriptor<?>> getAttributes() {
		return attributeDescriptors;
	}

	@Override
	public <V> AttributeDescriptor<V> getAttribute(String name) {
		for ( int i = 0; i < attributeDescriptors.size(); i++ ) {
			final AttributeDescriptor<?> attributeDescriptor = attributeDescriptors.get( i );
			if ( attributeDescriptor.getName().equals( name ) ) {
				//noinspection unchecked
				return (AttributeDescriptor<V>) attributeDescriptor;
			}
		}
		throw new UnknownAnnotationAttributeException( annotationType, name );
	}

	@Override
	public String getName() {
		return annotationType.getName();
	}

	@Override
	public <X extends Annotation> AnnotationUsage<X> getUsage(AnnotationDescriptor<X> descriptor) {
		// there are none
		return null;
	}

	@Override
	public <X extends Annotation> AnnotationUsage<X> getUsage(Class<X> type) {
		// there are none
		return null;
	}

	@Override
	public <X extends Annotation> List<AnnotationUsage<X>> getRepeatedUsages(AnnotationDescriptor<X> type) {
		// there are none
		return null;
	}

	@Override
	public <X extends Annotation> List<AnnotationUsage<X>> getRepeatedUsages(Class<X> type) {
		// there are none
		return null;
	}

	@Override
	public <X extends Annotation> void forEachUsage(
			AnnotationDescriptor<X> type,
			Consumer<AnnotationUsage<X>> consumer) {
		// there are none
	}

	@Override
	public <X extends Annotation> void forEachUsage(Class<X> type, Consumer<AnnotationUsage<X>> consumer) {
		// there are none
	}

	@Override
	public <X extends Annotation> AnnotationUsage<X> getNamedUsage(
			AnnotationDescriptor<X> type,
			String matchName,
			String attributeToMatch) {
		// there are none
		return null;
	}

	@Override
	public <X extends Annotation> AnnotationUsage<X> getNamedUsage(
			Class<X> type,
			String matchName,
			String attributeToMatch) {
		// there are none
		return null;
	}
}
