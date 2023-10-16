/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jandex;

import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationValue;

import static org.hibernate.models.source.internal.jandex.FloatValueWrapper.JANDEX_FLOAT_VALUE_WRAPPER;

/**
 * @author Steve Ebersole
 */
public class FloatValueExtractor extends AbstractValueExtractor<Float> {
	public static final FloatValueExtractor JANDEX_FLOAT_EXTRACTOR = new FloatValueExtractor();

	@Override
	protected Float extractAndWrap(
			AnnotationValue jandexValue,
			AnnotationTarget target,
			SourceModelBuildingContext buildingContext) {
		assert jandexValue != null;
		return JANDEX_FLOAT_VALUE_WRAPPER.wrap( jandexValue, target, buildingContext );
	}
}
