/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.ClassDetails;

/**
 * Details about the id, relative to a {@linkplain EntityHierarchy hierarchy}
 *
 * @author Steve Ebersole
 */
public interface IdMapping {
	ClassDetails getIdType();
}
