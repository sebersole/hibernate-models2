/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.internal.BindingHelper;
import org.hibernate.boot.models.bind.internal.SecondPass;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static org.hibernate.annotations.TimeZoneStorageType.AUTO;
import static org.hibernate.annotations.TimeZoneStorageType.COLUMN;
import static org.hibernate.boot.models.categorize.spi.AttributeMetadata.AttributeNature.BASIC;

/**
 * @author Steve Ebersole
 */
public class AttributeBinder {
	private final AttributeMetadata attributeMetadata;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	private final Property binding;

	private List<ValueSecondPass> valueSecondPasses;

	public AttributeBinder(
			AttributeMetadata attributeMetadata,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this.attributeMetadata = attributeMetadata;
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;

		this.binding = new Property();
		binding.setName( attributeMetadata.getName() );

		if ( attributeMetadata.getNature() == BASIC ) {
			final var basicValue = createBasicValue();
			binding.setValue( basicValue );
		}
		else {
			throw new UnsupportedOperationException( "Not yet implemented" );
		}
	}

	public Property getBinding() {
		return binding;
	}

	private void registerValueSecondPass(ValueSecondPass secondPass) {
		if ( valueSecondPasses == null ) {
			valueSecondPasses = new ArrayList<>();
		}
		valueSecondPasses.add( secondPass );
	}

	public void processSecondPasses() {
		BindingHelper.processSecondPassQueue( valueSecondPasses );
	}

	private BasicValue createBasicValue() {
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext() );
		// probably we don't need this as a second pass...
		registerValueSecondPass( new BasicValueSecondPass( attributeMetadata, getBinding(), basicValue, bindingOptions, bindingState, bindingContext ) );

		return basicValue;
	}

	public static void bindImplicitJavaType(
			MemberDetails member,
			@SuppressWarnings("unused") Property property,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		basicValue.setImplicitJavaTypeAccess( (typeConfiguration) -> member.getType().toJavaClass() );
	}

	public static void bindOptimisticLocking(
			MemberDetails member,
			Property property,
			@SuppressWarnings("unused") BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		final var annotationUsage = member.getAnnotationUsage( OptimisticLock.class );
		if ( annotationUsage != null ) {
			if ( annotationUsage.getBoolean( "excluded" ) ) {
				property.setOptimisticLocked( false );
				return;
			}
		}

		property.setOptimisticLocked( true );
	}

	public static void bindMutability(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		final var mutabilityAnn = member.getAnnotationUsage( Mutability.class );
		final var immutableAnn = member.getAnnotationUsage( Immutable.class );

		if ( immutableAnn != null ) {
			if ( mutabilityAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @Mutability and @Immutable - " + member.getName()
				);
			}

			property.setUpdateable( false );
		}
		else if ( mutabilityAnn != null ) {
			basicValue.setExplicitMutabilityPlanAccess( (typeConfiguration) -> {
				final ClassDetails classDetails = mutabilityAnn.getClassDetails( "value" );
				final Class<MutabilityPlan<?>> javaClass = classDetails.toJavaClass();
				try {
					return javaClass.getConstructor().newInstance();
				}
				catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					final ModelsException modelsException = new ModelsException( "Error instantiating local @MutabilityPlan - " + member.getName() );
					modelsException.addSuppressed( e );
					throw modelsException;
				}
			} );
		}
	}


	@FunctionalInterface
	interface ValueSecondPass extends SecondPass {

		boolean processValue();

		@Override
		default boolean process() {
			return processValue();
		}
	}

	private record BasicValueSecondPass(
			AttributeMetadata attributeMetadata,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) implements ValueSecondPass {

		@Override
		public boolean processValue() {
			final MemberDetails member = attributeMetadata.getMember();
			bindImplicitJavaType( member, property, basicValue, bindingOptions, bindingState, bindingContext );
			bindMutability( member, property, basicValue, bindingOptions, bindingState, bindingContext );
			bindOptimisticLocking( member, property, basicValue, bindingOptions, bindingState, bindingContext );
			bindConversion( member, property, basicValue, bindingOptions, bindingState, bindingContext );

			processColumn( member, property, basicValue, bindingState, bindingContext );

			BasicValueBinder.bindJavaType( member, property, basicValue, bindingOptions, bindingState, bindingContext );
			BasicValueBinder.bindJdbcType( member, property, basicValue, bindingOptions, bindingState, bindingContext );
			BasicValueBinder.bindLob( member, property, basicValue, bindingOptions, bindingState, bindingContext );
			BasicValueBinder.bindNationalized(
					member,
					property,
					basicValue,
					bindingOptions,
					bindingState,
					bindingContext
			);
			BasicValueBinder.bindEnumerated(
					member,
					property,
					basicValue,
					bindingOptions,
					bindingState,
					bindingContext
			);
			BasicValueBinder.bindTemporalPrecision(
					member,
					property,
					basicValue,
					bindingOptions,
					bindingState,
					bindingContext
			);
			BasicValueBinder.bindTimeZoneStorage(
					member,
					property,
					basicValue,
					bindingOptions,
					bindingState,
					bindingContext
			);

			return true;
		}

		private static void processColumn(
				MemberDetails member,
				Property property,
				BasicValue basicValue,
				BindingState bindingState,
				@SuppressWarnings("unused") BindingContext bindingContext) {
			// todo : implicit column
			final var columnAnn = member.getAnnotationUsage( Column.class );
			final var column = ColumnBinder.bindColumn( columnAnn, property::getName );

			if ( columnAnn != null ) {
				final var tableName = columnAnn.getString( "table", (String) null );
				if ( tableName != null ) {
					final Identifier identifier = Identifier.toIdentifier( tableName );
					final TableReference tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
					basicValue.setTable( tableByName.binding() );
				}
			}

			basicValue.addColumn( column );
		}

		private static void bindConversion(
				MemberDetails member,
				@SuppressWarnings("unused") Property property,
				@SuppressWarnings("unused") BasicValue basicValue,
				@SuppressWarnings("unused") BindingOptions bindingOptions,
				@SuppressWarnings("unused") BindingState bindingState,
				@SuppressWarnings("unused") BindingContext bindingContext) {
			// todo : do we need to account for auto-applied converters here?
			final var convertAnn = member.getAnnotationUsage( Convert.class );
			if ( convertAnn == null ) {
				return;
			}

			if ( convertAnn.getBoolean( "disableConversion" ) ) {
				return;
			}

			if ( convertAnn.getString( "attributeName" ) != null ) {
				throw new ModelsException( "@Convert#attributeName should not be specified on basic mappings - " + member.getName() );
			}

			final ClassDetails converterClassDetails = convertAnn.getClassDetails( "converter" );
			final Class<AttributeConverter<?, ?>> javaClass = converterClassDetails.toJavaClass();
			basicValue.setJpaAttributeConverterDescriptor( new ClassBasedConverterDescriptor(
					javaClass,
					bindingContext.getClassmateContext()
			) );
		}

		private void processTemporalPrecision(MemberDetails member, BasicValue basicValue) {
			final AnnotationUsage<Temporal> temporalAnn = member.getAnnotationUsage( Temporal.class );
			if ( temporalAnn == null ) {
				return;
			}

			//noinspection deprecation
			final TemporalType precision = temporalAnn.getEnum( "value" );
			basicValue.setTemporalPrecision( precision );
		}
	}
}
