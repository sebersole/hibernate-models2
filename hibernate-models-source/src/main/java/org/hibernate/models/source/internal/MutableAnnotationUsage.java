/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal;

import java.lang.annotation.Annotation;

import org.hibernate.models.source.spi.AnnotationUsage;

/**
 * @author Steve Ebersole
 */
public interface MutableAnnotationUsage<A extends Annotation> extends AnnotationUsage<A> {
	<V> V setAttributeValue(String name, V value);
}
