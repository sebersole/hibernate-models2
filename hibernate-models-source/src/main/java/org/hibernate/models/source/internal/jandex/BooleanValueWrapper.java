/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jandex;

import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.source.spi.ValueWrapper;

import org.jboss.jandex.AnnotationValue;

/**
 * Wraps AnnotationValue as a boolean
 *
 * @author Steve Ebersole
 */
public class BooleanValueWrapper implements ValueWrapper<Boolean, AnnotationValue> {
	public static final BooleanValueWrapper JANDEX_BOOLEAN_VALUE_WRAPPER = new BooleanValueWrapper();

	@Override
	public Boolean wrap(AnnotationValue rawValue, AnnotationTarget target, SourceModelBuildingContext buildingContext) {
		assert rawValue != null;
		return rawValue.asBoolean();
	}
}
