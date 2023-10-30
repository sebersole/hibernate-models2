/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.spi;

/**
 * Simple, single-valued id-mapping
 *
 * @see jakarta.persistence.Basic
 * @see jakarta.persistence.Id
 *
 * @author Steve Ebersole
 */
public interface BasicIdMapping extends SingleAttributeIdMapping {
}
