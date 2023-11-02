/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.models.orm.bind.spi.TableReference;

/**
 * Models a physical table from the underlying database schema
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
		String options) implements TableReference {

	@Override
	public Identifier getLogicalName() {
		return logicalName;
	}

	@Override
	public boolean isExportable() {
		return !isAbstract;
	}


}
