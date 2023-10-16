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

import static org.hibernate.models.source.internal.jandex.IntegerValueWrapper.JANDEX_INTEGER_VALUE_WRAPPER;

/**
 * @author Steve Ebersole
 */
public class IntegerValueExtractor extends AbstractValueExtractor<Integer> {
	public static final IntegerValueExtractor JANDEX_INTEGER_EXTRACTOR = new IntegerValueExtractor();

	@Override
	protected Integer extractAndWrap(
			AnnotationValue jandexValue,
			AnnotationTarget target,
			SourceModelBuildingContext buildingContext) {
		assert jandexValue != null;
		return JANDEX_INTEGER_VALUE_WRAPPER.wrap( jandexValue, target, buildingContext );
	}
}
