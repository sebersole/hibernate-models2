/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.Table;
import org.hibernate.models.orm.bind.spi.TableReference;

/**
 * Models a from-clause sub-query.
 *
 * @see org.hibernate.annotations.Subselect
 *
 * @author Steve Ebersole
 */
public record InLineView(Identifier logicalName, Table binding) implements TableReference {
	@Override
	public Identifier getLogicalName() {
		return logicalName;
	}

	public String getQuery() {
		return binding.getSubselect();
	}

	@Override
	public boolean isExportable() {
		return false;
	}

	@Override
	public Table getBinding() {
		return binding;
	}
}
