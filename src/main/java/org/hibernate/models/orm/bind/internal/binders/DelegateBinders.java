/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal.binders;

import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.BindingState;

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
