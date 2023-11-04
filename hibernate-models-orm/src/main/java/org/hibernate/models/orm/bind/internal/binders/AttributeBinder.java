/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.boot.model.internal.BasicValueBinder;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.models.orm.bind.internal.SecondPass;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;

import static org.hibernate.models.orm.categorize.spi.AttributeMetadata.AttributeNature.BASIC;

/**
 * @author Steve Ebersole
 */
public class AttributeBinder {
	private final AttributeMetadata attributeMetadata;
	private final BindingState bindingState;
	private final BindingOptions options;
	private final BindingContext bindingContext;

	private final Property binding;

	private List<ValueSecondPass> valueSecondPasses;

	public AttributeBinder(
			AttributeMetadata attributeMetadata,
			BindingState bindingState,
			BindingOptions options,
			BindingContext bindingContext) {
		this.attributeMetadata = attributeMetadata;
		this.bindingState = bindingState;
		this.options = options;
		this.bindingContext = bindingContext;

		this.binding = new Property();
		binding.setName( attributeMetadata.getName() );

		if ( attributeMetadata.getNature() == BASIC ) {
			final var basicValue = createBasicValue();
			binding.setValue( basicValue );
			registerValueSecondPass( new BasicValueSecondPass( attributeMetadata, basicValue ) );
		}
		else {
			throw new UnsupportedOperationException( "Not yet implemented" );
		}
	}

	private void registerValueSecondPass(ValueSecondPass secondPass) {
		if ( valueSecondPasses == null ) {
			valueSecondPasses = new ArrayList<>();
		}
		valueSecondPasses.add( secondPass );
	}

	private BasicValue createBasicValue() {
		return new BasicValue( bindingState.getMetadataBuildingContext() );
	}

	public Property getBinding() {
		return binding;
	}

	@FunctionalInterface
	interface ValueSecondPass extends SecondPass {
		boolean processValue();

		@Override
		default boolean process() {
			return processValue();
		}
	}

	private record BasicValueSecondPass(AttributeMetadata attributeMetadata, BasicValue binding) implements ValueSecondPass {

		@Override
			public boolean processValue() {
				return false;
			}
		}
}
