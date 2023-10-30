/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.spi;

import org.hibernate.models.source.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public interface SingleAttributeIdMapping extends IdMapping {
	AttributeMetadata getAttribute();

	default String getAttributeName() {
		return getAttribute().getName();
	}

	@Override
	default ClassDetails getIdType() {
		return getAttribute().getMember().getType();
	}
}
