/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

/**
 * Shared support for binding component-valued mappings.
 */
class ComponentBinder {
	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext context;

	ComponentBinder(
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		this.state = state;
		this.options = options;
		this.context = context;
	}

	List<Column> bindBasicProperties(
			ClassDetails componentType,
			Component component,
			Table table,
			Function<MemberDetails, ColumnSource> columnSourceResolver,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		final List<Column> columns = new ArrayList<>();
		componentType.forEachPersistableMember( (member) -> {
			validateBasicMember( member );

			final BasicValue basicValue = createBasicValue( table, member );
			final Property property = createProperty( member.resolveAttributeName(), basicValue );
			component.addProperty( property );

			final Column column = bindColumn(
					member::resolveAttributeName,
					basicValue,
					columnSourceResolver.apply( member ),
					uniqueByDefault,
					nullableByDefault,
					updatable
			);
			columnConsumer.accept( member, column );
			columns.add( column );
		} );
		return columns;
	}

	private void validateBasicMember(MemberDetails member) {
		if ( member.isPlural()
				|| member.hasDirectAnnotationUsage( jakarta.persistence.Embedded.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ManyToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ElementCollection.class ) ) {
			throw new UnsupportedOperationException(
					"Only basic embeddable members are supported for now - " + member.getName()
			);
		}
		if ( member.getType().determineRawClass().hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class ) ) {
			throw new UnsupportedOperationException(
					"Nested embeddables are not yet implemented - " + member.getName()
			);
		}
	}

	private BasicValue createBasicValue(Table table, MemberDetails member) {
		final BasicValue basicValue = new BasicValue( state.getMetadataBuildingContext(), table );
		basicValue.setTable( table );
		AttributeBinder.bindImplicitJavaType( member, null, basicValue, options, state, context );
		BasicValueBinder.bindJavaType( member, null, basicValue, options, state, context );
		BasicValueBinder.bindJdbcType( member, null, basicValue, options, state, context );
		BasicValueBinder.bindLob( member, null, basicValue, options, state, context );
		BasicValueBinder.bindNationalized( member, null, basicValue, options, state, context );
		BasicValueBinder.bindEnumerated( member, null, basicValue, options, state, context );
		BasicValueBinder.bindTemporalPrecision( member, null, basicValue, options, state, context );
		BasicValueBinder.bindTimeZoneStorage( member, null, basicValue, options, state, context );
		return basicValue;
	}

	private Property createProperty(String name, org.hibernate.mapping.Value value) {
		final Property property = new Property();
		property.setName( name );
		property.setValue( value );
		return property;
	}

	private Column bindColumn(
			java.util.function.Supplier<String> implicitName,
			BasicValue basicValue,
			ColumnSource columnSource,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		final Column column = ColumnBinder.bindColumn(
				columnSource,
				implicitName,
				uniqueByDefault,
				nullableByDefault
		);
		basicValue.addColumn( column, true, updatable );
		return column;
	}
}
