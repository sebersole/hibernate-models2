/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.internal.util.NamedConsumer;
import org.hibernate.models.orm.categorize.spi.FilterDefRegistration;

/**
 * @author Steve Ebersole
 */
public interface BindingState {
	Database getDatabase();

	int getTableCount();
	void forEachTable(NamedConsumer<TableReference> consumer);
	<T extends TableReference> T getTableByName(String name);
	void addTable(TableReference table);


	void apply(FilterDefRegistration registration);
}
