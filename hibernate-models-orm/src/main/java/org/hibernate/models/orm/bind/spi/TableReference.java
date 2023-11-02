/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.models.orm.bind.internal.InLineView;
import org.hibernate.models.orm.bind.internal.PhysicalTable;
import org.hibernate.models.orm.bind.internal.PhysicalView;

/**
 * Following the SQL "table reference" rule, will be one of <ul>
 *     <li>a {@linkplain PhysicalTable physical table}</li>
 *     <li>a {@linkplain PhysicalView physical view}</li>
 *     <li>a {@linkplain InLineView in-line view}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface TableReference {
	/**
	 * The name used across the metamodel sources (in the annotations, XML, etc...).
	 * In the case of physical tables and views, the logical name might not be the same
	 * as the table or view name (through {@linkplain org.hibernate.boot.model.naming.PhysicalNamingStrategy}, e.g.).
	 */
	Identifier getLogicalName();

	/**
	 * Should this "table" be exposed to schema tooling?
	 */
	boolean isExportable();
}
