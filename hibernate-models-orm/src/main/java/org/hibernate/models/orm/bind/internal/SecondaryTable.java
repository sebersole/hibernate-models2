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
 * @see jakarta.persistence.SecondaryTable
 *
 * @author Steve Ebersole
 */
public record SecondaryTable(
		Identifier logicalName,
		Identifier physicalName,
		Identifier catalog,
		Identifier schema,
		boolean isAbstract,
		String comment,
		String options,
		boolean owned,
		boolean optional) implements TableReference {
	@Override
	public Identifier getLogicalName() {
		return logicalName;
	}

	@Override
	public boolean isExportable() {
		return !isAbstract;
	}
}
