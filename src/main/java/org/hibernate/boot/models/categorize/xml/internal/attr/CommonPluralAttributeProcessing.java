/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.internal.attr;

import org.hibernate.annotations.Bag;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOrderColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;

import static org.hibernate.boot.models.categorize.xml.internal.XmlProcessingHelper.getOrMakeAnnotation;
import static org.hibernate.boot.models.categorize.xml.internal.XmlProcessingHelper.setIf;

/**
 * @author Marco Belladelli
 */
public class CommonPluralAttributeProcessing {
	public static void applyPluralAttributeStructure(
			JaxbPluralAttribute jaxbPluralAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext documentContext) {
		final SourceModelBuildingContext buildingContext = documentContext.getModelBuildingContext();
		final ClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry();

		if ( jaxbPluralAttribute.getFetchMode() != null ) {
			final MutableAnnotationUsage<Fetch> fetchAnn = getOrMakeAnnotation( Fetch.class, memberDetails, documentContext );
			fetchAnn.setAttributeValue( "value", jaxbPluralAttribute.getFetchMode() );
		}

		if ( jaxbPluralAttribute.getClassification() != null ) {
			final MutableAnnotationUsage<CollectionClassification> collectionClassificationAnn = getOrMakeAnnotation(
					CollectionClassification.class,
					memberDetails,
					documentContext
			);
			setIf( jaxbPluralAttribute.getClassification(), "value", collectionClassificationAnn );
			if ( jaxbPluralAttribute.getClassification() == LimitedCollectionClassification.BAG ) {
				getOrMakeAnnotation( Bag.class, memberDetails, documentContext );
			}
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// collection-structure

		XmlAnnotationHelper.applyCollectionUserType( jaxbPluralAttribute.getCollectionType(), memberDetails, documentContext );

		XmlAnnotationHelper.applyCollectionId( jaxbPluralAttribute.getCollectionId(), memberDetails, documentContext );

		if ( StringHelper.isNotEmpty( jaxbPluralAttribute.getOrderBy() ) ) {
			final MutableAnnotationUsage<OrderBy> orderByAnn = getOrMakeAnnotation(
					OrderBy.class,
					memberDetails,
					documentContext
			);
			orderByAnn.setAttributeValue( "value", jaxbPluralAttribute.getOrderBy() );
		}

		final JaxbOrderColumnImpl orderColumn = jaxbPluralAttribute.getOrderColumn();
		if ( orderColumn != null ) {
			final MutableAnnotationUsage<OrderColumn> orderByAnn = getOrMakeAnnotation(
					OrderColumn.class,
					memberDetails,
					documentContext
			);
			setIf( orderColumn.getName(), "name", orderByAnn );
			setIf( orderColumn.isNullable(), "nullable", orderByAnn );
			setIf( orderColumn.isInsertable(), "insertable", orderByAnn );
			setIf( orderColumn.isUpdatable(), "updatable", orderByAnn );
			setIf( orderColumn.getColumnDefinition(), "columnDefinition", orderByAnn );
		}

		if ( StringHelper.isNotEmpty( jaxbPluralAttribute.getSort() ) ) {
			final MutableAnnotationUsage<SortComparator> sortAnn = getOrMakeAnnotation(
					SortComparator.class,
					memberDetails,
					documentContext
			);
			final ClassDetails comparatorClassDetails = classDetailsRegistry.resolveClassDetails( jaxbPluralAttribute.getSort() );
			sortAnn.setAttributeValue( "value", comparatorClassDetails );
		}

		if ( jaxbPluralAttribute.getSortNatural() != null ) {
			getOrMakeAnnotation( SortNatural.class, memberDetails, documentContext );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// map-key

		if ( jaxbPluralAttribute.getMapKey() != null ) {
			final MutableAnnotationUsage<MapKey> mapKeyAnn = getOrMakeAnnotation( MapKey.class, memberDetails, documentContext );
			setIf( jaxbPluralAttribute.getMapKey().getName(), "name", mapKeyAnn );
		}

		if ( jaxbPluralAttribute.getMapKeyClass() != null ) {
			final ClassDetails mapKeyClass = classDetailsRegistry.resolveClassDetails( jaxbPluralAttribute.getMapKeyClass().getClazz() );
			getOrMakeAnnotation( MapKeyClass.class, memberDetails, documentContext ).setAttributeValue( "value", mapKeyClass );
		}

		if ( jaxbPluralAttribute.getMapKeyTemporal() != null ) {
			getOrMakeAnnotation( MapKeyTemporal.class, memberDetails, documentContext ).setAttributeValue(
					"value",
					jaxbPluralAttribute.getMapKeyTemporal()
			);
		}

		if ( jaxbPluralAttribute.getMapKeyEnumerated() != null ) {
			getOrMakeAnnotation( MapKeyEnumerated.class, memberDetails, documentContext ).setAttributeValue(
					"value",
					jaxbPluralAttribute.getMapKeyEnumerated()
			);
		}

		XmlAnnotationHelper.applyAttributeOverrides(
				jaxbPluralAttribute.getMapKeyAttributeOverrides(),
				memberDetails,
				"key",
				documentContext
		);

		jaxbPluralAttribute.getMapKeyConverts().forEach( (jaxbConvert) -> {
			XmlAnnotationHelper.applyConvert( jaxbConvert, memberDetails, "key", documentContext );
		} );


		// todo : map-key-column, map-key-join-column, map-key-foreign-key
//		XmlAnnotationHelper.applyMapKeyColumn( jaxbPluralAttribute.getMapKeyColumn(), memberDetails, buildingContext );
//
//		jaxbPluralAttribute.getMapKeyJoinColumns().forEach( jaxbMapKeyJoinColumn -> {
//			XmlAnnotationHelper.applyMapKeyJoinColumn( jaxbMapKeyJoinColumn, memberDetails, buildingContext );
//		} );
//
//		XmlAnnotationHelper.applyForeignKey( jaxbPluralAttribute.getMapKeyForeignKey(), memberDetails, buildingContext );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// filters and custom sql

		jaxbPluralAttribute.getFilters().forEach( (jaxbFilter) -> {
			XmlAnnotationHelper.applyFilter( jaxbFilter, memberDetails, documentContext );
		} );

		XmlAnnotationHelper.applySqlRestriction( jaxbPluralAttribute.getSqlRestriction(), memberDetails, documentContext );

		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlInsert(), memberDetails, SQLInsert.class, documentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlUpdate(), memberDetails, SQLUpdate.class, documentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlDelete(), memberDetails, SQLDelete.class, documentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlDeleteAll(), memberDetails, SQLDeleteAll.class, documentContext );
	}
}
