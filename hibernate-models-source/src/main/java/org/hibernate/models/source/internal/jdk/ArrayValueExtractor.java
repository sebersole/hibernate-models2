/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jdk;

import java.util.List;

import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.AttributeDescriptor;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.source.spi.ValueWrapper;

/**
 * @author Steve Ebersole
 */
public class ArrayValueExtractor<V,R> extends AbstractValueExtractor<List<V>,R[]> {
	private final ValueWrapper<List<V>,R[]> wrapper;

	public ArrayValueExtractor(ValueWrapper<List<V>, R[]> wrapper) {
		this.wrapper = wrapper;
	}

	@Override
	protected List<V> wrap(
			R[] rawValues,
			AttributeDescriptor<List<V>> attributeDescriptor,
			AnnotationTarget target,
			SourceModelBuildingContext buildingContext) {
		return wrapper.wrap( rawValues, target, buildingContext );
	}
}
