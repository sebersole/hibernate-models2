/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.internal.BindingHelper;
import org.hibernate.boot.models.bind.internal.SecondPass;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;

import static org.hibernate.boot.models.AttributeNature.BASIC;
import static org.hibernate.boot.models.AttributeNature.EMBEDDED;
import static org.hibernate.boot.models.AttributeNature.ELEMENT_COLLECTION;
import static org.hibernate.boot.models.AttributeNature.TO_ONE;

/**
 * @author Steve Ebersole
 */
public class AttributeBinder {
	private final AttributeMetadata attributeMetadata;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	private final Property binding;
	private final Table attributeTable;

	private List<ValueSecondPass> valueSecondPasses;

	public AttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			Table primaryTable,
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
			final var basicValue = createBasicValue( primaryTable );
			binding.setValue( basicValue );
			attributeTable = basicValue.getTable();
		}
		else if ( attributeMetadata.getNature() == TO_ONE ) {
			final var toOneValue = new ToOneAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					primaryTable,
					bindingOptions,
					bindingState,
					bindingContext
			).bind( binding );
			binding.setValue( toOneValue );
			attributeTable = toOneValue.getTable();
		}
		else if ( attributeMetadata.getNature() == EMBEDDED ) {
			final var componentValue = new EmbeddableAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					primaryTable,
					bindingState,
					bindingOptions,
					bindingContext
			).bind( binding );
			binding.setValue( componentValue );
			attributeTable = componentValue.getTable();
		}
		else if ( attributeMetadata.getNature() == ELEMENT_COLLECTION ) {
			final var collectionValue = new ElementCollectionAttributeBinder(
					ownerType,
					ownerBinding,
					attributeMetadata,
					bindingOptions,
					bindingState,
					bindingContext
			).bind( binding );
			binding.setValue( collectionValue );
			attributeTable = collectionValue.getCollectionTable();
		}
		else {
			throw new UnsupportedOperationException( "Not yet implemented" );
		}

		applyNaturalId( attributeMetadata, binding );
	}

	public Property getBinding() {
		return binding;
	}

	public Table getTable() {
		return attributeTable;
	}

	private void applyNaturalId(AttributeMetadata attributeMetadata, Property property) {
		final var naturalIdAnn = attributeMetadata.getMember().getDirectAnnotationUsage( NaturalId.class );
		if ( naturalIdAnn == null ) {
			return;
		}
		property.setNaturalIdentifier( true );
		property.setUpdatable( naturalIdAnn.mutable() );
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

	private BasicValue createBasicValue(Table primaryTable) {
		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext() );

		final MemberDetails member = attributeMetadata.getMember();
		bindMutability( member, binding, basicValue, bindingOptions, bindingState, bindingContext );
		bindOptimisticLocking( member, binding, basicValue, bindingOptions, bindingState, bindingContext );
		bindConversion( member, binding, basicValue, bindingOptions, bindingState, bindingContext );

		processColumn( member, binding, basicValue, primaryTable, bindingOptions, bindingState, bindingContext );

		BasicValueBinder.bindBasicValue(
				BasicValueSource.attribute( member ),
				binding,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);

		return basicValue;
	}

	public static void bindImplicitJavaType(
			MemberDetails member,
			@SuppressWarnings("unused") Property property,
			BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		basicValue.setImplicitJavaTypeAccess( (typeConfiguration) -> member.getType().determineRawClass().toJavaClass() );
	}

	public static void bindOptimisticLocking(
			MemberDetails member,
			Property property,
			@SuppressWarnings("unused") BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		final var annotationUsage = member.getDirectAnnotationUsage( OptimisticLock.class );
		if ( annotationUsage != null ) {
			if ( annotationUsage.excluded() ) {
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
		final var mutabilityAnn = member.getDirectAnnotationUsage( Mutability.class );
		final var immutableAnn = member.getDirectAnnotationUsage( Immutable.class );

		if ( immutableAnn != null ) {
			if ( mutabilityAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @Mutability and @Immutable - " + member.getName()
				);
			}

			property.setUpdatable( false );
		}
		else if ( mutabilityAnn != null ) {
			basicValue.setExplicitMutabilityPlanAccess( (typeConfiguration) -> {
				//noinspection unchecked
				final Class<MutabilityPlan<?>> javaClass = (Class<MutabilityPlan<?>>) mutabilityAnn.value();
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

	public static org.hibernate.mapping.Column processColumn(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		// todo : implicit column
		final var columnAnn = member.getDirectAnnotationUsage( Column.class );
		final var column = ColumnBinder.bindColumn( ColumnSource.from( columnAnn ), property::getName );

		var tableName = columnAnn == null ? "" : columnAnn.table();
		if ( "".equals( tableName ) || tableName == null ) {
			basicValue.setTable( primaryTable );
		}
		else {
			final Identifier identifier = Identifier.toIdentifier( tableName );
			final TableReference tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
			basicValue.setTable( tableByName.binding() );
		}

		basicValue.addColumn( column );

		return column;
	}

	public static void bindConversion(
			MemberDetails member,
			@SuppressWarnings("unused") Property property,
			@SuppressWarnings("unused") BasicValue basicValue,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			@SuppressWarnings("unused") BindingState bindingState,
			@SuppressWarnings("unused") BindingContext bindingContext) {
		// todo : do we need to account for auto-applied converters here?
		final var convertAnn = member.getDirectAnnotationUsage( Convert.class );
		if ( convertAnn == null ) {
			return;
		}

		if ( convertAnn.disableConversion() ) {
			return;
		}

		if ( StringHelper.isNotEmpty( convertAnn.attributeName() ) ) {
			throw new ModelsException( "@Convert#attributeName should not be specified on basic mappings - " + member.getName() );
		}

		//noinspection unchecked
		final Class<AttributeConverter<?, ?>> javaClass = (Class<AttributeConverter<?, ?>>) convertAnn.converter();
		basicValue.setJpaAttributeConverterDescriptor(
				new RegisteredConversion( null, javaClass, false ).getConverterDescriptor()
		);
	}

}
