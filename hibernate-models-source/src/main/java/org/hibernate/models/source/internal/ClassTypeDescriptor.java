/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal;

import java.lang.annotation.Annotation;

import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.source.spi.ValueExtractor;
import org.hibernate.models.source.spi.ValueWrapper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import static org.hibernate.models.source.internal.jandex.ClassValueExtractor.JANDEX_CLASS_EXTRACTOR;
import static org.hibernate.models.source.internal.jandex.ClassValueWrapper.JANDEX_CLASS_VALUE_WRAPPER;
import static org.hibernate.models.source.internal.jdk.ClassValueExtractor.JDK_CLASS_EXTRACTOR;
import static org.hibernate.models.source.internal.jdk.ClassValueWrapper.JDK_CLASS_VALUE_WRAPPER;

/**
 * Descriptor for class values
 *
 * @author Steve Ebersole
 */
public class ClassTypeDescriptor extends AbstractTypeDescriptor<ClassDetails> {
	public static final ClassTypeDescriptor CLASS_TYPE_DESCRIPTOR = new ClassTypeDescriptor();

	@Override
	public Class<ClassDetails> getWrappedValueType() {
		return ClassDetails.class;
	}

	@Override
	public ValueWrapper<ClassDetails, AnnotationValue> createJandexWrapper(SourceModelBuildingContext buildingContext) {
		return JANDEX_CLASS_VALUE_WRAPPER;
	}

	@Override
	public ValueExtractor<AnnotationInstance, ClassDetails> createJandexExtractor(SourceModelBuildingContext buildingContext) {
		return JANDEX_CLASS_EXTRACTOR;
	}

	@Override
	public ValueWrapper<ClassDetails, ?> createJdkWrapper(SourceModelBuildingContext buildingContext) {
		return JDK_CLASS_VALUE_WRAPPER;
	}

	@Override
	public ValueExtractor<Annotation, ClassDetails> createJdkExtractor(SourceModelBuildingContext buildingContext) {
		return JDK_CLASS_EXTRACTOR;
	}
}
