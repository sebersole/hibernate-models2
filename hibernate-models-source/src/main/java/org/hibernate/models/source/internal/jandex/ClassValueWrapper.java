/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jandex;

import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.source.spi.ValueWrapper;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.Type;

/**
 * Wraps AnnotationValue as a class
 *
 * @author Steve Ebersole
 */
public class ClassValueWrapper implements ValueWrapper<ClassDetails, AnnotationValue> {
	public static final ClassValueWrapper JANDEX_CLASS_VALUE_WRAPPER = new ClassValueWrapper();

	@Override
	public ClassDetails wrap(AnnotationValue rawValue, AnnotationTarget target, SourceModelBuildingContext buildingContext) {
		final Type classReference = rawValue.asClass();
		return buildingContext.getClassDetailsRegistry().resolveClassDetails( classReference.name().toString() );
	}
}
