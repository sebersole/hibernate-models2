/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.Table;
import org.hibernate.models.orm.bind.spi.PersistentTableReference;

/**
 * @see org.hibernate.annotations.View
 *
 * @author Steve Ebersole
 */
public record PhysicalView(
		Identifier logicalName,
		Identifier logicalCatalogName,
		Identifier logicalSchemaName,
		Identifier physicalName,
		Identifier physicalCatalogName,
		Identifier physicalSchemaName,
		Table binding) implements PersistentTableReference {
	@Override
	public Identifier getLogicalName() {
		return logicalName;
	}

	@Override
	public Identifier getPhysicalSchemaName() {
		return physicalSchemaName;
	}

	@Override
	public Identifier getLogicalSchemaName() {
		return logicalSchemaName;
	}

	@Override
	public Identifier getPhysicalCatalogName() {
		return physicalCatalogName;
	}

	@Override
	public Identifier getLogicalCatalogName() {
		return logicalCatalogName;
	}

	@Override
	public boolean isExportable() {
		return true;
	}

	@Override
	public Table getBinding() {
		return binding;
	}
}
