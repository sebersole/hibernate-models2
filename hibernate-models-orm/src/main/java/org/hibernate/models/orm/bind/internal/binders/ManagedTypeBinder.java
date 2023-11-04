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
import org.hibernate.models.orm.categorize.spi.ManagedTypeMetadata;

/**
 * @author Steve Ebersole
 */
public abstract class ManagedTypeBinder {
	private final ManagedTypeMetadata managedType;

	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext bindingContext;

	public ManagedTypeBinder(
			ManagedTypeMetadata managedType,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		this.managedType = managedType;
		this.state = state;
		this.options = options;
		this.bindingContext = bindingContext;
	}

	public ManagedTypeMetadata getManagedType() {
		return managedType;
	}

	public BindingState getBindingState() {
		return state;
	}

	public BindingOptions getOptions() {
		return options;
	}

	public BindingContext getBindingContext() {
		return bindingContext;
	}

	protected void prepareBinding(DelegateBinders delegateBinders) {
		// todo : ideally we'd pre-process attributes here, but the boot mapping model has
		//		no commonality between embeddable and identifiable (no ManagedType corollary)
	}
}
