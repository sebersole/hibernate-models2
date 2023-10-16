/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jandex;

import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.source.spi.ValueWrapper;

import org.jboss.jandex.AnnotationValue;

/**
 * Wraps AnnotationValue as a character
 *
 * @author Steve Ebersole
 */
public class CharacterValueWrapper implements ValueWrapper<Character,AnnotationValue> {
	public static final CharacterValueWrapper JANDEX_CHARACTER_VALUE_WRAPPER = new CharacterValueWrapper();

	@Override
	public Character wrap(
			AnnotationValue rawValue,
			AnnotationTarget target,
			SourceModelBuildingContext buildingContext) {
		assert rawValue != null;
		return rawValue.asChar();
	}
}
