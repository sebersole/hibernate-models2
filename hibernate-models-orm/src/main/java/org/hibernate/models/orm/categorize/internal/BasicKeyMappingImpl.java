/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.BasicKeyMapping;

/**
 * @author Steve Ebersole
 */
public class BasicKeyMappingImpl implements BasicKeyMapping {
	private final AttributeMetadata attribute;

	public BasicKeyMappingImpl(AttributeMetadata attribute) {
		this.attribute = attribute;
	}

	@Override
	public AttributeMetadata getAttribute() {
		return attribute;
	}
}