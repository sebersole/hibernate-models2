/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal;

import org.hibernate.annotations.Bag;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBaseAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGeneratedValueImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAnyMappingImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.source.internal.MutableAnnotationUsage;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Temporal;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper.getOrMakeAnnotation;
import static org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper.setIf;

/**
 * Helper for handling persistent attributes defined in mapping XML in metadata-complete mode
 *
 * @author Steve Ebersole
 */
public class AttributeProcessor {
	public static void processNaturalId(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		processNaturalId( jaxbNaturalId, mutableClassDetails, classAccessType, null, sourceModelBuildingContext );
	}

	public static void processNaturalId(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			MemberAdjuster memberAdjuster,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbNaturalId == null ) {
			return;
		}

		XmlAnnotationHelper.applyNaturalIdCache( jaxbNaturalId, mutableClassDetails, sourceModelBuildingContext );

		processBaseAttributes( jaxbNaturalId, mutableClassDetails, classAccessType, memberAdjuster, sourceModelBuildingContext );
	}

	public static void processBaseAttributes(
			JaxbBaseAttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			MemberAdjuster memberAdjuster,
			SourceModelBuildingContext sourceModelBuildingContext) {
		for ( int i = 0; i < attributesContainer.getBasicAttributes().size(); i++ ) {
			final JaxbBasicImpl jaxbBasic = attributesContainer.getBasicAttributes().get( i );
			final MutableMemberDetails memberDetails = processBasicAttribute(
					jaxbBasic,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbBasic, sourceModelBuildingContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getEmbeddedAttributes().size(); i++ ) {
			final JaxbEmbeddedImpl jaxbEmbedded = attributesContainer.getEmbeddedAttributes().get( i );
			final MutableMemberDetails memberDetails = processEmbeddedAttribute(
					jaxbEmbedded,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbEmbedded, sourceModelBuildingContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getManyToOneAttributes().size(); i++ ) {
			final JaxbManyToOneImpl jaxbManyToOne = attributesContainer.getManyToOneAttributes().get( i );
			final MutableMemberDetails memberDetails = processManyToOneAttribute(
					jaxbManyToOne,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbManyToOne, sourceModelBuildingContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getAnyMappingAttributes().size(); i++ ) {
			final JaxbAnyMappingImpl jaxbAnyMapping = attributesContainer.getAnyMappingAttributes().get( i );
			final MutableMemberDetails memberDetails = processAnyMappingAttribute(
					jaxbAnyMapping,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbAnyMapping, sourceModelBuildingContext );
			}
		}
	}

	@FunctionalInterface
	public interface MemberAdjuster {
		<M extends MutableMemberDetails> void adjust(M member, JaxbPersistentAttribute jaxbPersistentAttribute, SourceModelBuildingContext sourceModelBuildingContext);
	}

	public static void processAttributes(
			JaxbAttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		processAttributes( attributesContainer, mutableClassDetails, classAccessType, null, sourceModelBuildingContext );
	}

	public static void processAttributes(
			JaxbAttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			MemberAdjuster memberAdjuster,
			SourceModelBuildingContext sourceModelBuildingContext) {
		processBaseAttributes( attributesContainer, mutableClassDetails, classAccessType, memberAdjuster, sourceModelBuildingContext );

		for ( int i = 0; i < attributesContainer.getOneToOneAttributes().size(); i++ ) {
			final JaxbOneToOneImpl jaxbOneToOne = attributesContainer.getOneToOneAttributes().get( i );
			final MutableMemberDetails memberDetails = processOneToOneAttribute(
					jaxbOneToOne,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbOneToOne, sourceModelBuildingContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getElementCollectionAttributes().size(); i++ ) {
			final JaxbElementCollectionImpl jaxbElementCollection = attributesContainer.getElementCollectionAttributes().get( i );
			final MutableMemberDetails memberDetails = processElementCollectionAttribute(
					jaxbElementCollection,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbElementCollection, sourceModelBuildingContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getOneToManyAttributes().size(); i++ ) {
			final JaxbOneToManyImpl jaxbOneToMany = attributesContainer.getOneToManyAttributes().get( i );
			final MutableMemberDetails memberDetails = processOneToManyAttribute(
					jaxbOneToMany,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbOneToMany, sourceModelBuildingContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getManyToManyAttributes().size(); i++ ) {
			final JaxbManyToManyImpl jaxbManyToMany = attributesContainer.getManyToManyAttributes().get( i );
			final MutableMemberDetails memberDetails = processManyToManyAttribute(
					jaxbManyToMany,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbManyToMany, sourceModelBuildingContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getPluralAnyMappingAttributes().size(); i++ ) {
			final JaxbPluralAnyMappingImpl jaxbPluralAnyMapping = attributesContainer.getPluralAnyMappingAttributes()
					.get( i );
			final MutableMemberDetails memberDetails = processPluralAnyMappingAttributes(
					jaxbPluralAnyMapping,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbPluralAnyMapping, sourceModelBuildingContext );
			}
		}
	}

	public static void processCommonAttributeAnnotations(
			JaxbPersistentAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			AccessType accessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlAnnotationHelper.applyAccess( accessType, memberDetails, sourceModelBuildingContext );

		// todo : optimistic-lock
	}

	public static MutableMemberDetails processBasicIdAttribute(
			JaxbIdImpl jaxbId,
			MutableClassDetails declarer,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbId.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbId.getName(),
				accessType,
				declarer,
				sourceModelBuildingContext
		);
		XmlAnnotationHelper.applyId( jaxbId, memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyBasic( jaxbId, memberDetails, sourceModelBuildingContext );
		AttributeProcessor.processCommonAttributeAnnotations(
				jaxbId,
				memberDetails,
				accessType,
				sourceModelBuildingContext
		);

		XmlAnnotationHelper.applyColumn(
				jaxbId.getColumn(),
				memberDetails,
				sourceModelBuildingContext
		);

		XmlAnnotationHelper.applyBasicTypeComposition(
				jaxbId,
				memberDetails,
				sourceModelBuildingContext
		);
		XmlAnnotationHelper.applyTemporal(
				jaxbId.getTemporal(),
				memberDetails,
				sourceModelBuildingContext
		);

		XmlAnnotationHelper.applyGeneratedValue(
				jaxbId.getGeneratedValue(),
				memberDetails,
				sourceModelBuildingContext
		);
		XmlAnnotationHelper.applySequenceGenerator(
				jaxbId.getSequenceGenerator(),
				memberDetails,
				sourceModelBuildingContext
		);
		XmlAnnotationHelper.applyTableGenerator(
				jaxbId.getTableGenerator(),
				memberDetails,
				sourceModelBuildingContext
		);
		XmlAnnotationHelper.applyUuidGenerator(
				jaxbId.getUuidGenerator(),
				memberDetails,
				sourceModelBuildingContext
		);

		// todo : unsaved-value?

		return memberDetails;
	}

	public static MutableMemberDetails processEmbeddedIdAttribute(
			JaxbEmbeddedIdImpl jaxbEmbeddedId,
			MutableClassDetails classDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbEmbeddedId.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.findAttributeMember(
				jaxbEmbeddedId.getName(),
				accessType,
				classDetails,
				sourceModelBuildingContext
		);

		XmlAnnotationHelper.applyEmbeddedId( jaxbEmbeddedId, memberDetails, sourceModelBuildingContext );
		AttributeProcessor.processCommonAttributeAnnotations(
				jaxbEmbeddedId,
				memberDetails,
				accessType,
				sourceModelBuildingContext
		);

		XmlAnnotationHelper.applyAttributeOverrides(
				jaxbEmbeddedId.getAttributeOverride(),
				memberDetails,
				sourceModelBuildingContext
		);

		return memberDetails;
	}

	public static MutableMemberDetails processBasicAttribute(
			JaxbBasicImpl jaxbBasic,
			MutableClassDetails declarer,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbBasic.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbBasic.getName(),
				accessType,
				declarer,
				sourceModelBuildingContext
		);

		XmlAnnotationHelper.applyBasic( jaxbBasic, memberDetails, sourceModelBuildingContext );

		processCommonAttributeAnnotations( jaxbBasic, memberDetails, accessType, sourceModelBuildingContext );
		// only semi-common
		XmlAnnotationHelper.applyOptimisticLockInclusion( jaxbBasic.isOptimisticLock(), memberDetails, sourceModelBuildingContext );

		XmlAnnotationHelper.applyColumn( jaxbBasic.getColumn(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyFormula( jaxbBasic.getFormula(), memberDetails, sourceModelBuildingContext );

		// todo : value generation

		XmlAnnotationHelper.applyConvert( jaxbBasic.getConvert(), memberDetails, sourceModelBuildingContext );

		XmlAnnotationHelper.applyBasicTypeComposition( jaxbBasic, memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyTemporal( jaxbBasic.getTemporal(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyLob( jaxbBasic.getLob(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyEnumerated( jaxbBasic.getEnumerated(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyNationalized( jaxbBasic.getNationalized(), memberDetails, sourceModelBuildingContext );

		return memberDetails;
	}

	public static MutableMemberDetails processEmbeddedAttribute(
			JaxbEmbeddedImpl jaxbEmbedded,
			MutableClassDetails declarer,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbEmbedded.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbEmbedded.getName(),
				accessType,
				declarer,
				sourceModelBuildingContext
		);

		XmlProcessingHelper.getOrMakeAnnotation( Embedded.class, memberDetails );

		if ( StringHelper.isNotEmpty( jaxbEmbedded.getTarget() ) ) {
			final MutableAnnotationUsage<Target> targetAnn = getOrMakeAnnotation( Target.class, memberDetails );
			targetAnn.setAttributeValue( "value", jaxbEmbedded.getTarget() );
		}

		processCommonAttributeAnnotations( jaxbEmbedded, memberDetails, accessType, sourceModelBuildingContext );
		XmlAnnotationHelper.applyAttributeOverrides( jaxbEmbedded.getAttributeOverride(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyAssociationOverrides( jaxbEmbedded.getAssociationOverride(), memberDetails, sourceModelBuildingContext );

		return memberDetails;
	}

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processOneToOneAttribute(
			JaxbOneToOneImpl jaxbOneToOne,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for one-to-one attributes not yet implemented" );
	}

	public static MutableMemberDetails processManyToOneAttribute(
			JaxbManyToOneImpl jaxbOneToOne,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for many-to-one attributes not yet implemented" );
	}

	public static MutableMemberDetails processAnyMappingAttribute(
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for any attributes not yet implemented" );
	}

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processElementCollectionAttribute(
			JaxbElementCollectionImpl jaxbElementCollection,
			MutableClassDetails declarer,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();

		final AccessType accessType = coalesce( jaxbElementCollection.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbElementCollection.getName(),
				accessType,
				declarer,
				sourceModelBuildingContext
		);

		final MutableAnnotationUsage<ElementCollection> elementCollectionAnn = XmlProcessingHelper.getOrMakeAnnotation(
				ElementCollection.class,
				memberDetails
		);
		setIf( jaxbElementCollection.getTargetClass(), "targetClass", elementCollectionAnn );
		setIf( jaxbElementCollection.getFetch(), "fetch", elementCollectionAnn );

		processCommonAttributeAnnotations( jaxbElementCollection, memberDetails, accessType, sourceModelBuildingContext );

		if ( jaxbElementCollection.getFetchMode() != null ) {
			final MutableAnnotationUsage<Fetch> fetchAnn = getOrMakeAnnotation( Fetch.class, memberDetails );
			fetchAnn.setAttributeValue( "value", jaxbElementCollection.getFetchMode() );
		}

		final JaxbCollectionIdImpl jaxbCollectionId = jaxbElementCollection.getCollectionId();
		if ( jaxbCollectionId != null ) {
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

		if ( jaxbElementCollection.getClassification() != null ) {
			final MutableAnnotationUsage<CollectionClassification> collectionClassificationAnn = getOrMakeAnnotation(
					CollectionClassification.class,
					memberDetails
			);
			setIf( jaxbElementCollection.getClassification(), "value", collectionClassificationAnn );
			if ( jaxbElementCollection.getClassification() == LimitedCollectionClassification.BAG ) {
				getOrMakeAnnotation( Bag.class, memberDetails );
			}
		}

		XmlAnnotationHelper.applyCollectionUserType( jaxbElementCollection.getCollectionType(), memberDetails, sourceModelBuildingContext );

		if ( StringHelper.isNotEmpty( jaxbElementCollection.getSort() ) ) {
			final MutableAnnotationUsage<SortComparator> sortAnn = getOrMakeAnnotation(
					SortComparator.class,
					memberDetails
			);
			final ClassDetails comparatorClassDetails = classDetailsRegistry.resolveClassDetails( jaxbElementCollection.getSort() );
			sortAnn.setAttributeValue( "value", comparatorClassDetails );
		}

		if ( jaxbElementCollection.getSortNatural() != null ) {
			getOrMakeAnnotation( SortNatural.class, memberDetails );
		}

		if ( StringHelper.isNotEmpty( jaxbElementCollection.getOrderBy() ) ) {
			final MutableAnnotationUsage<OrderBy> orderByAnn = getOrMakeAnnotation(
					OrderBy.class,
					memberDetails
			);
			orderByAnn.setAttributeValue( "value", jaxbElementCollection.getOrderBy() );
		}

		if ( jaxbElementCollection.getOrderColumn() != null ) {
			final MutableAnnotationUsage<OrderColumn> orderByAnn = getOrMakeAnnotation(
					OrderColumn.class,
					memberDetails
			);
			setIf( jaxbElementCollection.getOrderColumn().getName(), "name", orderByAnn );
			setIf( jaxbElementCollection.getOrderColumn().isNullable(), "nullable", orderByAnn );
			setIf( jaxbElementCollection.getOrderColumn().isInsertable(), "insertable", orderByAnn );
			setIf( jaxbElementCollection.getOrderColumn().isUpdatable(), "updatable", orderByAnn );
			setIf( jaxbElementCollection.getOrderColumn().getColumnDefinition(), "columnDefinition", orderByAnn );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// elements

		if ( jaxbElementCollection.getEnumerated() != null ) {
			final MutableAnnotationUsage<Enumerated> enumeratedAnn = getOrMakeAnnotation(
					Enumerated.class,
					memberDetails
			);
			enumeratedAnn.setAttributeValue( "value", jaxbElementCollection.getEnumerated() );
		}

		if ( jaxbElementCollection.getLob() != null ) {
			getOrMakeAnnotation( Lob.class, memberDetails );
		}

		if ( jaxbElementCollection.getNationalized() != null ) {
			getOrMakeAnnotation( Nationalized.class, memberDetails );
		}

		if ( jaxbElementCollection.getTemporal() != null ) {
			final MutableAnnotationUsage<Temporal> temporalAnn = getOrMakeAnnotation(
					Temporal.class,
					memberDetails
			);
			temporalAnn.setAttributeValue( "value", jaxbElementCollection.getTemporal() );
		}

		XmlAnnotationHelper.applyBasicTypeComposition( jaxbElementCollection, memberDetails, sourceModelBuildingContext );
		if ( StringHelper.isNotEmpty( jaxbElementCollection.getTargetClass() ) ) {
			final MutableAnnotationUsage<Target> targetAnn = getOrMakeAnnotation( Target.class, memberDetails );
			targetAnn.setAttributeValue( "value", jaxbElementCollection.getTargetClass() );
		}

		jaxbElementCollection.getConvert().forEach( (jaxbConvert) -> {
			XmlAnnotationHelper.applyConvert( jaxbConvert, memberDetails, sourceModelBuildingContext );
		} );

		jaxbElementCollection.getFilters().forEach( (jaxbFilter) -> XmlAnnotationHelper.applyFilter(
				jaxbFilter,
				memberDetails,
				sourceModelBuildingContext
		) );

		XmlAnnotationHelper.applySqlRestriction( jaxbElementCollection.getSqlRestriction(), memberDetails, sourceModelBuildingContext );

		XmlAnnotationHelper.applyCustomSql( jaxbElementCollection.getSqlInsert(), memberDetails, SQLInsert.class, sourceModelBuildingContext );
		XmlAnnotationHelper.applyCustomSql( jaxbElementCollection.getSqlUpdate(), memberDetails, SQLUpdate.class, sourceModelBuildingContext );
		XmlAnnotationHelper.applyCustomSql( jaxbElementCollection.getSqlDelete(), memberDetails, SQLDelete.class, sourceModelBuildingContext );
		XmlAnnotationHelper.applyCustomSql( jaxbElementCollection.getSqlDeleteAll(), memberDetails, SQLDeleteAll.class, sourceModelBuildingContext );

		// todo : attribute-override
		// todo : association-override

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// map-key


		return memberDetails;
	}

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processOneToManyAttribute(
			JaxbOneToManyImpl jaxbOneToMany,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for one-to-many attributes not yet implemented" );
	}

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processManyToManyAttribute(
			JaxbManyToManyImpl jaxbManyToMany,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for many-to-many attributes not yet implemented" );
	}

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processPluralAnyMappingAttributes(
			JaxbPluralAnyMappingImpl jaxbHbmManyToAny,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for many-to-any attributes not yet implemented" );
	}
}
