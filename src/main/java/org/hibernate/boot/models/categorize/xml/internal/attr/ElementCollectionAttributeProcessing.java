/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.internal.attr;

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
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGeneratedValueImpl;
import org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.categorize.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;

import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Temporal;

import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class ElementCollectionAttributeProcessing {

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processElementCollectionAttribute(
			JaxbElementCollectionImpl jaxbElementCollection,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry();

		final AccessType accessType = coalesce( jaxbElementCollection.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbElementCollection.getName(),
				accessType,
				declarer
		);

		final MutableAnnotationUsage<ElementCollection> elementCollectionAnn = XmlProcessingHelper.getOrMakeAnnotation(
				ElementCollection.class,
				memberDetails,
				xmlDocumentContext
		);
		XmlProcessingHelper.setIf( jaxbElementCollection.getTargetClass(), "targetClass", elementCollectionAnn );
		XmlProcessingHelper.setIf( jaxbElementCollection.getFetch(), "fetch", elementCollectionAnn );

		CommonAttributeProcessing.applyAttributeBasics( jaxbElementCollection, memberDetails, elementCollectionAnn, accessType, xmlDocumentContext );

		if ( jaxbElementCollection.getFetchMode() != null ) {
			final MutableAnnotationUsage<Fetch> fetchAnn = XmlProcessingHelper.getOrMakeAnnotation( Fetch.class, memberDetails, xmlDocumentContext );
			fetchAnn.setAttributeValue( "value", jaxbElementCollection.getFetchMode() );
		}

		final JaxbCollectionIdImpl jaxbCollectionId = jaxbElementCollection.getCollectionId();
		if ( jaxbCollectionId != null ) {
			final MutableAnnotationUsage<CollectionId> collectionIdAnn = XmlProcessingHelper.getOrMakeAnnotation(
					CollectionId.class,
					memberDetails,
					xmlDocumentContext
			);

			final JaxbColumnImpl jaxbColumn = jaxbCollectionId.getColumn();
			final MutableAnnotationUsage<Column> columnAnn = XmlProcessingHelper.getOrMakeAnnotation(
					Column.class,
					memberDetails,
					xmlDocumentContext
			);
			collectionIdAnn.setAttributeValue( "column", columnAnn );
			XmlProcessingHelper.setIf( jaxbColumn.getName(), "name", columnAnn );
			columnAnn.setAttributeValue( "nullable", false );
			columnAnn.setAttributeValue( "unique", false );
			columnAnn.setAttributeValue( "updatable", false );
			XmlProcessingHelper.setIf( jaxbColumn.getLength(), "length", columnAnn );
			XmlProcessingHelper.setIf( jaxbColumn.getPrecision(), "precision", columnAnn );
			XmlProcessingHelper.setIf( jaxbColumn.getScale(), "scale", columnAnn );
			XmlProcessingHelper.setIf( jaxbColumn.getTable(), "table", columnAnn );
			XmlProcessingHelper.setIf( jaxbColumn.getColumnDefinition(), "columnDefinition", columnAnn );

			final JaxbGeneratedValueImpl generator = jaxbCollectionId.getGenerator();
			if ( generator != null ) {
				XmlProcessingHelper.setIf( generator.getGenerator(), "generator", collectionIdAnn );
			}
		}

		if ( jaxbElementCollection.getClassification() != null ) {
			final MutableAnnotationUsage<CollectionClassification> collectionClassificationAnn = XmlProcessingHelper.getOrMakeAnnotation(
					CollectionClassification.class,
					memberDetails,
					xmlDocumentContext
			);
			XmlProcessingHelper.setIf( jaxbElementCollection.getClassification(), "value", collectionClassificationAnn );
			if ( jaxbElementCollection.getClassification() == LimitedCollectionClassification.BAG ) {
				XmlProcessingHelper.getOrMakeAnnotation( Bag.class, memberDetails, xmlDocumentContext );
			}
		}

		XmlAnnotationHelper.applyCollectionUserType( jaxbElementCollection.getCollectionType(), memberDetails, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbElementCollection.getSort() ) ) {
			final MutableAnnotationUsage<SortComparator> sortAnn = XmlProcessingHelper.getOrMakeAnnotation(
					SortComparator.class,
					memberDetails,
					xmlDocumentContext
			);
			final ClassDetails comparatorClassDetails = classDetailsRegistry.resolveClassDetails( jaxbElementCollection.getSort() );
			sortAnn.setAttributeValue( "value", comparatorClassDetails );
		}

		if ( jaxbElementCollection.getSortNatural() != null ) {
			XmlProcessingHelper.getOrMakeAnnotation( SortNatural.class, memberDetails, xmlDocumentContext );
		}

		if ( StringHelper.isNotEmpty( jaxbElementCollection.getOrderBy() ) ) {
			final MutableAnnotationUsage<OrderBy> orderByAnn = XmlProcessingHelper.getOrMakeAnnotation(
					OrderBy.class,
					memberDetails,
					xmlDocumentContext
			);
			orderByAnn.setAttributeValue( "value", jaxbElementCollection.getOrderBy() );
		}

		if ( jaxbElementCollection.getOrderColumn() != null ) {
			final MutableAnnotationUsage<OrderColumn> orderByAnn = XmlProcessingHelper.getOrMakeAnnotation(
					OrderColumn.class,
					memberDetails,
					xmlDocumentContext
			);
			XmlProcessingHelper.setIf( jaxbElementCollection.getOrderColumn().getName(), "name", orderByAnn );
			XmlProcessingHelper.setIf( jaxbElementCollection.getOrderColumn().isNullable(), "nullable", orderByAnn );
			XmlProcessingHelper.setIf( jaxbElementCollection.getOrderColumn().isInsertable(), "insertable", orderByAnn );
			XmlProcessingHelper.setIf( jaxbElementCollection.getOrderColumn().isUpdatable(), "updatable", orderByAnn );
			XmlProcessingHelper.setIf( jaxbElementCollection.getOrderColumn().getColumnDefinition(), "columnDefinition", orderByAnn );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// elements

		if ( jaxbElementCollection.getEnumerated() != null ) {
			final MutableAnnotationUsage<Enumerated> enumeratedAnn = XmlProcessingHelper.getOrMakeAnnotation(
					Enumerated.class,
					memberDetails,
					xmlDocumentContext
			);
			enumeratedAnn.setAttributeValue( "value", jaxbElementCollection.getEnumerated() );
		}

		if ( jaxbElementCollection.getLob() != null ) {
			XmlProcessingHelper.getOrMakeAnnotation( Lob.class, memberDetails, xmlDocumentContext );
		}

		if ( jaxbElementCollection.getNationalized() != null ) {
			XmlProcessingHelper.getOrMakeAnnotation( Nationalized.class, memberDetails, xmlDocumentContext );
		}

		if ( jaxbElementCollection.getTemporal() != null ) {
			final MutableAnnotationUsage<Temporal> temporalAnn = XmlProcessingHelper.getOrMakeAnnotation(
					Temporal.class,
					memberDetails,
					xmlDocumentContext
			);
			temporalAnn.setAttributeValue( "value", jaxbElementCollection.getTemporal() );
		}

		XmlAnnotationHelper.applyBasicTypeComposition( jaxbElementCollection, memberDetails, xmlDocumentContext );
		if ( StringHelper.isNotEmpty( jaxbElementCollection.getTargetClass() ) ) {
			final MutableAnnotationUsage<Target> targetAnn = XmlProcessingHelper.getOrMakeAnnotation( Target.class, memberDetails, xmlDocumentContext );
			targetAnn.setAttributeValue( "value", jaxbElementCollection.getTargetClass() );
		}

		jaxbElementCollection.getConverts().forEach( (jaxbConvert) -> {
			XmlAnnotationHelper.applyConvert( jaxbConvert, memberDetails, xmlDocumentContext );
		} );

		jaxbElementCollection.getFilters().forEach( (jaxbFilter) -> XmlAnnotationHelper.applyFilter(
				jaxbFilter,
				memberDetails,
				xmlDocumentContext
		) );

		XmlAnnotationHelper.applySqlRestriction( jaxbElementCollection.getSqlRestriction(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyCustomSql( jaxbElementCollection.getSqlInsert(), memberDetails, SQLInsert.class, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbElementCollection.getSqlUpdate(), memberDetails, SQLUpdate.class, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbElementCollection.getSqlDelete(), memberDetails, SQLDelete.class, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbElementCollection.getSqlDeleteAll(), memberDetails, SQLDeleteAll.class, xmlDocumentContext );

		// todo : attribute-override
		// todo : association-override

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// map-key


		return memberDetails;
	}
}
