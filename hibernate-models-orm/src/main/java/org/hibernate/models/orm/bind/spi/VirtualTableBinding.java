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
public class VirtualTableBinding {
	private final Identifier logicalName;
	private final String query;

	public VirtualTableBinding(Identifier logicalName, String query) {
		this.logicalName = logicalName;
		this.query = query;
	}

	public Identifier getLogicalName() {
		return logicalName;
	}

	public String getQuery() {
		return query;
	}
}
