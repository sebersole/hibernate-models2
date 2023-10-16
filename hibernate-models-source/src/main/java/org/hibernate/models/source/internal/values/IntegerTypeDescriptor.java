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

import static org.hibernate.models.source.internal.jandex.IntegerValueExtractor.JANDEX_INTEGER_EXTRACTOR;
import static org.hibernate.models.source.internal.jandex.IntegerValueWrapper.JANDEX_INTEGER_VALUE_WRAPPER;
import static org.hibernate.models.source.internal.jdk.PassThruExtractor.PASS_THRU_EXTRACTOR;
import static org.hibernate.models.source.internal.jdk.PassThruWrapper.PASS_THRU_WRAPPER;

/**
 * Descriptor for integer values
 *
 * @author Steve Ebersole
 */
public class IntegerTypeDescriptor extends AbstractTypeDescriptor<Integer> {
	public static final IntegerTypeDescriptor INTEGER_TYPE_DESCRIPTOR = new IntegerTypeDescriptor();

	@Override
	public Class<Integer> getWrappedValueType() {
		return Integer.class;
	}

	@Override
	public ValueWrapper<Integer, AnnotationValue> createJandexWrapper(SourceModelBuildingContext buildingContext) {
		return JANDEX_INTEGER_VALUE_WRAPPER;
	}

	@Override
	public ValueExtractor<AnnotationInstance, Integer> createJandexExtractor(SourceModelBuildingContext buildingContext) {
		return JANDEX_INTEGER_EXTRACTOR;
	}

	@Override
	public ValueWrapper<Integer, ?> createJdkWrapper(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_WRAPPER;
	}

	@Override
	public ValueExtractor<Annotation, Integer> createJdkExtractor(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_EXTRACTOR;
	}
}
