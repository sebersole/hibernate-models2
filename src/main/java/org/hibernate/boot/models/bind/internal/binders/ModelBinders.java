/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;

/**
 * Composition of all binders which process aspects of the domain model
 *
 * @author Steve Ebersole
 */
public class ModelBinders {
	private final TableBinder tableBinder;

	public ModelBinders(
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this.tableBinder = new TableBinder( bindingState, bindingOptions, bindingContext, this );
	}

	/**
	 * Binder for tables
	 */
	public TableBinder getTableBinder() {
		return tableBinder;
	}
}
