/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jdk;

import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.AttributeDescriptor;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCommonValueExtractor<W> extends AbstractValueExtractor<W,W> {
	@Override
	protected W wrap(
			W rawValue,
			AttributeDescriptor<W> attributeDescriptor,
			AnnotationTarget target,
			SourceModelBuildingContext buildingContext) {
		return rawValue;
	}
}
