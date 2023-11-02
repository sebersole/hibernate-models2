/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.internal.util.NamedConsumer;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.bind.spi.TableBinding;
import org.hibernate.models.orm.bind.spi.VirtualTableBinding;

/**
 * @author Steve Ebersole
 */
public class BindingStateImpl implements BindingState {
	private Map<String,TableBinding> tableBindingMap;
	private Map<String,VirtualTableBinding> virtualTableBindingMap;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// TableBinding

	@Override
	public int getNumberOfTableBindings() {
		return tableBindingMap == null ? 0 : tableBindingMap.size();
	}

	@Override
	public void forEachTableBinding(NamedConsumer<TableBinding> consumer) {
		if ( tableBindingMap != null ) {
			//noinspection unchecked
			tableBindingMap.forEach( (BiConsumer<? super String, ? super TableBinding>) consumer );
		}
	}

	@Override
	public TableBinding getTableBindingByName(String name) {
		if ( tableBindingMap == null ) {
			return null;
		}
		return tableBindingMap.get( name );
	}

	@Override
	public TableBinding getTableBindingByPhysicalName(String name) {
		if ( tableBindingMap != null ) {
			for ( Map.Entry<String, TableBinding> entry : tableBindingMap.entrySet() ) {
				if ( entry.getValue().getPhysicalName().matches( name ) ) {
					return entry.getValue();
				}
			}
		}
		return null;
	}

	@Override
	public void addTableBinding(TableBinding tableBinding) {
		if ( tableBindingMap == null ) {
			tableBindingMap = new HashMap<>();
		}
		tableBindingMap.put( tableBinding.getLogicalName().getCanonicalName(), tableBinding );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// VirtualTableBinding

	@Override
	public int getNumberOfVirtualTableBindings() {
		return virtualTableBindingMap == null ? 0 : virtualTableBindingMap.size();
	}

	@Override
	public void forEachVirtualTableBinding(NamedConsumer<VirtualTableBinding> consumer) {
		if ( virtualTableBindingMap != null ) {
			//noinspection unchecked
			virtualTableBindingMap.forEach( (BiConsumer<? super String, ? super VirtualTableBinding>) consumer );
		}
	}

	@Override
	public VirtualTableBinding getVirtualTableBindingByName(String name) {
		if ( virtualTableBindingMap == null ) {
			return null;
		}
		return virtualTableBindingMap.get( name );
	}


	@Override
	public void addVirtualTableBinding(VirtualTableBinding binding) {
		if ( virtualTableBindingMap == null ) {
			virtualTableBindingMap = new HashMap<>();
		}
		virtualTableBindingMap.put( binding.getLogicalName().getCanonicalName(), binding );
	}
}
