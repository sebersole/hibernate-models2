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

import static org.hibernate.models.source.internal.jandex.ShortValueWrapper.JANDEX_SHORT_VALUE_WRAPPER;

/**
 * @author Steve Ebersole
 */
public class ShortValueExtractor extends AbstractValueExtractor<Short> {
	public static final ShortValueExtractor JANDEX_SHORT_EXTRACTOR = new ShortValueExtractor();

	protected Short extractAndWrap(
			AnnotationValue jandexValue,
			AnnotationTarget target,
			SourceModelBuildingContext buildingContext) {
		assert jandexValue != null;
		return JANDEX_SHORT_VALUE_WRAPPER.wrap( jandexValue, target, buildingContext );
	}

}
