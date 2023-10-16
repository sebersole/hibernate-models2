/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.values;

import java.lang.annotation.Annotation;

import org.hibernate.models.source.internal.jandex.EnumValueExtractor;
import org.hibernate.models.source.internal.jandex.EnumValueWrapper;
import org.hibernate.models.source.spi.ValueExtractor;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.source.spi.ValueWrapper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import static org.hibernate.models.source.internal.jdk.PassThruExtractor.PASS_THRU_EXTRACTOR;
import static org.hibernate.models.source.internal.jdk.PassThruWrapper.PASS_THRU_WRAPPER;

/**
 * Descriptor for enum values
 *
 * @author Steve Ebersole
 */
public class EnumTypeDescriptor<E extends Enum<E>> extends AbstractTypeDescriptor<E> {
	private final Class<E> enumType;

	private final EnumValueWrapper<E> jandexWrapper;
	private final EnumValueExtractor<E> jandexExtractor;

	public EnumTypeDescriptor(Class<E> enumType) {
		this.enumType = enumType;
		this.jandexWrapper = new EnumValueWrapper<>( enumType );
		this.jandexExtractor = new EnumValueExtractor<>( jandexWrapper );
	}

	@Override
	public Class<E> getWrappedValueType() {
		return enumType;
	}

	@Override
	public ValueWrapper<E, AnnotationValue> createJandexWrapper(SourceModelBuildingContext buildingContext) {
		return jandexWrapper;
	}

	@Override
	public ValueExtractor<AnnotationInstance, E> createJandexExtractor(SourceModelBuildingContext buildingContext) {
		return jandexExtractor;
	}

	@Override
	public ValueWrapper<E, ?> createJdkWrapper(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_WRAPPER;
	}

	@Override
	public ValueExtractor<Annotation, E> createJdkExtractor(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_EXTRACTOR;
	}
}
