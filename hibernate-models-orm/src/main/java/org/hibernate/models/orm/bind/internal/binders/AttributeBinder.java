/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal.binders;

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
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.models.ModelsException;
import org.hibernate.models.orm.AnnotationPlacementException;
import org.hibernate.models.orm.bind.internal.BindingHelper;
import org.hibernate.models.orm.bind.internal.SecondPass;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.bind.spi.TableReference;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.MemberDetails;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;

import static jakarta.persistence.EnumType.ORDINAL;
import static org.hibernate.models.orm.categorize.spi.AttributeMetadata.AttributeNature.BASIC;

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
				basicValue.setTable( tableByName.getBinding() );
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
				processJavaType( member, basicValue );
				processJdbcType( member, basicValue );
				processMutability( member, property, basicValue );
				processOptimisticLocking( member, property, basicValue );

				return true;
			}
		}
}
