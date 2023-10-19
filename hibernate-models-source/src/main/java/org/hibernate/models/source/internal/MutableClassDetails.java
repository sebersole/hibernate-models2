/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal;

import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.FieldDetails;
import org.hibernate.models.source.spi.MethodDetails;

/**
 * @author Steve Ebersole
 */
public interface MutableClassDetails extends ClassDetails {
	default void clearMemberAnnotationUsages() {
		forEachField( (i, field) -> ( (MutableAnnotationTarget) field ).clearAnnotationUsages() );
		forEachMethod( (i, method) -> ( (MutableAnnotationTarget) method ).clearAnnotationUsages() );
	}

	void addField(FieldDetails fieldDetails);
	void addMethod(MethodDetails methodDetails);
}
