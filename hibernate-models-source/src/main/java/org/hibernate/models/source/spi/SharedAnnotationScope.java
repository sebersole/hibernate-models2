/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.spi;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.util.List;
import java.util.function.Consumer;

/**
 * A scope for annotation usages.  Differs from {@linkplain AnnotationTarget} in that this
 * is a "registry" of usages that are shareable.
 * <p/>
 * Generally speaking, this equates to global and package scope.
 *
 * @author Steve Ebersole
 */
public interface SharedAnnotationScope {
	<A extends Annotation> List<AnnotationUsage<A>> getAllUsages(Class<A> annotationType);

	default <A extends Annotation> List<AnnotationUsage<A>> getAllUsages(AnnotationDescriptor<A> annotationDescriptor) {
		return getAllUsages( annotationDescriptor.getAnnotationType() );
	}

	<A extends Annotation> void forEachUsage(Class<A> annotationType, Consumer<AnnotationUsage<A>> consumer);

	default <A extends Annotation> void forEachUsage(
			AnnotationDescriptor<A> annotationDescriptor,
			Consumer<AnnotationUsage<A>> consumer) {
		forEachUsage( annotationDescriptor.getAnnotationType(), consumer );
	}
}
