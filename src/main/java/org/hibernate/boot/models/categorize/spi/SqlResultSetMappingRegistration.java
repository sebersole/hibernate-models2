/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import jakarta.persistence.SqlResultSetMapping;

/**
 * Registration of a SQL result set mapping while categorizing XML and annotation sources.
 */
public record SqlResultSetMappingRegistration(String name, SqlResultSetMapping configuration) {
}
