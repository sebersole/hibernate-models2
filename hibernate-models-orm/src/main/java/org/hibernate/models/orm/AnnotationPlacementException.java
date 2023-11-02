/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

import org.hibernate.models.ModelsException;

/**
 * @author Steve Ebersole
 */
public class AnnotationPlacementException extends ModelsException {
	public AnnotationPlacementException(String message) {
		super( message );
	}

	public AnnotationPlacementException(String message, Throwable cause) {
		super( message, cause );
	}
}
