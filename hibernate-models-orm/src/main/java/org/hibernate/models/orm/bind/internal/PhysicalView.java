/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.models.orm.bind.spi.PersistentTableReference;

/**
 * @see org.hibernate.annotations.View
 *
 * @author Steve Ebersole
 */
public record PhysicalView(Identifier logicalName, String query) implements PersistentTableReference {
	@Override
	public Identifier getLogicalName() {
		return logicalName;
	}

	@Override
	public boolean isExportable() {
		return true;
	}
}