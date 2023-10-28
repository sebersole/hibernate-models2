/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.util.List;

import org.hibernate.models.orm.bind.spi.NonAggregatedIdMapping;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.source.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public class NonAggregatedIdMappingImpl implements NonAggregatedIdMapping {
	private final List<AttributeMetadata> idAttributes;
	private final ClassDetails idClassType;

	public NonAggregatedIdMappingImpl(List<AttributeMetadata> idAttributes, ClassDetails idClassType) {
		this.idAttributes = idAttributes;
		this.idClassType = idClassType;
	}

	public List<AttributeMetadata> getIdAttributes() {
		return idAttributes;
	}

	@Override
	public ClassDetails getIdClassType() {
		return idClassType;
	}

	@Override
	public ClassDetails getIdType() {
		// todo : whether this is correct depends on how it will be used
		return getIdClassType();
	}
}
