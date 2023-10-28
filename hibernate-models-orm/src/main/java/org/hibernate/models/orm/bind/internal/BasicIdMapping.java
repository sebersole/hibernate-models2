/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import org.hibernate.models.orm.bind.spi.SingleAttributeIdMapping;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;

/**
 * @author Steve Ebersole
 */
public class BasicIdMapping implements SingleAttributeIdMapping {
	private final AttributeMetadata attribute;

	public BasicIdMapping(AttributeMetadata attribute) {
		this.attribute = attribute;
	}

	@Override
	public AttributeMetadata getAttribute() {
		return attribute;
	}
}
