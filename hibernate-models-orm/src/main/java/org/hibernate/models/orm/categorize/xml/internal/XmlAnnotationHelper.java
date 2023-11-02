/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.tree.MutableTreeNode;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.ResultCheckStyle;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAssociationOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributeOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCachingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCascadeTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCheckConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumn;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConvertImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCustomSqlImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbForeignKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGeneratedValueImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbHbmFilterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdClassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIndexImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumn;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallback;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallbackContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLobImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNationalizedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableColumn;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUniqueConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUuidGeneratorImpl;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.models.ModelsException;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.categorize.spi.JpaEventListener;
import org.hibernate.models.orm.categorize.spi.JpaEventListenerStyle;
import org.hibernate.models.orm.categorize.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.source.internal.MutableAnnotationTarget;
import org.hibernate.models.source.internal.MutableAnnotationUsage;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.MethodDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Basic;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

import static jakarta.persistence.FetchType.EAGER;
import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper.getOrMakeAnnotation;
import static org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper.setIf;

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

	public static MutableAnnotationUsage<Column> applyColumn(
			JaxbColumnImpl jaxbColumn,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbColumn == null ) {
			return null;
		}

		final MutableAnnotationUsage<Column> columnAnn = applyTableColumn(
				jaxbColumn,
				memberDetails,
				Column.class,
				buildingContext
		);

		applyCheckConstraints( jaxbColumn.getCheck(), memberDetails, columnAnn );

		setIf( jaxbColumn.getComment(), "comment", columnAnn );

		return columnAnn;
	}

	public static void applyMapKeyColumn(
			JaxbMapKeyColumnImpl jaxbMapKeyColumn,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbMapKeyColumn == null ) {
			return;
		}

		final MutableAnnotationUsage<MapKeyColumn> columnAnn = applyTableColumn(
				jaxbMapKeyColumn,
				memberDetails,
				MapKeyColumn.class,
				buildingContext
		);
	}

	private static <A extends Annotation> MutableAnnotationUsage<A> applyColumn(
			JaxbColumn jaxbColumn,
			MutableAnnotationTarget target,
			Class<A> annotationType,
			SourceModelBuildingContext buildingContext) {
		assert jaxbColumn != null;

		final MutableAnnotationUsage<A> columnAnn = XmlProcessingHelper.getOrMakeAnnotation( annotationType, target );

		setIf( jaxbColumn.getName(), "name", columnAnn );
		setIf( jaxbColumn.isUnique(), "unique", columnAnn );
		setIf( jaxbColumn.isNullable(), "nullable", columnAnn );
		setIf( jaxbColumn.isInsertable(), "insertable", columnAnn );
		setIf( jaxbColumn.isUpdatable(), "updatable", columnAnn );
		setIf( jaxbColumn.getColumnDefinition(), "columnDefinition", columnAnn );
		setIf( jaxbColumn.getOptions(), "options", columnAnn );
		setIf( jaxbColumn.getTable(), "table", columnAnn );

		return columnAnn;
	}

	private static <A extends Annotation> MutableAnnotationUsage<A> applyTableColumn(
			JaxbTableColumn jaxbTableColumn,
			MutableAnnotationTarget target,
			Class<A> annotationType,
			SourceModelBuildingContext buildingContext) {
		assert jaxbTableColumn != null;

		final MutableAnnotationUsage<A> tableColumnAnn = applyColumn( jaxbTableColumn, target, annotationType, buildingContext );
		setIf( jaxbTableColumn.getLength(), "length", tableColumnAnn );
		setIf( jaxbTableColumn.getPrecision(), "precision", tableColumnAnn );
		setIf( jaxbTableColumn.getScale(), "scale", tableColumnAnn );

		return tableColumnAnn;
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

	public static void applyCollectionId(
			JaxbCollectionIdImpl jaxbCollectionId,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbCollectionId == null ) {
			return;
		}

		final MutableAnnotationUsage<CollectionId> collectionIdAnn = XmlProcessingHelper.getOrMakeAnnotation(
				CollectionId.class,
				memberDetails
		);

		final JaxbColumnImpl jaxbColumn = jaxbCollectionId.getColumn();
		final MutableAnnotationUsage<Column> columnAnn = XmlProcessingHelper.getOrMakeAnnotation(
				Column.class,
				memberDetails
		);
		collectionIdAnn.setAttributeValue( "column", columnAnn );
		setIf( jaxbColumn.getName(), "name", columnAnn );
		columnAnn.setAttributeValue( "nullable", false );
		columnAnn.setAttributeValue( "unique", false );
		columnAnn.setAttributeValue( "updatable", false );
		setIf( jaxbColumn.getLength(), "length", columnAnn );
		setIf( jaxbColumn.getPrecision(), "precision", columnAnn );
		setIf( jaxbColumn.getScale(), "scale", columnAnn );
		setIf( jaxbColumn.getTable(), "table", columnAnn );
		setIf( jaxbColumn.getColumnDefinition(), "columnDefinition", columnAnn );

		final JaxbGeneratedValueImpl generator = jaxbCollectionId.getGenerator();
		if ( generator != null ) {
			setIf( generator.getGenerator(), "generator", collectionIdAnn );
		}
	}

	public static void applyTargetClass(
			String name,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final ClassDetails classDetails = resolveJavaType( name, sourceModelBuildingContext );
		final DynamicAnnotationUsage<Target> targetAnn = XmlProcessingHelper.makeAnnotation( Target.class, memberDetails );
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
		applyUniqueConstraints( jaxbGenerator.getUniqueConstraint(), memberDetails, annotationUsage );
		applyIndexes( jaxbGenerator.getIndex(), memberDetails, annotationUsage );
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
		applyAttributeOverrides( jaxbOverrides, memberDetails, null, sourceModelBuildingContext );
	}

	public static void applyAttributeOverrides(
			List<JaxbAttributeOverrideImpl> jaxbOverrides,
			MutableMemberDetails memberDetails,
			String namePrefix,
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
			annotationUsage.setAttributeValue( "name", StringHelper.qualifyConditionally( jaxbOverride.getName(), namePrefix ) );
			annotationUsage.setAttributeValue( "column", applyColumn( jaxbOverride.getColumn(), memberDetails, sourceModelBuildingContext ) );
		} );
	}

	public static void applyAssociationOverrides(
			List<JaxbAssociationOverrideImpl> jaxbOverrides,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		applyAssociationOverrides( jaxbOverrides, memberDetails, null, sourceModelBuildingContext );
	}

	public static void applyAssociationOverrides(
			List<JaxbAssociationOverrideImpl> jaxbOverrides,
			MutableMemberDetails memberDetails,
			String namePrefix,
			SourceModelBuildingContext buildingContext) {
		if ( CollectionHelper.isEmpty( jaxbOverrides ) ) {
			return;
		}

		jaxbOverrides.forEach( (jaxbOverride) -> {
			final MutableAnnotationUsage<AssociationOverride> associationOverrideAnn = getOrMakeAnnotation(
					AssociationOverride.class,
					memberDetails
			);
			associationOverrideAnn.setAttributeValue( "name", StringHelper.qualifyConditionally( jaxbOverride.getName(), namePrefix ) );
			applyJoinColumns( jaxbOverride.getJoinColumn(), associationOverrideAnn, "joinColumns", buildingContext );
			if ( jaxbOverride.getJoinTable() != null ) {
				associationOverrideAnn.setAttributeValue(
						"joinTable",
						applyJoinTable( jaxbOverride.getJoinTable(), null, buildingContext )
				);
			}
			if ( jaxbOverride.getForeignKey() != null ) {
				associationOverrideAnn.setAttributeValue(
						"foreignKey",
						applyForeignKey( jaxbOverride.getForeignKey(), null, buildingContext )
				);
			}
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
		applyConvert( jaxbConvert, memberDetails, null, sourceModelBuildingContext );
	}

	public static void applyConvert(
			JaxbConvertImpl jaxbConvert,
			MutableMemberDetails memberDetails,
			String namePrefix,
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
		annotationUsage.setAttributeValue( "attributeName", StringHelper.qualifyConditionally( jaxbConvert.getAttributeName(), namePrefix ) );
		annotationUsage.setAttributeValue( "disableConversion", jaxbConvert.isDisableConversion() );
	}

	public static void applyCascade(
			JaxbCascadeTypeImpl jaxbCascadeType,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbCascadeType == null ) {
			return;
		}

		// We always use Hibernate specific org.hibernate.annotations.CascadeType since
		// it offers additional options than jakarta.persistence.CascadeType
		final List<CascadeType> cascadeTypes = new ArrayList<>();
		if ( jaxbCascadeType.getCascadeAll() != null ) {
			cascadeTypes.add( CascadeType.ALL );
		}
		if ( jaxbCascadeType.getCascadePersist() != null ) {
			cascadeTypes.add( CascadeType.PERSIST );
		}
		if ( jaxbCascadeType.getCascadeMerge() != null ) {
			cascadeTypes.add( CascadeType.MERGE );
		}
		if ( jaxbCascadeType.getCascadeRemove() != null ) {
			cascadeTypes.add( CascadeType.REMOVE );
		}
		if ( jaxbCascadeType.getCascadeRefresh() != null ) {
			cascadeTypes.add( CascadeType.REFRESH );
		}
		if ( jaxbCascadeType.getCascadeDetach() != null ) {
			cascadeTypes.add( CascadeType.DETACH );
		}
		if ( jaxbCascadeType.getCascadeDelete() != null ) {
			cascadeTypes.add( CascadeType.DELETE );
		}
		if ( jaxbCascadeType.getCascadeSaveUpdate() != null ) {
			cascadeTypes.add( CascadeType.SAVE_UPDATE );
		}
		if ( jaxbCascadeType.getCascadeReplicate() != null ) {
			cascadeTypes.add( CascadeType.REPLICATE );
		}
		if ( jaxbCascadeType.getCascadeLock() != null ) {
			cascadeTypes.add( CascadeType.LOCK );
		}

		if ( !cascadeTypes.isEmpty() ) {
			getOrMakeAnnotation( Cascade.class, memberDetails ).setAttributeValue( "value", cascadeTypes.toArray(new CascadeType[0]) );
		}
	}

	public static void applyTable(
			JaxbTableImpl jaxbTable,
			MutableAnnotationTarget target,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		if ( jaxbTable == null ) {
			return;
		}

		final MutableAnnotationUsage<Table> tableAnn = getOrMakeAnnotation( Table.class, target );

		applyTableAttributes( tableAnn, jaxbTable, persistenceUnitMetadata );

		applyUniqueConstraints( jaxbTable.getUniqueConstraint(), target, tableAnn );

		applyCheckConstraints( jaxbTable.getCheck(), target, tableAnn );

		applyIndexes( jaxbTable.getIndex(), target, tableAnn );
	}

	public static void applyTableOverride(
			JaxbTableImpl jaxbTable,
			MutableAnnotationTarget target,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		if ( jaxbTable == null ) {
			return;
		}

		final MutableAnnotationUsage<Table> tableAnn = getOrMakeAnnotation( Table.class, target );

		applyTableAttributes( tableAnn, jaxbTable, persistenceUnitMetadata );

		applyUniqueConstraints( jaxbTable.getUniqueConstraint(), target, tableAnn );

		applyCheckConstraints( jaxbTable.getCheck(), target, tableAnn );

		applyIndexes( jaxbTable.getIndex(), target, tableAnn );
	}

	private static void applyTableAttributes(
			MutableAnnotationUsage<Table> tableAnn,
			JaxbTableImpl jaxbTable,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		applyAttributeIfSpecified( tableAnn, "name", jaxbTable.getName() );
		applyAttributeIfSpecified( tableAnn, "catalog", jaxbTable.getCatalog(), persistenceUnitMetadata.getDefaultCatalog() );
		applyAttributeIfSpecified( tableAnn, "schema", jaxbTable.getSchema(), persistenceUnitMetadata.getDefaultSchema() );
		applyAttributeIfSpecified( tableAnn, "options", jaxbTable.getOptions() );
		applyAttributeIfSpecified( tableAnn, "comment", jaxbTable.getComment() );
	}

	private static <A extends Annotation> void applyUniqueConstraints(
			List<JaxbUniqueConstraintImpl> jaxbUniqueConstraints,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage) {
		if ( CollectionHelper.isEmpty( jaxbUniqueConstraints ) ) {
			return;
		}

		final List<AnnotationUsage<UniqueConstraint>> uniqueConstraints = new ArrayList<>( jaxbUniqueConstraints.size() );
		jaxbUniqueConstraints.forEach( jaxbUniqueConstraint -> {
			final MutableAnnotationUsage<UniqueConstraint> uniqueConstraintAnn = getOrMakeAnnotation(
					UniqueConstraint.class,
					target
			);
			setIf( jaxbUniqueConstraint.getName(), "name", uniqueConstraintAnn );
			uniqueConstraintAnn.setAttributeValue( "columnNames", jaxbUniqueConstraint.getColumnName().toArray( new String[0] ) );
			uniqueConstraints.add( uniqueConstraintAnn );
		} );

		annotationUsage.setAttributeValue( "uniqueConstraints", uniqueConstraints.toArray( new AnnotationUsage[0] ) );
	}

	private static <A extends Annotation> void applyIndexes(
			List<JaxbIndexImpl> jaxbIndexes,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage) {
		if ( CollectionHelper.isEmpty( jaxbIndexes ) ) {
			return;
		}

		final List<AnnotationUsage<Index>> indexes = new ArrayList<>( jaxbIndexes.size() );
		jaxbIndexes.forEach( jaxbIndex -> {
			final MutableAnnotationUsage<Index> indexAnn = getOrMakeAnnotation(
					Index.class,
					target
			);
			setIf( jaxbIndex.getName(), "name", indexAnn );
			setIf( jaxbIndex.getColumnList(), "columnList", indexAnn );
			setIf( jaxbIndex.isUnique(), "unique", indexAnn );
			indexes.add( indexAnn );
		} );

		annotationUsage.setAttributeValue( "indexes", indexes.toArray( new AnnotationUsage[0] ) );
	}

	public static MutableAnnotationUsage<JoinTable> applyJoinTable(
			JaxbJoinTableImpl jaxbJoinTable,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbJoinTable == null ) {
			return null;
		}

		final MutableAnnotationUsage<JoinTable> joinTableAnn = getOrMakeAnnotation(
				JoinTable.class,
				memberDetails
		);

		setIf( jaxbJoinTable.getName(), "name", joinTableAnn );
		setIf( jaxbJoinTable.getCatalog(), "catalog", joinTableAnn );
		setIf( jaxbJoinTable.getSchema(), "schema", joinTableAnn );
		setIf( jaxbJoinTable.getOptions(), "options", joinTableAnn );
		setIf( jaxbJoinTable.getComment(), "comment", joinTableAnn );

		applyJoinColumns( jaxbJoinTable.getJoinColumn(), joinTableAnn, "joinColumns", buildingContext );
		applyJoinColumns( jaxbJoinTable.getInverseJoinColumn(), joinTableAnn, "inverseJoinColumns", buildingContext );

		if ( jaxbJoinTable.getForeignKey() != null ) {
			joinTableAnn.setAttributeValue(
					"foreignKey",
					applyForeignKey( jaxbJoinTable.getForeignKey(), null, buildingContext )
			);
		}
		if ( jaxbJoinTable.getInverseForeignKey() != null ) {
			joinTableAnn.setAttributeValue(
					"inverseForeignKey",
					applyForeignKey( jaxbJoinTable.getInverseForeignKey(), null, buildingContext )
			);
		}

		applyCheckConstraints( jaxbJoinTable.getCheck(), memberDetails, joinTableAnn );

		applyUniqueConstraints( jaxbJoinTable.getUniqueConstraint(), memberDetails, joinTableAnn );

		applyIndexes( jaxbJoinTable.getIndex(), memberDetails, joinTableAnn );

		return joinTableAnn;
	}

	private static <A extends Annotation> void applyJoinColumns(
			List<JaxbJoinColumnImpl> jaxbJoinColumns,
			MutableAnnotationUsage<A> annotationUsage,
			String attributeName,
			SourceModelBuildingContext buildingContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return;
		}
		final List<AnnotationUsage<JoinColumn>> joinColumns = new ArrayList<>( jaxbJoinColumns.size() );
		jaxbJoinColumns.forEach( jaxbJoinColumn -> {
			joinColumns.add( applyJoinColumn( jaxbJoinColumn, null, buildingContext ) );
		} );
		annotationUsage.setAttributeValue( attributeName, joinColumns.toArray( new AnnotationUsage[0] ) );
	}

	public static void applyCollectionTable(
			JaxbCollectionTableImpl jaxbCollectionTable,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbCollectionTable == null ) {
			return;
		}

		final MutableAnnotationUsage<CollectionTable> collectionTableAnn = getOrMakeAnnotation(
				CollectionTable.class,
				memberDetails
		);

		setIf( jaxbCollectionTable.getName(), "name", collectionTableAnn );
		setIf( jaxbCollectionTable.getCatalog(), "catalog", collectionTableAnn );
		setIf( jaxbCollectionTable.getSchema(), "schema", collectionTableAnn );
		setIf( jaxbCollectionTable.getOptions(), "options", collectionTableAnn );

		applyJoinColumns( jaxbCollectionTable.getJoinColumn(), collectionTableAnn, "joinColumns", buildingContext );

		if ( jaxbCollectionTable.getForeignKey() != null ) {
			collectionTableAnn.setAttributeValue(
					"foreignKey",
					applyForeignKey( jaxbCollectionTable.getForeignKey(), null, buildingContext )
			);
		}

		applyUniqueConstraints( jaxbCollectionTable.getUniqueConstraint(), memberDetails, collectionTableAnn );

		applyIndexes( jaxbCollectionTable.getIndex(), memberDetails, collectionTableAnn );
	}


	public static MutableAnnotationUsage<JoinColumn> applyJoinColumn(
			JaxbJoinColumnImpl jaxbJoinColumn,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbJoinColumn == null ) {
			return null;
		}

		final MutableAnnotationUsage<JoinColumn> joinColumnAnn = applyJoinColumn(
				jaxbJoinColumn,
				memberDetails,
				JoinColumn.class,
				buildingContext
		);
		setIf( jaxbJoinColumn.getReferencedColumnName(), "referencedColumnName", joinColumnAnn );

		if ( jaxbJoinColumn.getForeignKey() != null ) {
			joinColumnAnn.setAttributeValue(
					"foreignKey",
					applyForeignKey( jaxbJoinColumn.getForeignKey(), null, buildingContext )
			);
		}

		applyCheckConstraints( jaxbJoinColumn.getCheckConstraint(), memberDetails, joinColumnAnn );

		return joinColumnAnn;
	}

	public static void applyMapKeyJoinColumn(
			JaxbMapKeyJoinColumnImpl jaxbMapKeyJoinColumn,
			MutableMemberDetails memberDetails,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbMapKeyJoinColumn == null ) {
			return;
		}

		final MutableAnnotationUsage<MapKeyJoinColumn> joinColumnAnn = applyJoinColumn(
				jaxbMapKeyJoinColumn,
				memberDetails,
				MapKeyJoinColumn.class,
				buildingContext
		);
	}

	private static <A extends Annotation> MutableAnnotationUsage<A> applyJoinColumn(
			JaxbJoinColumn jaxbJoinColumn,
			MutableAnnotationTarget target,
			Class<A> annotationType,
			SourceModelBuildingContext buildingContext) {
		assert jaxbJoinColumn != null;

		final MutableAnnotationUsage<A> joinColumnAnn = applyColumn( jaxbJoinColumn, target, annotationType, buildingContext );
		setIf( jaxbJoinColumn.getReferencedColumnName(), "referencedColumnName", joinColumnAnn );
		if ( jaxbJoinColumn.getForeignKey() != null ) {
			joinColumnAnn.setAttributeValue(
					"foreignKey",
					applyForeignKey( jaxbJoinColumn.getForeignKey(), null, buildingContext )
			);
		}

		return joinColumnAnn;
	}

	public static MutableAnnotationUsage<ForeignKey> applyForeignKey(
			JaxbForeignKeyImpl jaxbForeignKey,
			MutableMemberDetails target,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbForeignKey == null ) {
			return null;
		}

		final MutableAnnotationUsage<ForeignKey> foreignKeyAnn = getOrMakeAnnotation( ForeignKey.class, target );
		setIf( jaxbForeignKey.getName(), "name", foreignKeyAnn );
		setIf( jaxbForeignKey.getConstraintMode(), "value", foreignKeyAnn );
		setIf( jaxbForeignKey.getForeignKeyDefinition(), "foreignKeyDefinition", foreignKeyAnn );

		return foreignKeyAnn;
	}

	public static <A extends Annotation> void applyCheckConstraints(
			List<JaxbCheckConstraintImpl> jaxbCheckConstraints,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage) {
		if ( CollectionHelper.isEmpty( jaxbCheckConstraints ) ) {
			return;
		}

		final List<AnnotationUsage<CheckConstraint>> checkConstraints = new ArrayList<>( jaxbCheckConstraints.size() );
		jaxbCheckConstraints.forEach( jaxbCheckConstraint -> {
			final MutableAnnotationUsage<CheckConstraint> checkConstraintAnn = getOrMakeAnnotation(
					CheckConstraint.class,
					target
			);
			setIf( jaxbCheckConstraint.getName(), "name", checkConstraintAnn );
			setIf( jaxbCheckConstraint.getConstraint(), "constraint", checkConstraintAnn );
			setIf( jaxbCheckConstraint.getOptions(), "options", checkConstraintAnn );
			checkConstraints.add( checkConstraintAnn );
		} );

		annotationUsage.setAttributeValue( "check", checkConstraints.toArray( new AnnotationUsage[0] ) );
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

		final MutableAnnotationUsage<Inheritance> inheritanceAnn = XmlProcessingHelper.getOrMakeAnnotation(
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
		final DynamicAnnotationUsage<JdbcType> jdbcTypeAnn = XmlProcessingHelper.makeAnnotation( JdbcType.class, memberDetails );
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

	static void applyFilter(
			JaxbHbmFilterImpl jaxbFilter,
			MutableAnnotationTarget target,
			SourceModelBuildingContext sourceModelBuildingContext) {
		applyFilter( jaxbFilter, target, Filter.class, sourceModelBuildingContext );
	}

	static void applyJoinTableFilter(
			JaxbHbmFilterImpl jaxbFilter,
			MutableAnnotationTarget target,
			SourceModelBuildingContext sourceModelBuildingContext) {
		applyFilter( jaxbFilter, target, FilterJoinTable.class, sourceModelBuildingContext );
	}

	private static <F extends Annotation> void applyFilter(
			JaxbHbmFilterImpl jaxbFilter,
			MutableAnnotationTarget target,
			Class<F> filterAnnotationClass,
			SourceModelBuildingContext buildingContext) {
		// Since @Filter and @FilterJoinTable have exactly the same attributes,
		// we can use the same method with parametrized annotation class
		final MutableAnnotationUsage<F> filterAnn = XmlProcessingHelper.getOrMakeNamedAnnotation(
				filterAnnotationClass,
				jaxbFilter.getName(),
				target
		);

		applyAttributeIfSpecified( filterAnn, "condition", jaxbFilter.getCondition() );
		applyAttributeIfSpecified( filterAnn, "deduceAliasInjectionPoints", jaxbFilter.isAutoAliasInjection() );

		final List<JaxbHbmFilterImpl.JaxbAliasesImpl> aliases = jaxbFilter.getAliases();
		if ( !CollectionHelper.isEmpty( aliases ) ) {
			filterAnn.setAttributeValue( "aliases", getSqlFragmentAliases( aliases, buildingContext ) );
		}
	}

	private static List<AnnotationUsage<SqlFragmentAlias>> getSqlFragmentAliases(
			List<JaxbHbmFilterImpl.JaxbAliasesImpl> aliases,
			SourceModelBuildingContext buildingContext) {
		final List<AnnotationUsage<SqlFragmentAlias>> sqlFragmentAliases = new ArrayList<>( aliases.size() );
		for ( JaxbHbmFilterImpl.JaxbAliasesImpl alias : aliases ) {
			final MutableAnnotationUsage<SqlFragmentAlias> aliasAnn = new DynamicAnnotationUsage<>( SqlFragmentAlias.class );
			aliasAnn.setAttributeValue( "alias", alias.getAlias() );
			applyAttributeIfSpecified( aliasAnn, "table", alias.getTable() );
			if ( StringHelper.isNotEmpty( alias.getEntity() ) ) {
				aliasAnn.setAttributeValue(
						"entity",
						buildingContext.getClassDetailsRegistry().resolveClassDetails( alias.getEntity() )
				);
			}
			sqlFragmentAliases.add( aliasAnn );
		}
		return sqlFragmentAliases;
	}

	static void applySqlRestriction(
			String sqlRestriction,
			MutableAnnotationTarget target,
			SourceModelBuildingContext buildingContext) {
		applySqlRestriction( sqlRestriction, target, SQLRestriction.class, buildingContext );
	}

	static void applySqlJoinTableRestriction(
			String sqlJoinTableRestriction,
			MutableAnnotationTarget target,
			SourceModelBuildingContext buildingContext) {
		applySqlRestriction( sqlJoinTableRestriction, target, SQLJoinTableRestriction.class, buildingContext );
	}

	private static <A extends Annotation> void applySqlRestriction(
			String sqlRestriction,
			MutableAnnotationTarget target,
			Class<A> annotationType,
			SourceModelBuildingContext buildingContext) {
		if ( StringHelper.isNotEmpty( sqlRestriction ) ) {
			final MutableAnnotationUsage<A> annotation = getOrMakeAnnotation( annotationType, target );
			annotation.setAttributeValue( "value", sqlRestriction );
		}
	}

	static <A extends Annotation> void applyCustomSql(
			JaxbCustomSqlImpl jaxbCustomSql,
			MutableAnnotationTarget target,
			Class<A> annotationType,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbCustomSql != null ) {
			final MutableAnnotationUsage<A> annotation = getOrMakeAnnotation( annotationType, target );
			annotation.setAttributeValue( "sql", jaxbCustomSql.getValue() );
			annotation.setAttributeValue( "callable", jaxbCustomSql.isCallable() );
			applyAttributeIfSpecified( annotation, "table", jaxbCustomSql.getTable() );
			if ( jaxbCustomSql.getCheck() != null ) {
				annotation.setAttributeValue( "check", getResultCheckStyle( jaxbCustomSql.getCheck() ) );
			}
		}
	}

	private static ResultCheckStyle getResultCheckStyle(ExecuteUpdateResultCheckStyle style) {
		switch ( style ) {
			case NONE:
				return ResultCheckStyle.NONE;
			case COUNT:
				return ResultCheckStyle.COUNT;
			case PARAM:
				return ResultCheckStyle.PARAM;
			default:
				return null;
		}
	}

	static void applyIdClass(
			JaxbIdClassImpl jaxbIdClass,
			MutableClassDetails target,
			SourceModelBuildingContext buildingContext) {
		if ( jaxbIdClass != null ) {
			getOrMakeAnnotation( IdClass.class, target ).setAttributeValue(
					"value",
					buildingContext.getClassDetailsRegistry().resolveClassDetails( jaxbIdClass.getClazz() )
			);
		}
	}

	static void applyEntityListener(
			JaxbEntityListenerImpl jaxbEntityListener,
			MutableClassDetails classDetails,
			SourceModelBuildingContext buildingContext) {
		final MutableAnnotationUsage<EntityListeners> entityListeners = XmlProcessingHelper.getOrMakeAnnotation(
				EntityListeners.class,
				classDetails
		);
		final MutableClassDetails entityListenerClass = (MutableClassDetails) buildingContext.getClassDetailsRegistry()
				.resolveClassDetails( jaxbEntityListener.getClazz() );
		applyLifecycleCallbacks(
				jaxbEntityListener,
				JpaEventListenerStyle.LISTENER,
				entityListenerClass,
				buildingContext
		);
		final List<ClassDetails> values = entityListeners.getAttributeValue( "value" );
		if ( values != null ) {
			values.add( entityListenerClass );
		}
		else {
			entityListeners.setAttributeValue( "value", new ArrayList<>( List.of( entityListenerClass ) ) );
		}
	}
	
	static void applyLifecycleCallbacks(
			JaxbLifecycleCallbackContainer lifecycleCallbackContainer,
			JpaEventListenerStyle callbackType,
			MutableClassDetails classDetails,
			SourceModelBuildingContext buildingContext) {
		applyLifecycleCallback( lifecycleCallbackContainer.getPrePersist(), callbackType, PrePersist.class, classDetails );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostPersist(), callbackType, PostPersist.class, classDetails );
		applyLifecycleCallback( lifecycleCallbackContainer.getPreRemove(), callbackType, PreRemove.class, classDetails );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostRemove(), callbackType, PostRemove.class, classDetails );
		applyLifecycleCallback( lifecycleCallbackContainer.getPreUpdate(), callbackType, PreUpdate.class, classDetails );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostUpdate(), callbackType, PostUpdate.class, classDetails );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostLoad(), callbackType, PostLoad.class, classDetails );
	}

	private static <A extends Annotation> void applyLifecycleCallback(
			JaxbLifecycleCallback lifecycleCallback,
			JpaEventListenerStyle callbackType,
			Class<A> lifecycleAnnotation,
			MutableClassDetails classDetails) {
		if ( lifecycleCallback != null ) {
			final MethodDetails methodDetails = getCallbackMethodDetails(
					lifecycleCallback.getMethodName(),
					callbackType,
					classDetails
			);
			if ( methodDetails == null ) {
				throw new AnnotationException( String.format(
						"Lifecycle callback method not found - %s (%s)",
						lifecycleCallback.getMethodName(),
						classDetails.getName()
				) );
			}
			XmlProcessingHelper.makeAnnotation( lifecycleAnnotation, (MutableMemberDetails) methodDetails );
		}
	}

	private static MethodDetails getCallbackMethodDetails(
			String name,
			JpaEventListenerStyle callbackType,
			ClassDetails classDetails) {
		for ( MethodDetails method : classDetails.getMethods() ) {
			if ( method.getName().equals( name ) && JpaEventListener.matchesSignature( callbackType, method ) ) {
				return method;
			}
		}
		return null;
	}

	static void applyRowId(
			String rowId,
			MutableClassDetails target,
			SourceModelBuildingContext buildingContext) {
		if ( rowId != null ) {
			final MutableAnnotationUsage<RowId> rowIdAnn = XmlProcessingHelper.getOrMakeAnnotation( RowId.class, target );
			applyAttributeIfSpecified( rowIdAnn, "value", rowId );
		}
	}
}
