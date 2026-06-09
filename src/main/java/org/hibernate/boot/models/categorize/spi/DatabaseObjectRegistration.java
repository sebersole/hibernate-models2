/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.List;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;

/**
 * Models an auxiliary database object in the boot mapping model.
 *
 * @param create The create command
 * @param drop The drop command
 * @param definition Optional name of the {@link AuxiliaryDatabaseObject} implementation to use
 * @param dialectScopes Dialect-specific scoping for the object
 *
 * @see AuxiliaryDatabaseObject
 */
public record DatabaseObjectRegistration(
		String create,
		String drop,
		String definition,
		List<DialectScopeRegistration> dialectScopes) {
}
