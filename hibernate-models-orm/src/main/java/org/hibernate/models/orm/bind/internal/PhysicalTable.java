/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.models.orm.bind.spi.PhysicalTableReference;

/**
 * Models a physical table from the underlying database schema
 *
 * @see jakarta.persistence.Table
 * @see jakarta.persistence.CollectionTable
 * @see jakarta.persistence.JoinTable
 *
 * @author Steve Ebersole
 */
public record PhysicalTable(
		Identifier logicalName,
		Identifier physicalName,
		Identifier catalog,
		Identifier schema,
		boolean isAbstract,
		String comment,
		String options) implements PhysicalTableReference {

	@Override
	public Identifier getLogicalName() {
		return logicalName;
	}

	@Override
	public boolean isExportable() {
		return !isAbstract;
	}

	@Override
	public Identifier getTableName() {
		return physicalName;
	}

	@Override
	public Identifier getSchemaName() {
		return schema;
	}

	@Override
	public Identifier getCatalogName() {
		return catalog;
	}
}