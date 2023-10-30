/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import org.hibernate.models.orm.categorize.spi.BasicIdMapping;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;

/**
 * @author Steve Ebersole
 */
public class BasicIdMappingImpl implements BasicIdMapping {
	private final AttributeMetadata attribute;

	public BasicIdMappingImpl(AttributeMetadata attribute) {
		this.attribute = attribute;
	}

	@Override
	public AttributeMetadata getAttribute() {
		return attribute;
	}
}
