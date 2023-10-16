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

import static org.hibernate.models.source.internal.jandex.BooleanValueExtractor.JANDEX_BOOLEAN_EXTRACTOR;
import static org.hibernate.models.source.internal.jandex.BooleanValueWrapper.JANDEX_BOOLEAN_VALUE_WRAPPER;
import static org.hibernate.models.source.internal.jdk.PassThruExtractor.PASS_THRU_EXTRACTOR;
import static org.hibernate.models.source.internal.jdk.PassThruWrapper.PASS_THRU_WRAPPER;

/**
 * Descriptor for boolean values
 *
 * @author Steve Ebersole
 */
public class BooleanTypeDescriptor extends AbstractTypeDescriptor<Boolean> {
	public static final BooleanTypeDescriptor BOOLEAN_TYPE_DESCRIPTOR = new BooleanTypeDescriptor();

	@Override
	public Class<Boolean> getWrappedValueType() {
		return Boolean.class;
	}

	@Override
	public ValueWrapper<Boolean,AnnotationValue> createJandexWrapper(SourceModelBuildingContext buildingContext) {
		return JANDEX_BOOLEAN_VALUE_WRAPPER;
	}

	@Override
	public ValueExtractor<AnnotationInstance, Boolean> createJandexExtractor(SourceModelBuildingContext buildingContext) {
		return JANDEX_BOOLEAN_EXTRACTOR;
	}

	@Override
	public ValueWrapper<Boolean, ?> createJdkWrapper(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_WRAPPER;
	}

	@Override
	public ValueExtractor<Annotation, Boolean> createJdkExtractor(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_EXTRACTOR;
	}
}
