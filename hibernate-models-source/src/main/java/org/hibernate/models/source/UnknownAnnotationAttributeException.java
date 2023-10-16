/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source;

import java.lang.annotation.Annotation;
import java.util.Locale;

import org.hibernate.models.ModelsException;

/**
 * Indicates an attempt to access a non-existent annotation attribute
 *
 * @author Steve Ebersole
 */
public class UnknownAnnotationAttributeException extends ModelsException {
	public UnknownAnnotationAttributeException(Class<? extends Annotation> annotationType, String attributeName) {
		this(
				String.format(
						Locale.ROOT,
						"Unable to locate attribute named `%s` on annotation (%s)",
						attributeName,
						annotationType.getName()
				)
		);
	}

	public UnknownAnnotationAttributeException(String message) {
		super( message );
	}
}
