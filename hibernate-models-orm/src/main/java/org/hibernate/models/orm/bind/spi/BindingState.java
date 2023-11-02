/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.internal.util.NamedConsumer;

/**
 * @author Steve Ebersole
 */
public interface BindingState {
	int getNumberOfTableBindings();
	void forEachTableBinding(NamedConsumer<TableBinding> consumer);
	TableBinding getTableBindingByName(String name);
	TableBinding getTableBindingByPhysicalName(String name);
	void addTableBinding(TableBinding tableBinding);

	int getNumberOfVirtualTableBindings();
	void forEachVirtualTableBinding(NamedConsumer<VirtualTableBinding> consumer);
	VirtualTableBinding getVirtualTableBindingByName(String name);
	void addVirtualTableBinding(VirtualTableBinding binding);
}
