/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.boot.model.naming.Identifier;

/**
 * @author Steve Ebersole
 */
public interface SchemaAware {
	Identifier getPhysicalSchemaName();
	Identifier getLogicalSchemaName();

	Identifier getPhysicalCatalogName();
	Identifier getLogicalCatalogName();
}
