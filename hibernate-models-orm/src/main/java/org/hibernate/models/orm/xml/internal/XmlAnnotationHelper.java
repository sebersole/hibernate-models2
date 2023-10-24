/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAssociationOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributeOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCachingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConvertImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGeneratedValueImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLobImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNationalizedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUuidGeneratorImpl;
import org.hibernate.models.ModelsException;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.source.internal.MutableAnnotationTarget;
import org.hibernate.models.source.internal.MutableAnnotationUsage;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static jakarta.persistence.FetchType.EAGER;
import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.orm.xml.internal.XmlProcessingHelper.getOrMakeAnnotation;
import static org.hibernate.models.orm.xml.internal.XmlProcessingHelper.makeAnnotation;

/**
 * Helper for creating annotation from equivalent JAXB
 *
 * @author Steve Ebersole
 */
public class XmlAnnotationHelper {

	/**
	 * Handle creating {@linkplain Entity @Entity} from an {@code <entity/>} element.
	 * Used in both complete and override modes.
	 */
	public static void applyEntity(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final MutableAnnotationUsage<Entity> entityUsage = XmlProcessingHelper.getOrMakeAnnotation( Entity.class, classDetails );
		if ( StringHelper.isNotEmpty( jaxbEntity.getName() ) ) {
			entityUsage.setAttributeValue( "name", jaxbEntity.getName() );
		}
	}

