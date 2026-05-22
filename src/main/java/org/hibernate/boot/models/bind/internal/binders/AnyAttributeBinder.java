/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.internal.sources.AnySource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

/// Binds a singular Hibernate `@Any` attribute.
///
/// The heavy lifting is delegated to [AnyValueBinder] so the same two-column
/// discriminated association value can later be reused as a `@ManyToAny`
/// collection element.
///
/// @author Steve Ebersole
class AnyAttributeBinder {
	private final AttributeMetadata attributeMetadata;
	private final BindingOptions bindingOptions;
	private final BindingState bindingState;
	private final BindingContext bindingContext;

	AnyAttributeBinder(
			AttributeMetadata attributeMetadata,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		this.attributeMetadata = attributeMetadata;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	Any bind(Property property, Table table) {
		final AnySource source = AnySource.create( attributeMetadata.getMember(), bindingContext, bindingState );
		final Any value = new AnyValueBinder(
				bindingOptions,
				bindingState,
				bindingContext
		).bind( source, attributeMetadata.getName(), table );
		property.setOptional( source.optional() );
		property.setCascade( source.cascades() );
		return value;
	}
}
