/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.values;

import java.lang.annotation.Annotation;

import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.source.spi.ValueExtractor;
import org.hibernate.models.source.spi.ValueWrapper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import static org.hibernate.models.source.internal.jandex.DoubleValueExtractor.JANDEX_DOUBLE_EXTRACTOR;
import static org.hibernate.models.source.internal.jandex.DoubleValueWrapper.JANDEX_DOUBLE_VALUE_WRAPPER;
import static org.hibernate.models.source.internal.jdk.PassThruExtractor.PASS_THRU_EXTRACTOR;
import static org.hibernate.models.source.internal.jdk.PassThruWrapper.PASS_THRU_WRAPPER;

/**
 * Descriptor for double values
 *
 * @author Steve Ebersole
 */
public class DoubleTypeDescriptor extends AbstractTypeDescriptor<Double> {
	public static final DoubleTypeDescriptor DOUBLE_TYPE_DESCRIPTOR = new DoubleTypeDescriptor();

	@Override
	public Class<Double> getWrappedValueType() {
		return Double.class;
	}

	@Override
	public ValueWrapper<Double, AnnotationValue> createJandexWrapper(SourceModelBuildingContext buildingContext) {
		return JANDEX_DOUBLE_VALUE_WRAPPER;
	}

	@Override
	public ValueExtractor<AnnotationInstance, Double> createJandexExtractor(SourceModelBuildingContext buildingContext) {
		return JANDEX_DOUBLE_EXTRACTOR;
	}

	@Override
	public ValueWrapper<Double, ?> createJdkWrapper(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_WRAPPER;
	}

	@Override
	public ValueExtractor<Annotation, Double> createJdkExtractor(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_EXTRACTOR;
	}
}
