/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.spi;

import org.hibernate.models.source.spi.ClassDetails;

/**
 * Details about the id, relative to a {@linkplain org.hibernate.models.orm.categorize.spi.EntityHierarchy hierarchy}
 *
 * @author Steve Ebersole
 */
public interface IdMapping {
	ClassDetails getIdType();
}
