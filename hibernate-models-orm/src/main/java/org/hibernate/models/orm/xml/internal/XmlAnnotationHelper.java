/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.jaxb.mapping.JaxbAssociationOverride;
import org.hibernate.boot.jaxb.mapping.JaxbAttributeOverride;
import org.hibernate.boot.jaxb.mapping.JaxbBasic;
import org.hibernate.boot.jaxb.mapping.JaxbCaching;
import org.hibernate.boot.jaxb.mapping.JaxbColumn;
import org.hibernate.boot.jaxb.mapping.JaxbColumnType;
import org.hibernate.boot.jaxb.mapping.JaxbConfigurationParameter;
import org.hibernate.boot.jaxb.mapping.JaxbConvert;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddedId;
import org.hibernate.boot.jaxb.mapping.JaxbGeneratedValue;
import org.hibernate.boot.jaxb.mapping.JaxbId;
import org.hibernate.boot.jaxb.mapping.JaxbLob;
import org.hibernate.boot.jaxb.mapping.JaxbNationalized;
import org.hibernate.boot.jaxb.mapping.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.JaxbSequenceGenerator;
import org.hibernate.boot.jaxb.mapping.JaxbTable;
import org.hibernate.boot.jaxb.mapping.JaxbTableGenerator;
import org.hibernate.boot.jaxb.mapping.JaxbUuidGenerator;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.source.internal.MutableAnnotationTarget;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static jakarta.persistence.FetchType.EAGER;
import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.internal.StringHelper.nullIfEmpty;

/**
 * Helper for creating annotation from equivalent JAXB
 *
 * @author Steve Ebersole
 */
public class XmlAnnotationHelper {

	public static void applyBasic(
			JaxbBasic jaxbBasic,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final DynamicAnnotationUsage<Basic> annotationUsage = new DynamicAnnotationUsage<>( Basic.class, memberDetails );
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "fetch", jaxbBasic.getFetch() );
		annotationUsage.setAttributeValue( "optional", jaxbBasic.isOptional() );
	}

	public static void applyBasic(
			JaxbId jaxbId,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final DynamicAnnotationUsage<Basic> annotationUsage = new DynamicAnnotationUsage<>( Basic.class, memberDetails );
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "fetch", EAGER );
		annotationUsage.setAttributeValue( "optional", false );
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
			JaxbColumn jaxbColumn,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		final DynamicAnnotationUsage<Column> columnAnn = createColumnAnnotation( jaxbColumn, memberDetails );
		memberDetails.addAnnotationUsage( columnAnn );
	}

	private static DynamicAnnotationUsage<Column> createColumnAnnotation(
			JaxbColumn jaxbColumn,
			AnnotationTarget target) {
		final DynamicAnnotationUsage<Column> columnAnn = new DynamicAnnotationUsage<>( Column.class, target );

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
			JaxbColumnType jaxbType,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbType == null ) {
			return;
		}

		final DynamicAnnotationUsage<Type> typeAnn = new DynamicAnnotationUsage<>( Type.class, memberDetails );
		memberDetails.addAnnotationUsage( typeAnn );

		final ClassDetails userTypeImpl = buildingContext.getClassDetailsRegistry().resolveClassDetails( jaxbType.getValue() );
		typeAnn.setAttributeValue( "value", userTypeImpl );
		typeAnn.setAttributeValue( "parameters", collectParameters( jaxbType.getParameters(), memberDetails ) );
	}

	private static List<AnnotationUsage<Parameter>> collectParameters(
			List<JaxbConfigurationParameter> jaxbParameters,
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
			JaxbLob jaxbLob,
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
			JaxbNationalized jaxbNationalized,
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
			JaxbGeneratedValue jaxbGeneratedValue,
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
			JaxbSequenceGenerator jaxbGenerator,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final DynamicAnnotationUsage<SequenceGenerator> annotationUsage = new DynamicAnnotationUsage<>( SequenceGenerator.class, memberDetails );
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "name", jaxbGenerator.getName() );
		annotationUsage.setAttributeValue( "sequenceName", jaxbGenerator.getSequenceName() );
		annotationUsage.setAttributeValue( "catalog", jaxbGenerator.getCatalog() );
		annotationUsage.setAttributeValue( "schema", jaxbGenerator.getSchema() );
		annotationUsage.setAttributeValue( "initialValue", jaxbGenerator.getInitialValue() );
		annotationUsage.setAttributeValue( "allocationSize", jaxbGenerator.getInitialValue() );
	}

	public static void applyTableGenerator(
			JaxbTableGenerator jaxbGenerator,
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
			JaxbUuidGenerator jaxbGenerator,
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
			List<JaxbAttributeOverride> jaxbOverrides,
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
			List<JaxbAssociationOverride> jaxbOverrides,
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
			JaxbConvert jaxbConvert,
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
			JaxbTable jaxbTable,
			MutableAnnotationTarget target,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		final DynamicAnnotationUsage<Table> tableAnn = new DynamicAnnotationUsage<>( Table.class, target );
		tableAnn.setAttributeValue( "name", nullIfEmpty( jaxbTable.getName() ) );
		tableAnn.setAttributeValue( "catalog", coalesce(
				nullIfEmpty( jaxbTable.getCatalog() ),
				persistenceUnitMetadata.getDefaultCatalog()
		) );
		tableAnn.setAttributeValue( "schema",  coalesce(
				nullIfEmpty( jaxbTable.getSchema() ),
				persistenceUnitMetadata.getDefaultSchema()
		) );
		// todo : uniqueConstraints
		// todo : indexes
		target.addAnnotationUsage( tableAnn );

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
		if ( jaxbNaturalId == null || jaxbNaturalId.getCache() == null ) {
			return;
		}

		final DynamicAnnotationUsage<NaturalIdCache> annotationUsage = new DynamicAnnotationUsage<>(
				NaturalIdCache.class,
				classDetails
		);
		classDetails.addAnnotationUsage( annotationUsage );

		final JaxbCaching jaxbCaching = jaxbNaturalId.getCache();
		annotationUsage.setAttributeValue( "region", jaxbCaching.getRegion() );
	}

	public static void applyId(
			JaxbId jaxbId,
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
			JaxbEmbeddedId jaxbEmbeddedId,
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
}
