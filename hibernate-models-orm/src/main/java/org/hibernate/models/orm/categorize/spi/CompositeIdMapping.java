/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.spi;

/**
 * Id-mapping which is embeddable - either {@linkplain AggregatedIdMapping physically}
 * or {@linkplain NonAggregatedIdMapping virtually}.
 *
 * @author Steve Ebersole
 */
public interface CompositeIdMapping extends IdMapping {
}
