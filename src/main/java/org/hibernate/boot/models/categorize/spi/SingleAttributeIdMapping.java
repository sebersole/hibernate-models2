/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.ClassDetails;

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
