/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
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
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCachingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCascadeTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCheckConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConvertImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCustomSqlImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDiscriminatorColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDiscriminatorColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbForeignKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGeneratedValueImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbHbmFilterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdClassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIndexImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallback;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallbackContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLobImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedAttributeNodeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedEntityGraphImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedSubgraphImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNationalizedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUniqueConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUuidGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbCheckable;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnJoined;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbTableMapping;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.categorize.xml.internal.db.ColumnProcessing;
import org.hibernate.boot.models.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.ModelsException;
import org.hibernate.models.internal.MutableAnnotationTarget;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
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
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
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

import static java.util.Collections.emptyList;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

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
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Entity> entityUsage = XmlProcessingHelper.getOrMakeAnnotation( Entity.class, classDetails, xmlDocumentContext );
		if ( StringHelper.isNotEmpty( jaxbEntity.getName() ) ) {
			entityUsage.setAttributeValue( "name", jaxbEntity.getName() );
		}
	}

	public static MutableAnnotationUsage<Access> createAccessAnnotation(
			AccessType accessType,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Access> annotationUsage = XmlProcessingHelper.makeAnnotation( Access.class, target, xmlDocumentContext );
		annotationUsage.setAttributeValue( "value", accessType );
		return annotationUsage;
	}

	public static void applyAttributeAccessor(
			String attributeAccessor,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<AttributeAccessor> accessorAnn = XmlProcessingHelper.makeAnnotation( AttributeAccessor.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( accessorAnn );
		// todo : this is the old, deprecated form
		accessorAnn.setAttributeValue( "value", attributeAccessor );
	}

	public static void applyColumn(
			JaxbColumnImpl jaxbColumn,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbColumn == null ) {
			return;
		}

		createColumnAnnotation( jaxbColumn, memberDetails, xmlDocumentContext );
	}

	public static MutableAnnotationUsage<JoinColumn> applyJoinColumn(
			JaxbJoinColumnImpl jaxbJoinColumn,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbJoinColumn == null ) {
			return null;
		}

		return createJoinColumnAnnotation( jaxbJoinColumn, memberDetails, JoinColumn.class, xmlDocumentContext );
	}

	public static <A extends Annotation> MutableAnnotationUsage<A> createJoinColumnAnnotation(
			JaxbColumnJoined jaxbJoinColumn,
			MutableMemberDetails memberDetails,
			Class<A> annotationType,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<A> joinColumnAnn = XmlProcessingHelper.getOrMakeAnnotation( annotationType, memberDetails, xmlDocumentContext );
		final AnnotationDescriptor<A> joinColumnDescriptor = xmlDocumentContext
				.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( annotationType );

		ColumnProcessing.applyColumnDetails( jaxbJoinColumn, memberDetails, joinColumnAnn, xmlDocumentContext );

		applyOr( jaxbJoinColumn, JaxbColumnJoined::getReferencedColumnName, "referencedColumnName", joinColumnAnn, joinColumnDescriptor );

		final JaxbForeignKeyImpl jaxbForeignKey = jaxbJoinColumn.getForeignKey();
		if ( jaxbForeignKey != null ) {
			joinColumnAnn.setAttributeValue(
					"foreignKey",
					createForeignKeyAnnotation( jaxbForeignKey, memberDetails, xmlDocumentContext )
			);
		}

		return joinColumnAnn;
	}

	public static void applyJoinColumns(
			List<JaxbJoinColumnImpl> jaxbJoinColumns,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return;
		}

		if ( jaxbJoinColumns.size() == 1 ) {
			XmlAnnotationHelper.applyJoinColumn( jaxbJoinColumns.get( 0 ), memberDetails, xmlDocumentContext );
		}
		else {
			final MutableAnnotationUsage<JoinColumns> columnsAnn = XmlProcessingHelper.makeAnnotation(
					JoinColumns.class,
					memberDetails,
					xmlDocumentContext
			);
			columnsAnn.setAttributeValue( "value", createJoinColumns( jaxbJoinColumns, memberDetails, xmlDocumentContext ) );
		}
	}

	public static List<AnnotationUsage<JoinColumn>> createJoinColumns(
			List<JaxbJoinColumnImpl> jaxbJoinColumns,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return Collections.emptyList();
		}
		final List<AnnotationUsage<JoinColumn>> joinColumns = new ArrayList<>( jaxbJoinColumns.size() );
		jaxbJoinColumns.forEach( jaxbJoinColumn -> {
			joinColumns.add( applyJoinColumn( jaxbJoinColumn, memberDetails, xmlDocumentContext ) );
		} );
		return joinColumns;
	}

	public static <T,N> void applyOr(
			N jaxbNode,
			Function<N,T> jaxbValueAccess,
			String name,
			KeyedConsumer<String, T> valueConsumer,
			Supplier<T> defaultValueProvider) {
		if ( jaxbNode != null ) {
			final T value = jaxbValueAccess.apply( jaxbNode );
			if ( value != null ) {
				valueConsumer.accept( name, value );
				return;
			}
		}

		valueConsumer.accept( name, defaultValueProvider.get() );
	}

	public static <T,N,A extends Annotation> void applyOr(
			N jaxbNode,
			Function<N,T> jaxbValueAccess,
			String name,
			MutableAnnotationUsage<A> annotationUsage,
			AnnotationDescriptor<A> annotationDescriptor) {
		//noinspection unchecked
		applyOr(
				jaxbNode,
				jaxbValueAccess,
				name,
				(key, value) -> annotationUsage.setAttributeValue( name, value ),
				() -> (T) annotationDescriptor.getAttribute( name ).getAttributeMethod().getDefaultValue()
		);
	}

	private static MutableAnnotationUsage<Column> createColumnAnnotation(
			JaxbColumnImpl jaxbColumn,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Column> columnAnn = XmlProcessingHelper.getOrMakeAnnotation( Column.class, target, xmlDocumentContext );

		ColumnProcessing.applyColumnDetails( jaxbColumn, target, columnAnn, xmlDocumentContext );

		return columnAnn;
	}

	public static void applyUserType(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbType == null ) {
			return;
		}

		final MutableAnnotationUsage<Type> typeAnn = XmlProcessingHelper.makeAnnotation( Type.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( typeAnn );

		final ClassDetails userTypeImpl = resolveJavaType( jaxbType.getValue(), xmlDocumentContext );
		typeAnn.setAttributeValue( "value", userTypeImpl );
		typeAnn.setAttributeValue( "parameters", collectParameters( jaxbType.getParameters(), memberDetails, xmlDocumentContext ) );
	}

	public static List<AnnotationUsage<Parameter>> collectParameters(
			List<JaxbConfigurationParameterImpl> jaxbParameters,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbParameters ) ) {
			return emptyList();
		}

		List<AnnotationUsage<Parameter>> parameterAnnList = new ArrayList<>( jaxbParameters.size() );
		jaxbParameters.forEach( (jaxbParam) -> {
			final MutableAnnotationUsage<Parameter> annotationUsage = XmlProcessingHelper.makeNestedAnnotation( Parameter.class, target, xmlDocumentContext );
			parameterAnnList.add( annotationUsage );
			annotationUsage.setAttributeValue( "name", jaxbParam.getName() );
			annotationUsage.setAttributeValue( "value", jaxbParam.getValue() );
		} );
		return parameterAnnList;
	}

	public static void applyCollectionUserType(
			JaxbCollectionUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbType == null ) {
			return;
		}

		final MutableAnnotationUsage<CollectionType> typeAnn = XmlProcessingHelper.makeAnnotation( CollectionType.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( typeAnn );

		final ClassDetails userTypeImpl = resolveJavaType( jaxbType.getType(), xmlDocumentContext );
		typeAnn.setAttributeValue( "type", userTypeImpl );
		typeAnn.setAttributeValue( "parameters", collectParameters( jaxbType.getParameters(), memberDetails, xmlDocumentContext ) );
	}

	public static void applyCollectionId(
			JaxbCollectionIdImpl jaxbCollectionId,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCollectionId == null ) {
			return;
		}

		final MutableAnnotationUsage<CollectionId> collectionIdAnn = XmlProcessingHelper.getOrMakeAnnotation( CollectionId.class, memberDetails, xmlDocumentContext );
		final AnnotationDescriptor<CollectionId> collectionIdDescriptor = xmlDocumentContext
				.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( CollectionId.class );

		final JaxbColumnImpl jaxbColumn = jaxbCollectionId.getColumn();
		if ( jaxbColumn != null ) {
			collectionIdAnn.setAttributeValue( "column", createColumnAnnotation(
					jaxbColumn,
					memberDetails,
					xmlDocumentContext
			) );
		}

		applyOr( jaxbCollectionId, JaxbCollectionIdImpl::getGenerator, "generator", collectionIdAnn, collectionIdDescriptor );
	}

	public static void applyForeignKey(
			JaxbForeignKeyImpl jaxbForeignKey,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbForeignKey == null ) {
			return;
		}

		createForeignKeyAnnotation( jaxbForeignKey, memberDetails, xmlDocumentContext );
	}

	public static MutableAnnotationUsage<ForeignKey> createForeignKeyAnnotation(
			JaxbForeignKeyImpl jaxbForeignKey,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<ForeignKey> foreignKeyAnn = XmlProcessingHelper.getOrMakeAnnotation( ForeignKey.class, memberDetails, xmlDocumentContext );
		final AnnotationDescriptor<ForeignKey> foreignKeyDescriptor = xmlDocumentContext
				.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( ForeignKey.class );

		applyOr( jaxbForeignKey, JaxbForeignKeyImpl::getName, "name", foreignKeyAnn, foreignKeyDescriptor );
		applyOr( jaxbForeignKey, JaxbForeignKeyImpl::getConstraintMode, "value", foreignKeyAnn, foreignKeyDescriptor );
		applyOr( jaxbForeignKey, JaxbForeignKeyImpl::getForeignKeyDefinition, "foreignKeyDefinition", foreignKeyAnn, foreignKeyDescriptor );
		applyOr( jaxbForeignKey, JaxbForeignKeyImpl::getOptions, "options", foreignKeyAnn, foreignKeyDescriptor );

		return foreignKeyAnn;
	}

	public static void applyMapKeyColumn(
			JaxbMapKeyColumnImpl jaxbMapKeyColumn,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbMapKeyColumn == null ) {
			return;
		}

		final MutableAnnotationUsage<MapKeyColumn> columnAnn = XmlProcessingHelper.getOrMakeAnnotation( MapKeyColumn.class, memberDetails, xmlDocumentContext );

		ColumnProcessing.applyColumnDetails( jaxbMapKeyColumn, memberDetails, columnAnn, xmlDocumentContext );
	}

	public static void applyMapKeyJoinColumn(
			JaxbMapKeyJoinColumnImpl jaxbMapKeyJoinColumn,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbMapKeyJoinColumn == null ) {
			return;
		}

		createJoinColumnAnnotation( jaxbMapKeyJoinColumn, memberDetails, MapKeyJoinColumn.class, xmlDocumentContext );
	}

	public static void applyCascading(
			JaxbCascadeTypeImpl jaxbCascadeType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
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
		if ( jaxbCascadeType.getCascadeReplicate() != null ) {
			//noinspection deprecation
			cascadeTypes.add( CascadeType.REPLICATE );
		}
		if ( jaxbCascadeType.getCascadeLock() != null ) {
			cascadeTypes.add( CascadeType.LOCK );
		}

		if ( !cascadeTypes.isEmpty() ) {
			XmlProcessingHelper.getOrMakeAnnotation( Cascade.class, memberDetails, xmlDocumentContext )
					.setAttributeValue( "value", cascadeTypes );
		}
	}

	public static <A extends Annotation> void applyUniqueConstraints(
			List<JaxbUniqueConstraintImpl> jaxbUniqueConstraints,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbUniqueConstraints ) ) {
			return;
		}

		final List<AnnotationUsage<UniqueConstraint>> uniqueConstraints = new ArrayList<>( jaxbUniqueConstraints.size() );
		jaxbUniqueConstraints.forEach( jaxbUniqueConstraint -> {
			final MutableAnnotationUsage<UniqueConstraint> uniqueConstraintAnn = XmlProcessingHelper.getOrMakeAnnotation(
					UniqueConstraint.class,
					target,
					xmlDocumentContext
			);
			final AnnotationDescriptor<UniqueConstraint> uniqueConstraintDescriptor = xmlDocumentContext.getModelBuildingContext()
					.getAnnotationDescriptorRegistry()
					.getDescriptor( UniqueConstraint.class );
			applyOr( jaxbUniqueConstraint, JaxbUniqueConstraintImpl::getName, "name", uniqueConstraintAnn, uniqueConstraintDescriptor );
			uniqueConstraintAnn.setAttributeValue( "columnNames", jaxbUniqueConstraint.getColumnName() );
			uniqueConstraints.add( uniqueConstraintAnn );
		} );

		annotationUsage.setAttributeValue( "uniqueConstraints", uniqueConstraints );
	}

	public static <A extends Annotation> void applyIndexes(
			List<JaxbIndexImpl> jaxbIndexes,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbIndexes ) ) {
			return;
		}

		final List<AnnotationUsage<Index>> indexes = new ArrayList<>( jaxbIndexes.size() );
		jaxbIndexes.forEach( jaxbIndex -> {
			final MutableAnnotationUsage<Index> indexAnn = XmlProcessingHelper.getOrMakeAnnotation(
					Index.class,
					target,
					xmlDocumentContext
			);
			final AnnotationDescriptor<Index> indexDescriptor = xmlDocumentContext.getModelBuildingContext()
					.getAnnotationDescriptorRegistry()
					.getDescriptor( Index.class );
			applyOr( jaxbIndex, JaxbIndexImpl::getName, "name", indexAnn, indexDescriptor );
			applyOr( jaxbIndex, JaxbIndexImpl::getColumnList, "columnList", indexAnn, indexDescriptor );
			applyOr( jaxbIndex, JaxbIndexImpl::isUnique, "unique", indexAnn, indexDescriptor );
			indexes.add( indexAnn );
		} );

		annotationUsage.setAttributeValue( "indexes", indexes );
	}

	public static MutableAnnotationUsage<JoinTable> applyJoinTable(
			JaxbJoinTableImpl jaxbJoinTable,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbJoinTable == null ) {
			return null;
		}

		final MutableAnnotationUsage<JoinTable> joinTableAnn = XmlProcessingHelper.getOrMakeAnnotation(
				JoinTable.class,
				memberDetails,
				xmlDocumentContext
		);
		final AnnotationDescriptor<JoinTable> joinTableDescriptor = xmlDocumentContext.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( JoinTable.class );

		applyOr( jaxbJoinTable, JaxbJoinTableImpl::getName, "name", joinTableAnn, joinTableDescriptor );
		applyTableAttributes( jaxbJoinTable, memberDetails, joinTableAnn, joinTableDescriptor, xmlDocumentContext );

		final List<JaxbJoinColumnImpl> joinColumns = jaxbJoinTable.getJoinColumn();
		if ( CollectionHelper.isNotEmpty( joinColumns ) ) {
			joinTableAnn.setAttributeValue( "joinColumns", createJoinColumns( joinColumns, memberDetails, xmlDocumentContext ) );
		}

		final List<JaxbJoinColumnImpl> inverseJoinColumns = jaxbJoinTable.getInverseJoinColumn();
		if ( CollectionHelper.isNotEmpty( inverseJoinColumns ) ) {
			joinTableAnn.setAttributeValue( "inverseJoinColumns", createJoinColumns( inverseJoinColumns, memberDetails, xmlDocumentContext ) );
		}

		if ( jaxbJoinTable.getForeignKey() != null ) {
			joinTableAnn.setAttributeValue(
					"foreignKey",
					createForeignKeyAnnotation( jaxbJoinTable.getForeignKey(), memberDetails, xmlDocumentContext )
			);
		}
		if ( jaxbJoinTable.getInverseForeignKey() != null ) {
			joinTableAnn.setAttributeValue(
					"inverseForeignKey",
					createForeignKeyAnnotation( jaxbJoinTable.getInverseForeignKey(), memberDetails, xmlDocumentContext )
			);
		}

		applyUniqueConstraints( jaxbJoinTable.getUniqueConstraint(), memberDetails, joinTableAnn, xmlDocumentContext );

		applyIndexes( jaxbJoinTable.getIndex(), memberDetails, joinTableAnn, xmlDocumentContext );

		return joinTableAnn;
	}

	public static <A extends Annotation> void applyCheckConstraints(
			JaxbCheckable jaxbCheckable,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> annotationUsage,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isNotEmpty( jaxbCheckable.getCheckConstraints() ) ) {
			final List<AnnotationUsage<CheckConstraint>> checks = new ArrayList<>( jaxbCheckable.getCheckConstraints().size() );
			final AnnotationDescriptor<CheckConstraint> checkConstraintDescriptor = xmlDocumentContext.getModelBuildingContext()
					.getAnnotationDescriptorRegistry()
					.getDescriptor( CheckConstraint.class );
			for ( JaxbCheckConstraintImpl jaxbCheck : jaxbCheckable.getCheckConstraints() ) {
				final MutableAnnotationUsage<CheckConstraint> checkAnn = XmlProcessingHelper.getOrMakeAnnotation( CheckConstraint.class, target, xmlDocumentContext );
				applyOr( jaxbCheck, JaxbCheckConstraintImpl::getName, "name", checkAnn, checkConstraintDescriptor );
				applyOr( jaxbCheck, JaxbCheckConstraintImpl::getConstraint, "constraint", checkAnn, checkConstraintDescriptor );
				applyOr( jaxbCheck, JaxbCheckConstraintImpl::getOptions, "options", checkAnn, checkConstraintDescriptor );
				checks.add( checkAnn );
			}
			annotationUsage.setAttributeValue( "check", checks );
		}
	}

	public static void applyTargetClass(
			String name,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final ClassDetails classDetails = resolveJavaType( name, xmlDocumentContext );
		final MutableAnnotationUsage<Target> targetAnn = XmlProcessingHelper.makeAnnotation( Target.class, memberDetails, xmlDocumentContext );
		targetAnn.setAttributeValue( "value", classDetails );
	}

	public static void applyTemporal(
			TemporalType temporalType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( temporalType == null ) {
			return;
		}

		final MutableAnnotationUsage<Temporal> annotationUsage = XmlProcessingHelper.makeAnnotation( Temporal.class, memberDetails, xmlDocumentContext );
		annotationUsage.setAttributeValue( "value", temporalType );
	}

	public static void applyLob(JaxbLobImpl jaxbLob, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext) {
		if ( jaxbLob == null ) {
			return;
		}

		XmlProcessingHelper.makeAnnotation( Lob.class, memberDetails, xmlDocumentContext );
	}

	public static void applyEnumerated(EnumType enumType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext) {
		if ( enumType == null ) {
			return;
		}

		final MutableAnnotationUsage<Enumerated> annotationUsage = XmlProcessingHelper.makeAnnotation(
				Enumerated.class,
				memberDetails,
				xmlDocumentContext
		);

		annotationUsage.setAttributeValue( "value", enumType );
	}

	public static void applyNationalized(
			JaxbNationalizedImpl jaxbNationalized,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNationalized == null ) {
			return;
		}

		XmlProcessingHelper.makeAnnotation( Nationalized.class, memberDetails, xmlDocumentContext );
	}

	public static void applyGeneratedValue(
			JaxbGeneratedValueImpl jaxbGeneratedValue,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGeneratedValue == null ) {
			return;
		}

		final MutableAnnotationUsage<GeneratedValue> generatedValueAnn = XmlProcessingHelper.makeAnnotation( GeneratedValue.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( generatedValueAnn );
		generatedValueAnn.setAttributeValue( "strategy", jaxbGeneratedValue.getStrategy() );
		generatedValueAnn.setAttributeValue( "generator", jaxbGeneratedValue.getGenerator() );
	}

	public static void applySequenceGenerator(
			JaxbSequenceGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final MutableAnnotationUsage<SequenceGenerator> sequenceAnn = XmlProcessingHelper.getOrMakeNamedAnnotation(
				SequenceGenerator.class,
				jaxbGenerator.getName(),
				memberDetails,
				xmlDocumentContext
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
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final MutableAnnotationUsage<TableGenerator> annotationUsage = XmlProcessingHelper.makeAnnotation( TableGenerator.class, memberDetails, xmlDocumentContext );
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
		applyUniqueConstraints( jaxbGenerator.getUniqueConstraint(), memberDetails, annotationUsage, xmlDocumentContext );
		applyIndexes( jaxbGenerator.getIndex(), memberDetails, annotationUsage, xmlDocumentContext );
	}

	public static void applyUuidGenerator(
			JaxbUuidGeneratorImpl jaxbGenerator,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbGenerator == null ) {
			return;
		}

		final MutableAnnotationUsage<UuidGenerator> annotationUsage = XmlProcessingHelper.makeAnnotation( UuidGenerator.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "style", jaxbGenerator.getStyle() );
	}

	public static void applyAttributeOverrides(
			List<JaxbAttributeOverrideImpl> jaxbOverrides,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyAttributeOverrides( jaxbOverrides, memberDetails, null, xmlDocumentContext );
	}

	public static void applyAttributeOverrides(
			List<JaxbAttributeOverrideImpl> jaxbOverrides,
			MutableMemberDetails memberDetails,
			String namePrefix,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbOverrides ) ) {
			return;
		}

		jaxbOverrides.forEach( (jaxbOverride) -> {
			final MutableAnnotationUsage<AttributeOverride> annotationUsage = XmlProcessingHelper.makeAnnotation(
					AttributeOverride.class,
					memberDetails,
					xmlDocumentContext
			);
			memberDetails.addAnnotationUsage( annotationUsage );
			annotationUsage.setAttributeValue( "name", prefixIfNotAlready( jaxbOverride.getName(), namePrefix ) );
			annotationUsage.setAttributeValue( "column", createColumnAnnotation( jaxbOverride.getColumn(), memberDetails, xmlDocumentContext ) );
		} );
	}

	public static void applyAssociationOverrides(
			List<JaxbAssociationOverrideImpl> jaxbOverrides,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbOverrides ) ) {
			return;
		}

		jaxbOverrides.forEach( (jaxbOverride) -> {
			final MutableAnnotationUsage<AssociationOverride> annotationUsage = XmlProcessingHelper.makeAnnotation(
					AssociationOverride.class,
					memberDetails,
					xmlDocumentContext
			);
			memberDetails.addAnnotationUsage( annotationUsage );
			annotationUsage.setAttributeValue( "name", jaxbOverride.getName() );
			final List<JaxbJoinColumnImpl> joinColumns = jaxbOverride.getJoinColumns();
			if ( CollectionHelper.isNotEmpty(  joinColumns)) {
				annotationUsage.setAttributeValue( "joinColumns", createJoinColumns( joinColumns, memberDetails, xmlDocumentContext ) );
			}
			if ( jaxbOverride.getJoinTable() != null ) {
				annotationUsage.setAttributeValue(
						"joinTable",
						applyJoinTable( jaxbOverride.getJoinTable(), memberDetails, xmlDocumentContext )
				);
			}
			if ( jaxbOverride.getForeignKeys() != null ) {
				annotationUsage.setAttributeValue(
						"foreignKey",
						createForeignKeyAnnotation( jaxbOverride.getForeignKeys(), memberDetails, xmlDocumentContext )
				);
			}
		} );
	}

	public static void applyOptimisticLockInclusion(
			boolean inclusion,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<OptimisticLock> annotationUsage = XmlProcessingHelper.makeAnnotation(
				OptimisticLock.class,
				memberDetails,
				xmlDocumentContext
		);
		memberDetails.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "exclude", !inclusion );
	}

	public static void applyConvert(
			JaxbConvertImpl jaxbConvert,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyConvert( jaxbConvert, memberDetails, null, xmlDocumentContext );
	}

	public static void applyConvert(
			JaxbConvertImpl jaxbConvert,
			MutableMemberDetails memberDetails,
			String namePrefix,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbConvert == null ) {
			return;
		}

		final MutableAnnotationUsage<Convert> annotationUsage = XmlProcessingHelper.makeAnnotation(
				Convert.class,
				memberDetails,
				xmlDocumentContext
		);
		memberDetails.addAnnotationUsage( annotationUsage );

		final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry();
		final ClassDetails converter = classDetailsRegistry.resolveClassDetails( jaxbConvert.getConverter() );
		annotationUsage.setAttributeValue( "converter", converter );
		annotationUsage.setAttributeValue( "attributeName", prefixIfNotAlready( jaxbConvert.getAttributeName(), namePrefix ) );
		annotationUsage.setAttributeValue( "disableConversion", jaxbConvert.isDisableConversion() );
	}

	public static void applyTable(
			JaxbTableImpl jaxbTable,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<Table> tableAnn = XmlProcessingHelper.makeAnnotation( Table.class, target, xmlDocumentContext );
		final AnnotationDescriptor<Table> tableDescriptor = xmlDocumentContext.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( Table.class );

		applyOr( jaxbTable, JaxbTableImpl::getName, "name", tableAnn, tableDescriptor );
		applyTableAttributes( jaxbTable, target, tableAnn, tableDescriptor, xmlDocumentContext );
	}

	public static void applyTableOverride(
			JaxbTableImpl jaxbTable,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbTable == null ) {
			return;
		}

		final MutableAnnotationUsage<Table> tableAnn = XmlProcessingHelper.getOrMakeAnnotation( Table.class, target, xmlDocumentContext );
		final AnnotationDescriptor<Table> tableDescriptor = xmlDocumentContext.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( Table.class );

		applyOr( jaxbTable, JaxbTableImpl::getName, "name", tableAnn, tableDescriptor );
		applyTableAttributes( jaxbTable, target, tableAnn, tableDescriptor, xmlDocumentContext );
	}

	public static <A extends Annotation> void applyTableAttributes(
			JaxbTableMapping jaxbTable,
			MutableAnnotationTarget target,
			MutableAnnotationUsage<A> tableAnn,
			AnnotationDescriptor<A> annotationDescriptor,
			XmlDocumentContext xmlDocumentContext) {
		applyOr( jaxbTable, JaxbTableMapping::getCatalog, "catalog", tableAnn, annotationDescriptor );
		applyOr( jaxbTable, JaxbTableMapping::getSchema, "schema", tableAnn, annotationDescriptor );
		applyOr( jaxbTable, JaxbTableMapping::getOptions, "options", tableAnn, annotationDescriptor );
		applyOr( jaxbTable, JaxbTableMapping::getComment, "comment", tableAnn, annotationDescriptor );
		applyCheckConstraints( jaxbTable, target, tableAnn, xmlDocumentContext );
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
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNaturalId == null ) {
			return;
		}
		final MutableAnnotationUsage<NaturalId> annotationUsage = XmlProcessingHelper.makeAnnotation(
				NaturalId.class,
				backingMember,
				xmlDocumentContext
		);
		backingMember.addAnnotationUsage( annotationUsage );
		annotationUsage.setAttributeValue( "mutable", jaxbNaturalId.isMutable() );
	}

	public static void applyNaturalIdCache(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNaturalId == null || jaxbNaturalId.getCaching() == null ) {
			return;
		}

		final MutableAnnotationUsage<NaturalIdCache> annotationUsage = XmlProcessingHelper.makeAnnotation(
				NaturalIdCache.class,
				classDetails,
				xmlDocumentContext
		);
		classDetails.addAnnotationUsage( annotationUsage );

		final JaxbCachingImpl jaxbCaching = jaxbNaturalId.getCaching();
		annotationUsage.setAttributeValue( "region", jaxbCaching.getRegion() );
	}

	public static void applyId(
			JaxbIdImpl jaxbId,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbId == null ) {
			return;
		}
		final MutableAnnotationUsage<Id> annotationUsage = XmlProcessingHelper.makeAnnotation(
				Id.class,
				memberDetails,
				xmlDocumentContext
		);
		memberDetails.addAnnotationUsage( annotationUsage );
	}

	public static void applyEmbeddedId(
			JaxbEmbeddedIdImpl jaxbEmbeddedId,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbEmbeddedId == null ) {
			return;
		}
		final MutableAnnotationUsage<EmbeddedId> annotationUsage = XmlProcessingHelper.makeAnnotation(
				EmbeddedId.class,
				memberDetails,
				xmlDocumentContext
		);
		memberDetails.addAnnotationUsage( annotationUsage );
	}

	static void applyInheritance(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbEntity.getInheritance() == null ) {
			return;
		}

		final MutableAnnotationUsage<Inheritance> inheritanceAnn = XmlProcessingHelper.getOrMakeAnnotation(
				Inheritance.class,
				classDetails,
				xmlDocumentContext
		);
		inheritanceAnn.setAttributeValue( "strategy", jaxbEntity.getInheritance().getStrategy() );
	}

	public static ClassDetails resolveJavaType(String value, XmlDocumentContext xmlDocumentContext) {
		return resolveJavaType( value, xmlDocumentContext.getModelBuildingContext() );
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
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbBasicMapping.getType() != null ) {
			applyUserType( jaxbBasicMapping.getType(), memberDetails, xmlDocumentContext );
		}
		else if ( jaxbBasicMapping.getJavaType() != null ) {
			applyJavaTypeDescriptor( jaxbBasicMapping.getJavaType(), memberDetails, xmlDocumentContext );
		}
		else if ( StringHelper.isNotEmpty( jaxbBasicMapping.getTarget() ) ) {
			applyTargetClass( jaxbBasicMapping.getTarget(), memberDetails, xmlDocumentContext );
		}

		if ( StringHelper.isNotEmpty( jaxbBasicMapping.getJdbcType() ) ) {
			applyJdbcTypeDescriptor( jaxbBasicMapping.getJdbcType(), memberDetails, xmlDocumentContext );
		}
		else if ( jaxbBasicMapping.getJdbcTypeCode() != null ) {
			applyJdbcTypeCode( jaxbBasicMapping.getJdbcTypeCode(), memberDetails, xmlDocumentContext );
		}
		else if ( StringHelper.isNotEmpty( jaxbBasicMapping.getJdbcTypeName() ) ) {
			applyJdbcTypeCode(
					resolveJdbcTypeName( jaxbBasicMapping.getJdbcTypeName() ),
					memberDetails,
					xmlDocumentContext
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
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<JavaType> typeAnn = XmlProcessingHelper.makeAnnotation( JavaType.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( typeAnn );

		final ClassDetails descriptorClass = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( descriptorClassName );
		typeAnn.setAttributeValue( "value", descriptorClass );
	}


	private static void applyJdbcTypeDescriptor(
			String descriptorClassName,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final ClassDetails descriptorClassDetails = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry()
				.resolveClassDetails( descriptorClassName );
		final MutableAnnotationUsage<JdbcType> jdbcTypeAnn = XmlProcessingHelper.makeAnnotation( JdbcType.class, memberDetails, xmlDocumentContext );
		jdbcTypeAnn.setAttributeValue( "value", descriptorClassDetails );

	}

	public static void applyJdbcTypeCode(
			Integer jdbcTypeCode,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jdbcTypeCode == null ) {
			return;
		}

		final MutableAnnotationUsage<JdbcTypeCode> typeCodeAnn = XmlProcessingHelper.makeAnnotation( JdbcTypeCode.class, memberDetails, xmlDocumentContext );
		memberDetails.addAnnotationUsage( typeCodeAnn );
		typeCodeAnn.setAttributeValue( "value", jdbcTypeCode );
	}

	public static void applyFilter(
			JaxbHbmFilterImpl jaxbFilter,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applyFilter( jaxbFilter, target, Filter.class, xmlDocumentContext );
	}

	public static void applyJoinTableFilter(
			JaxbHbmFilterImpl jaxbFilter,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applyFilter( jaxbFilter, target, FilterJoinTable.class, xmlDocumentContext );
	}

	private static <F extends Annotation> void applyFilter(
			JaxbHbmFilterImpl jaxbFilter,
			MutableAnnotationTarget target,
			Class<F> filterAnnotationClass,
			XmlDocumentContext xmlDocumentContext) {
		// Since @Filter and @FilterJoinTable have exactly the same attributes,
		// we can use the same method with parametrized annotation class
		final MutableAnnotationUsage<F> filterAnn = XmlProcessingHelper.getOrMakeNamedAnnotation(
				filterAnnotationClass,
				jaxbFilter.getName(),
				target,
				xmlDocumentContext
		);

		applyAttributeIfSpecified( filterAnn, "condition", jaxbFilter.getCondition() );
		applyAttributeIfSpecified( filterAnn, "deduceAliasInjectionPoints", jaxbFilter.isAutoAliasInjection() );

		final List<JaxbHbmFilterImpl.JaxbAliasesImpl> aliases = jaxbFilter.getAliases();
		if ( !CollectionHelper.isEmpty( aliases ) ) {
			filterAnn.setAttributeValue( "aliases", getSqlFragmentAliases( aliases, target, xmlDocumentContext ) );
		}
	}

	private static List<AnnotationUsage<SqlFragmentAlias>> getSqlFragmentAliases(
			List<JaxbHbmFilterImpl.JaxbAliasesImpl> aliases,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		final List<AnnotationUsage<SqlFragmentAlias>> sqlFragmentAliases = new ArrayList<>( aliases.size() );
		for ( JaxbHbmFilterImpl.JaxbAliasesImpl alias : aliases ) {
			final MutableAnnotationUsage<SqlFragmentAlias> aliasAnn = XmlProcessingHelper.makeNestedAnnotation( SqlFragmentAlias.class, target, xmlDocumentContext );
			aliasAnn.setAttributeValue( "alias", alias.getAlias() );
			applyAttributeIfSpecified( aliasAnn, "table", alias.getTable() );
			if ( StringHelper.isNotEmpty( alias.getEntity() ) ) {
				aliasAnn.setAttributeValue(
						"entity",
						xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry().resolveClassDetails( alias.getEntity() )
				);
			}
			sqlFragmentAliases.add( aliasAnn );
		}
		return sqlFragmentAliases;
	}

	public static void applySqlRestriction(
			String sqlRestriction,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applySqlRestriction( sqlRestriction, target, SQLRestriction.class, xmlDocumentContext );
	}

	public static void applySqlJoinTableRestriction(
			String sqlJoinTableRestriction,
			MutableAnnotationTarget target,
			XmlDocumentContext xmlDocumentContext) {
		applySqlRestriction( sqlJoinTableRestriction, target, SQLJoinTableRestriction.class, xmlDocumentContext );
	}

	private static <A extends Annotation> void applySqlRestriction(
			String sqlRestriction,
			MutableAnnotationTarget target,
			Class<A> annotationType,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( sqlRestriction ) ) {
			final MutableAnnotationUsage<A> annotation = XmlProcessingHelper.getOrMakeAnnotation( annotationType, target, xmlDocumentContext );
			annotation.setAttributeValue( "value", sqlRestriction );
		}
	}

	public static <A extends Annotation> void applyCustomSql(
			JaxbCustomSqlImpl jaxbCustomSql,
			MutableAnnotationTarget target,
			Class<A> annotationType,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCustomSql != null ) {
			final MutableAnnotationUsage<A> annotation = XmlProcessingHelper.getOrMakeAnnotation( annotationType, target, xmlDocumentContext );
			annotation.setAttributeValue( "sql", jaxbCustomSql.getValue() );
			annotation.setAttributeValue( "callable", jaxbCustomSql.isCallable() );
			applyAttributeIfSpecified( annotation, "table", jaxbCustomSql.getTable() );
			if ( jaxbCustomSql.getResultCheck() != null ) {
				annotation.setAttributeValue( "check", getResultCheckStyle( jaxbCustomSql.getResultCheck() ) );
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
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbIdClass != null ) {
			XmlProcessingHelper.getOrMakeAnnotation( IdClass.class, target, xmlDocumentContext ).setAttributeValue(
					"value",
					xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry().resolveClassDetails( jaxbIdClass.getClazz() )
			);
		}
	}

	static void applyEntityListener(
			JaxbEntityListenerImpl jaxbEntityListener,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<EntityListeners> entityListeners = XmlProcessingHelper.getOrMakeAnnotation(
				EntityListeners.class,
				classDetails,
				xmlDocumentContext
		);
		final MutableClassDetails entityListenerClass = (MutableClassDetails) xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry()
				.resolveClassDetails( jaxbEntityListener.getClazz() );
		applyLifecycleCallbacks(
				jaxbEntityListener,
				JpaEventListenerStyle.LISTENER,
				entityListenerClass,
				xmlDocumentContext
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
			XmlDocumentContext xmlDocumentContext) {
		applyLifecycleCallback( lifecycleCallbackContainer.getPrePersist(), callbackType, PrePersist.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostPersist(), callbackType, PostPersist.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPreRemove(), callbackType, PreRemove.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostRemove(), callbackType, PostRemove.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPreUpdate(), callbackType, PreUpdate.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostUpdate(), callbackType, PostUpdate.class, classDetails, xmlDocumentContext );
		applyLifecycleCallback( lifecycleCallbackContainer.getPostLoad(), callbackType, PostLoad.class, classDetails, xmlDocumentContext );
	}

	private static <A extends Annotation> void applyLifecycleCallback(
			JaxbLifecycleCallback lifecycleCallback,
			JpaEventListenerStyle callbackType,
			Class<A> lifecycleAnnotation,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
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
			XmlProcessingHelper.makeAnnotation( lifecycleAnnotation, (MutableMemberDetails) methodDetails, xmlDocumentContext );
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
			XmlDocumentContext xmlDocumentContext) {
		if ( rowId != null ) {
			final MutableAnnotationUsage<RowId> rowIdAnn = XmlProcessingHelper.getOrMakeAnnotation( RowId.class, target, xmlDocumentContext );
			applyAttributeIfSpecified( rowIdAnn, "value", rowId );
		}
	}

	private static String prefixIfNotAlready(String value, String prefix) {
		if ( StringHelper.isNotEmpty( prefix ) ) {
			final String previous = StringHelper.unqualify( value );
			if ( !previous.equalsIgnoreCase( prefix ) ) {
				return StringHelper.qualify( prefix, value );
			}
		}
		return value;
	}

	static void applyNamedEntityGraph(
			JaxbNamedEntityGraphImpl namedEntityGraph,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( namedEntityGraph != null ) {
			final MutableAnnotationUsage<NamedEntityGraph> namedEntityGraphAnn = XmlProcessingHelper.getOrMakeAnnotation(
					NamedEntityGraph.class,
					target,
					xmlDocumentContext
			);

			final AnnotationDescriptor<NamedEntityGraph> namedEntityGraphAnnotationDescriptor = namedEntityGraphAnn.getAnnotationDescriptor();
			applyOr(
					namedEntityGraph,
					JaxbNamedEntityGraphImpl::getName,
					"name",
					namedEntityGraphAnn,
					namedEntityGraphAnnotationDescriptor
			);

			applyOr(
					namedEntityGraph,
					JaxbNamedEntityGraphImpl::isIncludeAllAttributes,
					"includeAllAttributes",
					namedEntityGraphAnn,
					namedEntityGraphAnnotationDescriptor
			);

			namedEntityGraphAnn.setAttributeValue(
					"attributeNodes",
					makeNamedAttributeNodes( namedEntityGraph.getNamedAttributeNode(), target, xmlDocumentContext )
			);

			namedEntityGraphAnn.setAttributeValue(
					"subgraphs",
					makeNamedSubgraphs(
							target,
							xmlDocumentContext,
							namedEntityGraph.getSubgraph()
					)
			);

			namedEntityGraphAnn.setAttributeValue(
					"subclassSubgraphs",
					makeNamedSubgraphs(
							target,
							xmlDocumentContext,
							namedEntityGraph.getSubclassSubgraph()
					)
			);

		}

	}

	private static List<MutableAnnotationUsage<NamedSubgraph>> makeNamedSubgraphs(
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext,
			List<JaxbNamedSubgraphImpl> subclassSubgraphNodes) {
		final List<MutableAnnotationUsage<NamedSubgraph>> subgraphAnnotations =
				new ArrayList<>( subclassSubgraphNodes.size() );
		for ( JaxbNamedSubgraphImpl subclassSubgraphNode : subclassSubgraphNodes ) {
			final String subGraphsNodeName = subclassSubgraphNode.getName();
			final MutableAnnotationUsage<NamedSubgraph> namedSubgraphNodeAnn = XmlProcessingHelper.getOrMakeNamedAnnotation(
					NamedSubgraph.class,
					subGraphsNodeName,
					target,
					xmlDocumentContext
			);
			applyAttributeIfSpecified( namedSubgraphNodeAnn, "name", subGraphsNodeName );

			final String clazz = subclassSubgraphNode.getClazz();
			if ( clazz == null ) {
				namedSubgraphNodeAnn.setAttributeValue(
						"type",
						resolveJavaType(
								namedSubgraphNodeAnn.getAnnotationDescriptor()
										.getAttribute( "type" )
										.getAttributeMethod()
										.getDefaultValue().toString(),
								xmlDocumentContext
						)
				);
			}
			else {
				namedSubgraphNodeAnn.setAttributeValue(
						"type",
						resolveJavaType( subclassSubgraphNode.getClazz(), xmlDocumentContext )

				);
			}
			namedSubgraphNodeAnn.setAttributeValue(
					"attributeNodes",
					makeNamedAttributeNodes( subclassSubgraphNode.getNamedAttributeNode(), target, xmlDocumentContext )
			);

			subgraphAnnotations.add( namedSubgraphNodeAnn );
		}
		return subgraphAnnotations;
	}

	private static List<MutableAnnotationUsage<NamedAttributeNode>> makeNamedAttributeNodes(
			List<JaxbNamedAttributeNodeImpl> namedAttributeNodes,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		final List<MutableAnnotationUsage<NamedAttributeNode>> namedAttributeNodeAnnotations =
				new ArrayList<>( namedAttributeNodes.size() );
		for ( JaxbNamedAttributeNodeImpl namedAttributeNode : namedAttributeNodes ) {
			final MutableAnnotationUsage<NamedAttributeNode> namedAttributeNodeAnn = XmlProcessingHelper.makeNestedAnnotation(
					NamedAttributeNode.class,
					target,
					xmlDocumentContext
			);
			applyAttributeIfSpecified( namedAttributeNodeAnn, "value", namedAttributeNode.getName() );
			final AnnotationDescriptor<NamedAttributeNode> namedAttributeNodeDescriptor = xmlDocumentContext
					.getModelBuildingContext()
					.getAnnotationDescriptorRegistry()
					.getDescriptor( NamedAttributeNode.class );
			applyOr(
					namedAttributeNode,
					JaxbNamedAttributeNodeImpl::getSubgraph,
					"subgraph",
					namedAttributeNodeAnn,
					namedAttributeNodeDescriptor
			);
			applyOr(
					namedAttributeNode,
					JaxbNamedAttributeNodeImpl::getKeySubgraph,
					"keySubgraph",
					namedAttributeNodeAnn,
					namedAttributeNodeDescriptor
			);
			namedAttributeNodeAnnotations.add( namedAttributeNodeAnn );

		}
		return namedAttributeNodeAnnotations;
	}

	static void applyDiscriminatorValue(
			String discriminatorValue,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( discriminatorValue != null ) {
			final MutableAnnotationUsage<DiscriminatorValue> rowIdAnn = XmlProcessingHelper
					.getOrMakeAnnotation( DiscriminatorValue.class, target, xmlDocumentContext );
			applyAttributeIfSpecified( rowIdAnn, "value", discriminatorValue );
		}
	}

	static void applyDiscriminatorColumn(
			JaxbDiscriminatorColumnImpl discriminatorColumn,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext){
		if ( discriminatorColumn != null ) {
			final MutableAnnotationUsage<DiscriminatorColumn> discriminatorColumnAnn = XmlProcessingHelper
					.getOrMakeAnnotation( DiscriminatorColumn.class, target, xmlDocumentContext );
			final AnnotationDescriptor<DiscriminatorColumn> discriminatorColumnDescriptor = xmlDocumentContext
					.getModelBuildingContext()
					.getAnnotationDescriptorRegistry()
					.getDescriptor( DiscriminatorColumn.class );
			applyOr(
					discriminatorColumn,
					JaxbDiscriminatorColumnImpl::getName,
					"name",
					discriminatorColumnAnn,
					discriminatorColumnDescriptor
			);

			applyOr(
					discriminatorColumn,
					JaxbDiscriminatorColumnImpl::getDiscriminatorType,
					"discriminatorType",
					discriminatorColumnAnn,
					discriminatorColumnDescriptor
			);
			applyOr(
					discriminatorColumn,
					JaxbDiscriminatorColumnImpl::getColumnDefinition,
					"columnDefinition",
					discriminatorColumnAnn,
					discriminatorColumnDescriptor
			);
			applyOr(
					discriminatorColumn,
					JaxbDiscriminatorColumnImpl::getOptions,
					"options",
					discriminatorColumnAnn,
					discriminatorColumnDescriptor
			);
			applyOr(
					discriminatorColumn,
					JaxbDiscriminatorColumnImpl::getLength,
					"length",
					discriminatorColumnAnn,
					discriminatorColumnDescriptor
			);

			// todo : add force-selection attribute to @DiscriminatorColumn
		}

	}

	public static void applyDiscriminatorFormula(
			String discriminatorFormula,
			MutableClassDetails target,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( discriminatorFormula ) ) {
			final MutableAnnotationUsage<DiscriminatorFormula> discriminatorFormulaAnn = XmlProcessingHelper
					.getOrMakeAnnotation( DiscriminatorFormula.class, target, xmlDocumentContext );
			discriminatorFormulaAnn.setAttributeValue( "value", discriminatorFormula );

			// todo add to mapping-3.2.0.xsd discriminatorType of @DiscriminatorFormula
		}
	}
}
