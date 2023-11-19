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
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.internal.BindingHelper;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.models.ModelsException;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.internal.SecondPass;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static jakarta.persistence.EnumType.ORDINAL;
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
		registerValueSecondPass( new BasicValueSecondPass( attributeMetadata, getBinding(), basicValue, bindingState, bindingContext ) );

		return basicValue;
	}

	private static void processColumn(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingState bindingState,
			BindingContext bindingContext) {
		// todo : implicit column
		final var columnAnn = member.getAnnotationUsage( Column.class );
		final var column = ColumnBinder.bindColumn( columnAnn, property::getName );

		if ( columnAnn != null ) {
			final var tableName = columnAnn.getString( "table", null );
			TableReference tableByName = null;
			if ( tableName != null ) {
				final Identifier identifier = Identifier.toIdentifier( tableName );
				tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
				basicValue.setTable( tableByName.binding() );
			}
		}

		basicValue.addColumn( column );
	}

	private static void processLob(MemberDetails member, BasicValue basicValue) {
		if ( member.getAnnotationUsage( Lob.class ) != null ) {
			basicValue.makeLob();
		}
	}

	private static void processNationalized(MemberDetails member, BasicValue basicValue) {
		if ( member.getAnnotationUsage( Nationalized.class ) != null ) {
			basicValue.makeNationalized();
		}
	}

	private static void processEnumerated(MemberDetails member, BasicValue basicValue) {
		final AnnotationUsage<Enumerated> enumerated = member.getAnnotationUsage( Enumerated.class );
		if ( enumerated == null ) {
			return;
		}

		basicValue.setEnumerationStyle( enumerated.getEnum( "value", ORDINAL ) );
	}

	private static void processConversion(
			MemberDetails member,
			BasicValue basicValue,
			BindingContext bindingContext) {
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
		final Class<AttributeConverter<?,?>> javaClass = converterClassDetails.toJavaClass();
		basicValue.setJpaAttributeConverterDescriptor( new ClassBasedConverterDescriptor(
				javaClass,
				bindingContext.getClassmateContext()
		) );
	}

	private static void processJavaType(MemberDetails member, BasicValue basicValue) {
		// todo : do we need to account for JavaTypeRegistration here?
		final var javaTypeAnn = member.getAnnotationUsage( JavaType.class );
		if ( javaTypeAnn == null ) {
			return;
		}

		basicValue.setExplicitJavaTypeAccess( (typeConfiguration) -> {
			final var classDetails = javaTypeAnn.getClassDetails( "value" );
			final Class<BasicJavaType<?>> javaClass = classDetails.toJavaClass();
			try {
				return javaClass.getConstructor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				final ModelsException modelsException = new ModelsException( "Error instantiating local @JavaType - " + member.getName() );
				modelsException.addSuppressed( e );
				throw modelsException;
			}
		} );
	}

	private static void processJdbcType(MemberDetails member, BasicValue basicValue) {
		// todo : do we need to account for JdbcTypeRegistration here?
		final var jdbcTypeAnn = member.getAnnotationUsage( JdbcType.class );
		final var jdbcTypeCodeAnn = member.getAnnotationUsage( JdbcTypeCode.class );

		if ( jdbcTypeAnn != null ) {
			if ( jdbcTypeCodeAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @JdbcType and @JdbcTypeCode - " + member.getName()
				);
			}

			basicValue.setExplicitJdbcTypeAccess( (typeConfiguration) -> {
				final var classDetails = jdbcTypeAnn.getClassDetails( "value" );
				final Class<org.hibernate.type.descriptor.jdbc.JdbcType> javaClass = classDetails.toJavaClass();
				try {
					return javaClass.getConstructor().newInstance();
				}
				catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					final ModelsException modelsException = new ModelsException( "Error instantiating local @JdbcType - " + member.getName() );
					modelsException.addSuppressed( e );
					throw modelsException;
				}
			} );
		}
		else if ( jdbcTypeCodeAnn != null ) {
			final Integer typeCode = jdbcTypeCodeAnn.getInteger( "value" );
			basicValue.setExplicitJdbcTypeCode( typeCode );
		}
	}

	private static void processMutability(
			MemberDetails member,
			Property property, BasicValue basicValue) {
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

	private static void processOptimisticLocking(
			MemberDetails member,
			Property property,
			BasicValue basicValue) {
		final var annotationUsage = member.getAnnotationUsage( OptimisticLock.class );
		if ( annotationUsage != null ) {
			if ( annotationUsage.getBoolean( "excluded" ) ) {
				property.setOptimisticLocked( false );
				return;
			}
		}

		property.setOptimisticLocked( true );
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
			BindingState bindingState,
			BindingContext bindingContext) implements ValueSecondPass {

		@Override
			public boolean processValue() {
				final MemberDetails member = attributeMetadata.getMember();
				processColumn( member, property, basicValue, bindingState, bindingContext );
				processLob( member, basicValue );
				processNationalized( member, basicValue );
				processEnumerated( member, basicValue );
				processConversion( member, basicValue, bindingContext );
				processImplicitJavaType( member, basicValue );
				processJavaType( member, basicValue );
				processJdbcType( member, basicValue );
				processMutability( member, property, basicValue );
				processOptimisticLocking( member, property, basicValue );
				processTemporalPrecision( member, basicValue );
				processTimeZoneStorage( member, property, basicValue );

				return true;
			}

		private void processImplicitJavaType(MemberDetails member, BasicValue basicValue) {
			basicValue.setImplicitJavaTypeAccess( (typeConfiguration) -> member.getType().toJavaClass() );
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

		private void processTimeZoneStorage(MemberDetails member, Property property, BasicValue basicValue) {
			final AnnotationUsage<TimeZoneStorage> storageAnn = member.getAnnotationUsage( TimeZoneStorage.class );
			final AnnotationUsage<TimeZoneColumn> columnAnn = member.getAnnotationUsage( TimeZoneColumn.class );
			if ( storageAnn != null ) {
				final TimeZoneStorageType strategy = storageAnn.getEnum( "value", AUTO );
				if ( strategy != COLUMN && columnAnn != null ) {
					throw new AnnotationPlacementException(
							"Illegal combination of @TimeZoneStorage(" + strategy.name() + ") and @TimeZoneColumn"
					);
				}
				basicValue.setTimeZoneStorageType( strategy );
			}

			if ( columnAnn != null ) {
				final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) basicValue.getColumn();
				column.setName( columnAnn.getString( "name", property.getName() + "_tz" ) );
				column.setSqlType( columnAnn.getString( "columnDefinition", null ) );

				final var tableName = columnAnn.getString( "table", null );
				TableReference tableByName = null;
				if ( tableName != null ) {
					final Identifier identifier = Identifier.toIdentifier( tableName );
					tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
					basicValue.setTable( tableByName.binding() );
				}

				property.setInsertable( columnAnn.getBoolean( "insertable", true ) );
				property.setUpdateable( columnAnn.getBoolean( "updatable", true ) );
			}
		}
	}
}
