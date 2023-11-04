/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import java.util.List;

import org.hibernate.models.orm.categorize.spi.AttributeConsumer;
import org.hibernate.models.orm.categorize.spi.NonAggregatedKeyMapping;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.source.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public class NonAggregatedKeyMappingImpl implements NonAggregatedKeyMapping {
	private final List<AttributeMetadata> idAttributes;
	private final ClassDetails idClassType;

	public NonAggregatedKeyMappingImpl(List<AttributeMetadata> idAttributes, ClassDetails idClassType) {
		this.idAttributes = idAttributes;
		this.idClassType = idClassType;
	}

	@Override
	public List<AttributeMetadata> getIdAttributes() {
		return idAttributes;
	}

	@Override
	public ClassDetails getIdClassType() {
		return idClassType;
	}

	@Override
	public ClassDetails getKeyType() {
		return idClassType;
	}

	@Override
	public void forEachAttribute(AttributeConsumer consumer) {
		for ( int i = 0; i < idAttributes.size(); i++ ) {
			consumer.accept( i, idAttributes.get( i ) );
		}
	}

	@Override
	public boolean contains(AttributeMetadata attributeMetadata) {
		for ( int i = 0; i < idAttributes.size(); i++ ) {
			if ( idAttributes.get( i ) == attributeMetadata ) {
				return true;
			}
		}
		return false;
	}
}