	public static void applyBasic(
			JaxbIdImpl jaxbId,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final DynamicAnnotationUsage<Basic> annotationUsage = new DynamicAnnotationUsage<>( Basic.class, memberDetails );
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "fetch", EAGER );
		annotationUsage.setAttributeValue( "optional", false );
	}

	public static void applyBasic(
			JaxbBasicImpl jaxbBasic,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final MutableAnnotationUsage<Basic> basicAnn = XmlProcessingHelper.getOrMakeAnnotation( Basic.class, memberDetails );
		if ( jaxbBasic.getFetch() != null ) {
			basicAnn.setAttributeValue( "fetch", jaxbBasic.getFetch() );
		}
		if ( jaxbBasic.isOptional() != null ) {
			basicAnn.setAttributeValue( "optional", jaxbBasic.isOptional() );
		}
	}

	public static void applyAccess(
			AccessType accessType,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final DynamicAnnotationUsage<Access> annotationUsage = createAccessAnnotation(
				accessType,
				memberDetails
		);
		memberDetails.addAnnotationUsage( annotationUsage );
	}

	public static DynamicAnnotationUsage<Access> createAccessAnnotation(
			AccessType accessType,
			MutableAnnotationTarget target) {
		final DynamicAnnotationUsage<Access> annotationUsage = new DynamicAnnotationUsage<>(
				Access.class,
				target
		);
		annotationUsage.setAttributeValue( "value", accessType );
		return annotationUsage;
	}

	public static void applyAttributeAccessor(
			String attributeAccessor,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final DynamicAnnotationUsage<AttributeAccessor> accessorAnn = new DynamicAnnotationUsage<>( AttributeAccessor.class, memberDetails );
		memberDetails.addAnnotationUsage( accessorAnn );
		// todo : this is the old, deprecated form
		accessorAnn.setAttributeValue( "value", attributeAccessor );
	}

	public static void applyColumn(
			JaxbColumnImpl jaxbColumn,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		createColumnAnnotation( jaxbColumn, memberDetails );
	}

	private static MutableAnnotationUsage<Column> createColumnAnnotation(
			JaxbColumnImpl jaxbColumn,
			MutableAnnotationTarget target) {
		final MutableAnnotationUsage<Column> columnAnn = XmlProcessingHelper.getOrMakeAnnotation( Column.class, target );

		if ( jaxbColumn != null ) {
			if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
				columnAnn.setAttributeValue( "name", jaxbColumn.getName() );
			}

			if ( StringHelper.isNotEmpty( jaxbColumn.getTable() ) ) {
				columnAnn.setAttributeValue( "table", jaxbColumn.getTable() );
			}

			if ( jaxbColumn.isUnique() != null ) {
				columnAnn.setAttributeValue( "unique", jaxbColumn.isUnique() );
			}

			if ( jaxbColumn.isNullable() != null ) {
				columnAnn.setAttributeValue( "nullable", jaxbColumn.isNullable() );
			}

			if ( jaxbColumn.isInsertable() != null ) {
				columnAnn.setAttributeValue( "insertable", jaxbColumn.isInsertable() );
			}

			if ( jaxbColumn.isUpdatable() != null ) {
				columnAnn.setAttributeValue( "updatable", jaxbColumn.isUpdatable() );
			}

			if ( StringHelper.isNotEmpty( jaxbColumn.getColumnDefinition() ) ) {
				columnAnn.setAttributeValue( "columnDefinition", jaxbColumn.getColumnDefinition() );
			}

			if ( jaxbColumn.getLength() != null ) {
				columnAnn.setAttributeValue( "length", jaxbColumn.getLength() );
			}

			if ( jaxbColumn.getPrecision() != null ) {
				columnAnn.setAttributeValue( "precision", jaxbColumn.getPrecision() );
			}

			if ( jaxbColumn.getScale() != null ) {
				columnAnn.setAttributeValue( "scale", jaxbColumn.getScale() );
			}
		}

		return columnAnn;
	}

	public static void applyFormula(
			String formula,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( StringHelper.isEmpty( formula ) ) {
			return;
		}

		final AnnotationUsage<Formula> annotationUsage = createFormulaAnnotation( formula, memberDetails );
		memberDetails.addAnnotationUsage( annotationUsage );
	}

	private static AnnotationUsage<Formula> createFormulaAnnotation(
			String formula,
			AnnotationTarget target) {
		final DynamicAnnotationUsage<Formula> annotationUsage = new DynamicAnnotationUsage<>( Formula.class, target );
		annotationUsage.setAttributeValue( "value", formula );
		return annotationUsage;
	}

	public static void applyUserType(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbType == null ) {
			return;
		}

		final DynamicAnnotationUsage<Type> typeAnn = new DynamicAnnotationUsage<>( Type.class, memberDetails );
		memberDetails.addAnnotationUsage( typeAnn );

		final ClassDetails userTypeImpl = resolveJavaType( jaxbType.getType(), buildingContext );
		typeAnn.setAttributeValue( "value", userTypeImpl );
		typeAnn.setAttributeValue( "parameters", collectParameters( jaxbType.getParameters(), memberDetails ) );
	}

	public static List<AnnotationUsage<Parameter>> collectParameters(
			List<JaxbConfigurationParameterImpl> jaxbParameters,
			AnnotationTarget target) {
		if ( CollectionHelper.isEmpty( jaxbParameters ) ) {
			return emptyList();
		}

		List<AnnotationUsage<Parameter>> parameterAnnList = new ArrayList<>( jaxbParameters.size() );
		jaxbParameters.forEach( (jaxbParam) -> {
			final DynamicAnnotationUsage<Parameter> annotationUsage = new DynamicAnnotationUsage<>( Parameter.class, target );
			parameterAnnList.add( annotationUsage );
			annotationUsage.setAttributeValue( "name", jaxbParam.getName() );
			annotationUsage.setAttributeValue( "value", jaxbParam.getValue() );
		} );
		return parameterAnnList;
	}

	public static void applyCollectionUserType(
			JaxbCollectionUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbType == null ) {
			return;
		}

		final DynamicAnnotationUsage<CollectionType> typeAnn = new DynamicAnnotationUsage<>( CollectionType.class, memberDetails );
		memberDetails.addAnnotationUsage( typeAnn );

		final ClassDetails userTypeImpl = resolveJavaType( jaxbType.getType(), buildingContext );
		typeAnn.setAttributeValue( "type", userTypeImpl );
		typeAnn.setAttributeValue( "parameters", collectParameters( jaxbType.getParameters(), memberDetails ) );
	}

	public static void applyTargetClass(
			String name,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final ClassDetails classDetails = resolveJavaType( name, sourceModelBuildingContext );
		final DynamicAnnotationUsage<Target> targetAnn = makeAnnotation( Target.class, memberDetails );
		targetAnn.setAttributeValue( "value", classDetails );
	}

	public static void applyTemporal(
			TemporalType temporalType,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( temporalType == null ) {
			return;
		}

		final DynamicAnnotationUsage<Temporal> annotationUsage = new DynamicAnnotationUsage<>( Temporal.class, memberDetails );
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "value", temporalType );
	}

	public static void applyLob(
			JaxbLobImpl jaxbLob,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbLob == null ) {
			return;
		}

		final DynamicAnnotationUsage<Lob> annotationUsage = new DynamicAnnotationUsage<>( Lob.class, memberDetails );
		memberDetails.addAnnotationUsage( annotationUsage );
	}

	public static void applyEnumerated(
			EnumType enumType,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( enumType == null ) {
			return;
		}

		final DynamicAnnotationUsage<Enumerated> annotationUsage = new DynamicAnnotationUsage<>(
				Enumerated.class,
				memberDetails
		);
		memberDetails.addAnnotationUsage( annotationUsage );

		annotationUsage.setAttributeValue( "value", enumType );
	}

	public static void applyNationalized(
			JaxbNationalizedImpl jaxbNationalized,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbNationalized == null ) {
			return;
		}

		final DynamicAnnotationUsage<Nationalized> annotationUsage = new DynamicAnnotationUsage<>(
				Nationalized.class,
				memberDetails
		);
		memberDetails.addAnnotationUsage( annotationUsage );
	}

	public static void applyGeneratedValue(
			JaxbGeneratedValueImpl jaxbGeneratedValue,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbGeneratedValue == null ) {
			return;
		}

		final DynamicAnnotationUsage<GeneratedValue> generatedValueAnn = new DynamicAnnotationUsage<>( GeneratedValue.class, memberDetails );
		memberDetails.addAnnotationUsage( generatedValueAnn );
		generatedValueAnn.setAttributeValue( "strategy", jaxbGeneratedValue.getStrategy() );
		generatedValueAnn.setAttributeValue( "generator", jaxbGeneratedValue.getGenerator() );
	}

	public static void applySequenceGenerator(
			JaxbSequenceGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final MutableAnnotationUsage<SequenceGenerator> sequenceAnn = XmlProcessingHelper.getOrMakeNamedAnnotation(
				SequenceGenerator.class,
				jaxbGenerator.getName(),
				memberDetails
		);

		if ( StringHelper.isNotEmpty( jaxbGenerator.getSequenceName() ) ) {
			sequenceAnn.setAttributeValue( "sequenceName", jaxbGenerator.getSequenceName() );
		}

		sequenceAnn.setAttributeValue( "catalog", jaxbGenerator.getCatalog() );
		sequenceAnn.setAttributeValue( "schema", jaxbGenerator.getSchema() );
		sequenceAnn.setAttributeValue( "initialValue", jaxbGenerator.getInitialValue() );
		sequenceAnn.setAttributeValue( "allocationSize", jaxbGenerator.getInitialValue() );
	}

	public static void applyTableGenerator(
			JaxbTableGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final DynamicAnnotationUsage<TableGenerator> annotationUsage = new DynamicAnnotationUsage<>( TableGenerator.class, memberDetails );
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "name", jaxbGenerator.getName() );
		annotationUsage.setAttributeValue( "table", jaxbGenerator.getTable() );
		annotationUsage.setAttributeValue( "catalog", jaxbGenerator.getCatalog() );
		annotationUsage.setAttributeValue( "schema", jaxbGenerator.getSchema() );
		annotationUsage.setAttributeValue( "pkColumnName", jaxbGenerator.getPkColumnName() );
		annotationUsage.setAttributeValue( "valueColumnName", jaxbGenerator.getValueColumnName() );
		annotationUsage.setAttributeValue( "pkColumnValue", jaxbGenerator.getPkColumnValue() );
		annotationUsage.setAttributeValue( "initialValue", jaxbGenerator.getInitialValue() );
		annotationUsage.setAttributeValue( "allocationSize", jaxbGenerator.getInitialValue() );
		// todo : uniqueConstraints
		// todo : indexes
	}

	public static void applyUuidGenerator(
			JaxbUuidGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final DynamicAnnotationUsage<UuidGenerator> annotationUsage = new DynamicAnnotationUsage<>( UuidGenerator.class, memberDetails );
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "style", jaxbGenerator.getStyle() );
	}

	public static void applyAttributeOverrides(
			List<JaxbAttributeOverrideImpl> jaxbOverrides,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( CollectionHelper.isEmpty( jaxbOverrides ) ) {
			return;
		}

		jaxbOverrides.forEach( (jaxbOverride) -> {
			final DynamicAnnotationUsage<AttributeOverride> annotationUsage = new DynamicAnnotationUsage<>(
					AttributeOverride.class,
					memberDetails
			);
			memberDetails.addAnnotationUsage( annotationUsage );
			annotationUsage.setAttributeValue( "name", jaxbOverride.getName() );
			annotationUsage.setAttributeValue( "column", createColumnAnnotation( jaxbOverride.getColumn(), memberDetails ) );
		} );
	}

	public static void applyAssociationOverrides(
			List<JaxbAssociationOverrideImpl> jaxbOverrides,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( CollectionHelper.isEmpty( jaxbOverrides ) ) {
			return;
		}

		jaxbOverrides.forEach( (jaxbOverride) -> {
			final DynamicAnnotationUsage<AssociationOverride> annotationUsage = new DynamicAnnotationUsage<>(
					AssociationOverride.class,
					memberDetails
			);
			memberDetails.addAnnotationUsage( annotationUsage );
			annotationUsage.setAttributeValue( "name", jaxbOverride.getName() );
			// todo : join columns
			// todo : join table
			// todo : foreign key
		} );
	}

	public static void applyOptimisticLockInclusion(
			boolean inclusion,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final DynamicAnnotationUsage<OptimisticLock> annotationUsage = new DynamicAnnotationUsage<>(
				OptimisticLock.class,
				memberDetails
		);
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "exclude", !inclusion );
	}

	public static void applyConvert(
			JaxbConvertImpl jaxbConvert,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbConvert == null ) {
			return;
		}

		final DynamicAnnotationUsage<Convert> annotationUsage = new DynamicAnnotationUsage<>(
				Convert.class,
				memberDetails
		);
		memberDetails.addAnnotationUsage( annotationUsage );

		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();
		final ClassDetails converter = classDetailsRegistry.resolveClassDetails( jaxbConvert.getConverter() );
		annotationUsage.setAttributeValue( "converter", converter );
		annotationUsage.setAttributeValue( "attributeName", jaxbConvert.getAttributeName() );
		annotationUsage.setAttributeValue( "disableConversion", jaxbConvert.isDisableConversion() );
	}

	public static void applyTable(
			JaxbTableImpl jaxbTable,
			MutableAnnotationTarget target,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		final DynamicAnnotationUsage<Table> tableAnn = new DynamicAnnotationUsage<>( Table.class, target );
		target.addAnnotationUsage( tableAnn );

		applyTableAttributes( tableAnn, jaxbTable, persistenceUnitMetadata );

		// todo : uniqueConstraints
		// todo : indexes
	}

	public static void applyTableOverride(
			JaxbTableImpl jaxbTable,
			MutableAnnotationTarget target,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		if ( jaxbTable == null ) {
			return;
		}

		final MutableAnnotationUsage<Table> tableAnn = XmlProcessingHelper.getOrMakeAnnotation( Table.class, target );

		applyTableAttributes( tableAnn, jaxbTable, persistenceUnitMetadata );

		// todo : uniqueConstraints
		// todo : indexes
	}

	private static void applyTableAttributes(
			MutableAnnotationUsage<Table> tableAnn,
			JaxbTableImpl jaxbTable,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		applyAttributeIfSpecified( tableAnn, "name", jaxbTable.getName() );
		applyAttributeIfSpecified( tableAnn, "catalog", jaxbTable.getCatalog(), persistenceUnitMetadata.getDefaultCatalog() );
		applyAttributeIfSpecified( tableAnn, "schema", jaxbTable.getSchema(), persistenceUnitMetadata.getDefaultSchema() );
	}

	private static <A extends Annotation> void applyAttributeIfSpecified(
			MutableAnnotationUsage<A> annotationUsage,
			String attributeName,
			String value) {
		if ( StringHelper.isNotEmpty( value ) ) {
			annotationUsage.setAttributeValue( attributeName, value );
		}
	}

	private static <A extends Annotation, V> void applyAttributeIfSpecified(
			MutableAnnotationUsage<A> annotationUsage,
			String attributeName,
			V... values) {
		final V coalesced = coalesce( values );
		if ( coalesced != null ) {
			annotationUsage.setAttributeValue( attributeName, coalesced );
		}
	}

	private static <A extends Annotation> void applyAttributeIfSpecified(
			MutableAnnotationUsage<A> tableAnn,
			String attributeName,
			Object value) {
		if ( value != null ) {
			tableAnn.setAttributeValue( attributeName, value );
		}
	}

	public static void applyNaturalId(
			JaxbNaturalId jaxbNaturalId,
			MutableMemberDetails backingMember,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbNaturalId == null ) {
			return;
		}
		final DynamicAnnotationUsage<NaturalId> annotationUsage = new DynamicAnnotationUsage<>(
				NaturalId.class,
				backingMember
		);
		backingMember.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "mutable", jaxbNaturalId.isMutable() );
	}

	public static void applyNaturalIdCache(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails classDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbNaturalId == null || jaxbNaturalId.getCaching() == null ) {
			return;
		}

		final DynamicAnnotationUsage<NaturalIdCache> annotationUsage = new DynamicAnnotationUsage<>(
				NaturalIdCache.class,
				classDetails
		);
		classDetails.addAnnotationUsage( annotationUsage );

		final JaxbCachingImpl jaxbCaching = jaxbNaturalId.getCaching();
		annotationUsage.setAttributeValue( "region", jaxbCaching.getRegion() );
	}

	public static void applyId(
			JaxbIdImpl jaxbId,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbId == null ) {
			return;
		}
		final DynamicAnnotationUsage<Id> annotationUsage = new DynamicAnnotationUsage<>(
				Id.class,
				memberDetails
		);
		memberDetails.addAnnotationUsage( annotationUsage );
	}

	public static void applyEmbeddedId(
			JaxbEmbeddedIdImpl jaxbEmbeddedId,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbEmbeddedId == null ) {
			return;
		}
		final DynamicAnnotationUsage<EmbeddedId> annotationUsage = new DynamicAnnotationUsage<>(
				EmbeddedId.class,
				memberDetails
		);
		memberDetails.addAnnotationUsage( annotationUsage );
	}

	static void applyInheritance(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbEntity.getInheritance() == null ) {
			return;
		}

		final MutableAnnotationUsage<Inheritance> inheritanceAnn = getOrMakeAnnotation(
				Inheritance.class,
				classDetails
		);
		inheritanceAnn.setAttributeValue( "strategy", jaxbEntity.getInheritance().getStrategy() );
	}

	public static ClassDetails resolveJavaType(String value, SourceModelBuildingContext sourceModelBuildingContext) {
		return resolveJavaType( value, sourceModelBuildingContext.getClassDetailsRegistry() );
	}

	public static ClassDetails resolveJavaType(String value, ClassDetailsRegistry classDetailsRegistry) {
		if ( StringHelper.isEmpty( value ) ) {
			value = Object.class.getName();
		}
		else if ( byte.class.getName().equals( value )
				|| boolean.class.getName().equals( value )
				|| short.class.getName().equals( value )
				|| int.class.getName().equals( value )
				|| long.class.getName().equals( value )
				|| double.class.getName().equals( value )
				|| float.class.getName().equals( value ) ) {
			// nothing to do for primitives
		}
		else if ( Byte.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = Byte.class.getName();
		}
		else if ( Boolean.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = Boolean.class.getName();
		}
		else if ( Short.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = Short.class.getName();
		}
		else if ( Integer.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = Integer.class.getName();
		}
		else if ( Long.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = Long.class.getName();
		}
		else if ( Double.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = Double.class.getName();
		}
		else if ( Float.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = Float.class.getName();
		}
		else if ( BigInteger.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = BigInteger.class.getName();
		}
		else if ( BigDecimal.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = BigDecimal.class.getName();
		}
		else if ( String.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = String.class.getName();
		}
		else if ( Character.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = Character.class.getName();
		}
		else if ( UUID.class.getSimpleName().equalsIgnoreCase( value ) ) {
			value = Character.class.getName();
		}

		return classDetailsRegistry.resolveClassDetails( value );
	}

	public static void applyBasicTypeComposition(
			JaxbBasicMapping jaxbBasicMapping,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbBasicMapping.getType() != null ) {
			applyUserType( jaxbBasicMapping.getType(), memberDetails, sourceModelBuildingContext );
		}
		else if ( jaxbBasicMapping.getJavaType() != null ) {
			applyJavaTypeDescriptor( jaxbBasicMapping.getJavaType(), memberDetails, sourceModelBuildingContext );
		}
		else if ( StringHelper.isNotEmpty( jaxbBasicMapping.getTarget() ) ) {
			applyTargetClass( jaxbBasicMapping.getTarget(), memberDetails, sourceModelBuildingContext );
		}

		if ( StringHelper.isNotEmpty( jaxbBasicMapping.getJdbcType() ) ) {
			applyJdbcTypeDescriptor( jaxbBasicMapping.getJdbcType(), memberDetails, sourceModelBuildingContext );
		}
		else if ( jaxbBasicMapping.getJdbcTypeCode() != null ) {
			applyJdbcTypeCode( jaxbBasicMapping.getJdbcTypeCode(), memberDetails, sourceModelBuildingContext );
		}
		else if ( StringHelper.isNotEmpty( jaxbBasicMapping.getJdbcTypeName() ) ) {
			applyJdbcTypeCode(
					resolveJdbcTypeName( jaxbBasicMapping.getJdbcTypeName() ),
					memberDetails,
					sourceModelBuildingContext
			);
		}
	}

	private static int resolveJdbcTypeName(String name) {
		try {
			final Field matchingField = SqlTypes.class.getDeclaredField( name );
			return matchingField.getInt( null );
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			throw new ModelsException( "Could not resolve <jdbc-type-name>" + name + "</jdbc-type-name>", e );
		}
	}

	public static void applyJavaTypeDescriptor(
			String descriptorClassName,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final DynamicAnnotationUsage<JavaType> typeAnn = new DynamicAnnotationUsage<>( JavaType.class, memberDetails );
		memberDetails.addAnnotationUsage( typeAnn );

		final ClassDetails descriptorClass = sourceModelBuildingContext
				.getClassDetailsRegistry()
				.resolveClassDetails( descriptorClassName );
		typeAnn.setAttributeValue( "value", descriptorClass );
	}


	private static void applyJdbcTypeDescriptor(
			String descriptorClassName,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final ClassDetails descriptorClassDetails = sourceModelBuildingContext
				.getClassDetailsRegistry()
				.resolveClassDetails( descriptorClassName );
		final DynamicAnnotationUsage<JdbcType> jdbcTypeAnn = makeAnnotation( JdbcType.class, memberDetails );
		jdbcTypeAnn.setAttributeValue( "value", descriptorClassDetails );

	}

	public static void applyJdbcTypeCode(
			Integer jdbcTypeCode,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jdbcTypeCode == null ) {
			return;
		}

		final DynamicAnnotationUsage<JdbcTypeCode> typeCodeAnn = new DynamicAnnotationUsage<>( JdbcTypeCode.class, memberDetails );
		memberDetails.addAnnotationUsage( typeCodeAnn );
		typeCodeAnn.setAttributeValue( "value", jdbcTypeCode );
	}
}
