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
public class TableBinding {
	private final Identifier logicalName;
	private final Identifier physicalName;

	private final Identifier catalog;
	private final Identifier schema;

	private final String comment;
	private final String options;

	public TableBinding(
			Identifier logicalName,
			Identifier physicalName,
			Identifier catalog,
			Identifier schema,
			String comment,
			String options) {
		this.logicalName = logicalName;
		this.physicalName = physicalName;
		this.catalog = catalog;
		this.schema = schema;
		this.comment = comment;
		this.options = options;
	}

	public Identifier getLogicalName() {
		return logicalName;
	}

	public Identifier getPhysicalName() {
		return physicalName;
	}

	public Identifier getCatalog() {
		return catalog;
	}

	public Identifier getSchema() {
		return schema;
	}

	public String getComment() {
		return comment;
	}

	public String getOptions() {
		return options;
	}
}
