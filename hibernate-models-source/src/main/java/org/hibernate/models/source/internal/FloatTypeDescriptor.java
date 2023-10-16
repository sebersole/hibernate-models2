/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal;

import java.lang.annotation.Annotation;

import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.source.spi.ValueExtractor;
import org.hibernate.models.source.spi.ValueWrapper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import static org.hibernate.models.source.internal.jandex.FloatValueExtractor.JANDEX_FLOAT_EXTRACTOR;
import static org.hibernate.models.source.internal.jandex.FloatValueWrapper.JANDEX_FLOAT_VALUE_WRAPPER;
import static org.hibernate.models.source.internal.jdk.PassThruExtractor.PASS_THRU_EXTRACTOR;
import static org.hibernate.models.source.internal.jdk.PassThruWrapper.PASS_THRU_WRAPPER;

/**
 * Descriptor for float values
 *
 * @author Steve Ebersole
 */
public class FloatTypeDescriptor extends AbstractTypeDescriptor<Float> {
	public static final FloatTypeDescriptor FLOAT_TYPE_DESCRIPTOR = new FloatTypeDescriptor();

	@Override
	public Class<Float> getWrappedValueType() {
		return Float.class;
	}

	@Override
	public ValueWrapper<Float, AnnotationValue> createJandexWrapper(SourceModelBuildingContext buildingContext) {
		return JANDEX_FLOAT_VALUE_WRAPPER;
	}

	@Override
	public ValueExtractor<AnnotationInstance, Float> createJandexExtractor(SourceModelBuildingContext buildingContext) {
		return JANDEX_FLOAT_EXTRACTOR;
	}

	@Override
	public ValueWrapper<Float, ?> createJdkWrapper(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_WRAPPER;
	}

	@Override
	public ValueExtractor<Annotation, Float> createJdkExtractor(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_EXTRACTOR;
	}
}
