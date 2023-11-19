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
 * @author Steve Ebersole
 */
public class DelegateBinders {
	private final TableBinder tableBinder;

	public DelegateBinders(
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this.tableBinder = new TableBinder( bindingState, bindingOptions, bindingContext, this );
	}

	public TableBinder getTableBinder() {
		return tableBinder;
	}
}
