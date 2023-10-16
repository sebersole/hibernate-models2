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

import static org.hibernate.models.source.internal.jandex.CharacterValueExtractor.JANDEX_CHARACTER_EXTRACTOR;
import static org.hibernate.models.source.internal.jandex.CharacterValueWrapper.JANDEX_CHARACTER_VALUE_WRAPPER;
import static org.hibernate.models.source.internal.jdk.PassThruExtractor.PASS_THRU_EXTRACTOR;
import static org.hibernate.models.source.internal.jdk.PassThruWrapper.PASS_THRU_WRAPPER;

/**
 * Descriptor for char values
 *
 * @author Steve Ebersole
 */
public class CharacterTypeDescriptor extends AbstractTypeDescriptor<Character> {
	public static final CharacterTypeDescriptor CHARACTER_TYPE_DESCRIPTOR = new CharacterTypeDescriptor();

	@Override
	public Class<Character> getWrappedValueType() {
		return Character.class;
	}

	@Override
	public ValueWrapper<Character, AnnotationValue> createJandexWrapper(SourceModelBuildingContext buildingContext) {
		return JANDEX_CHARACTER_VALUE_WRAPPER;
	}

	@Override
	public ValueExtractor<AnnotationInstance, Character> createJandexExtractor(SourceModelBuildingContext buildingContext) {
		return JANDEX_CHARACTER_EXTRACTOR;
	}

	@Override
	public ValueWrapper<Character, ?> createJdkWrapper(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_WRAPPER;
	}

	@Override
	public ValueExtractor<Annotation, Character> createJdkExtractor(SourceModelBuildingContext buildingContext) {
		//noinspection unchecked
		return PASS_THRU_EXTRACTOR;
	}
}
