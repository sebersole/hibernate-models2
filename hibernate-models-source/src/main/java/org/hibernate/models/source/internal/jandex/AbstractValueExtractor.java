/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jandex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.ValueExtractor;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.source.spi.ValueWrapper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractValueExtractor<W> implements ValueExtractor<AnnotationInstance,W> {

	protected abstract W extractAndWrap(
			AnnotationValue jandexValue,
			AnnotationTarget target,
			SourceModelBuildingContext buildingContext);

	@Override
	public W extractValue(
			AnnotationInstance annotation,
			String attributeName,
			AnnotationTarget target,
			SourceModelBuildingContext buildingContext) {
		final AnnotationValue jandexValue = resolveAnnotationValue( annotation, attributeName, buildingContext );
		assert jandexValue != null;
		return extractAndWrap( jandexValue, target, buildingContext );
	}

	protected AnnotationValue resolveAnnotationValue(
			AnnotationInstance annotation,
			String attributeName,
			SourceModelBuildingContext buildingContext) {
		final AnnotationValue explicitValue = annotation.value( attributeName );
		if ( explicitValue != null ) {
			return explicitValue;
		}

		return annotation.valueWithDefault( buildingContext.getJandexIndex(), attributeName );
	}
}
