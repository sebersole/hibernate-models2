/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.internal.util.NamedConsumer;
import org.hibernate.models.orm.bind.internal.InLineView;
import org.hibernate.models.orm.bind.internal.PhysicalTable;
import org.hibernate.models.orm.categorize.spi.FilterDefRegistration;

/**
 * @author Steve Ebersole
 */
public interface BindingState {
	Database getDatabase();

	int getPhysicalTableCount();
	void forEachPhysicalTable(NamedConsumer<PhysicalTable> consumer);
	PhysicalTable getPhysicalTableByName(String name);
	PhysicalTable getPhysicalTableByPhysicalName(String name);
	void addPhysicalTable(PhysicalTable physicalTable);

	int getNumberOfVirtualTableBindings();
	void forEachVirtualTableBinding(NamedConsumer<InLineView> consumer);
	InLineView getVirtualTableBindingByName(String name);
	void addVirtualTableBinding(InLineView binding);

	void apply(FilterDefRegistration registration);
}
