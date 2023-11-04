/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.Table;
import org.hibernate.models.orm.bind.spi.PhysicalTableReference;

/**
 * @see jakarta.persistence.SecondaryTable
 *
 * @author Steve Ebersole
 */
public record SecondaryTable(
		Identifier logicalName,
		Identifier logicalCatalogName,
		Identifier logicalSchemaName,
		Identifier physicalName,
		Identifier physicalCatalogName,
		Identifier physicalSchemaName,
		boolean optional,
		boolean owned,
		Table binding) implements PhysicalTableReference {
	@Override
	public Identifier getLogicalName() {
		return logicalName;
	}

	@Override
	public Identifier getLogicalSchemaName() {
		return logicalSchemaName;
	}

	@Override
	public Identifier getLogicalCatalogName() {
		return logicalCatalogName;
	}

	@Override
	public Identifier getPhysicalTableName() {
		return physicalName;
	}

	@Override
	public Identifier getPhysicalSchemaName() {
		return physicalSchemaName;
	}

	@Override
	public Identifier getPhysicalCatalogName() {
		return physicalCatalogName;
	}

	@Override
	public boolean isExportable() {
		return !binding.isAbstract();
	}

	@Override
	public Table getBinding() {
		return binding;
	}
}
