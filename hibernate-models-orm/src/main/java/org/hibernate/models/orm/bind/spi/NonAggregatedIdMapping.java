/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.models.source.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public interface NonAggregatedIdMapping extends CompositeIdMapping {
	ClassDetails getIdClassType();

	@Override
	default ClassDetails getIdType() {
		// todo : whether this is correct depends on how it will be used
		return getIdClassType();
	}
}
