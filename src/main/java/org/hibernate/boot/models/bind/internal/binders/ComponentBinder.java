/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Convert;

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
			BiFunction<String, MemberDetails, ColumnSource> columnSourceResolver,
			BiFunction<String, MemberDetails, Convert> conversionResolver,
			BiFunction<String, MemberDetails, AssociationOverride> associationOverrideResolver,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		return bindProperties(
				componentType,
				component,
				table,
				"",
				columnSourceResolver,
				conversionResolver,
				associationOverrideResolver,
				columnConsumer,
				uniqueByDefault,
				nullableByDefault,
				updatable
		);
	}

	private List<Column> bindProperties(
			ClassDetails componentType,
			Component component,
			Table table,
			String pathPrefix,
			BiFunction<String, MemberDetails, ColumnSource> columnSourceResolver,
			BiFunction<String, MemberDetails, Convert> conversionResolver,
			BiFunction<String, MemberDetails, AssociationOverride> associationOverrideResolver,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		final List<Column> columns = new ArrayList<>();
		componentType.forEachPersistableMember( (member) -> {
			validateMember( member );
			final String attributeName = member.resolveAttributeName();
			final String memberPath = pathPrefix + attributeName;

			if ( isToOneMember( member ) ) {
				final Property property = new Property();
				property.setName( attributeName );
				final var manyToOne = ToOneAttributeBinder.bindToOne(
						componentType.getClassName(),
						attributeName,
						member,
						property,
						table,
						associationOverrideResolver.apply( memberPath, member ),
						state,
						context
				);
				property.setValue( manyToOne );
				component.addProperty( property );
				columns.addAll( manyToOne.getColumns() );
				return;
			}

			if ( isEmbeddedMember( member ) ) {
				final Component nestedComponent = new Component( state.getMetadataBuildingContext(), component );
				nestedComponent.setEmbedded( true );
				nestedComponent.setComponentClassName( member.getType().determineRawClass().getClassName() );
				nestedComponent.setTable( table );
				nestedComponent.setTypeUsingReflection( componentType.getClassName(), attributeName );

				final Property property = createProperty( attributeName, nestedComponent );
				component.addProperty( property );
				columns.addAll( bindProperties(
						member.getType().determineRawClass(),
						nestedComponent,
						table,
						memberPath + ".",
						columnSourceResolver,
						conversionResolver,
						associationOverrideResolver,
						columnConsumer,
						uniqueByDefault,
						nullableByDefault,
						updatable
				) );
				return;
			}

			final BasicValue basicValue = createBasicValue( table, member );
			bindConversion( member, memberPath, basicValue, conversionResolver.apply( memberPath, member ) );
			final Property property = createProperty( attributeName, basicValue );
			component.addProperty( property );

			final Column column = bindColumn(
					() -> attributeName,
					basicValue,
					columnSourceResolver.apply( memberPath, member ),
					uniqueByDefault,
					nullableByDefault,
					updatable
			);
			columnConsumer.accept( member, column );
			columns.add( column );
		} );
		return columns;
	}

	private void validateMember(MemberDetails member) {
		if ( member.isPlural()
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ManyToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ElementCollection.class ) ) {
			throw new UnsupportedOperationException(
					"Only basic embeddable members are supported for now - " + member.getName()
			);
		}
	}

	private boolean isEmbeddedMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.Embedded.class )
				|| member.getType().determineRawClass().hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class );
	}

	private boolean isToOneMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class );
	}

	private void bindConversion(
			MemberDetails member,
			String memberPath,
			BasicValue basicValue,
			Convert conversion) {
		if ( conversion == null || conversion.disableConversion() ) {
			return;
		}

		final String attributeName = conversion.attributeName();
		if ( StringHelper.isNotEmpty( attributeName ) && !memberPath.equals( attributeName ) ) {
			throw new ModelsException( "@Convert#attributeName did not match component path - " + memberPath );
		}

		final Class<AttributeConverter<?, ?>> javaClass = (Class<AttributeConverter<?, ?>>) conversion.converter();
		basicValue.setJpaAttributeConverterDescriptor(
				new RegisteredConversion( null, javaClass, false ).getConverterDescriptor()
		);
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
