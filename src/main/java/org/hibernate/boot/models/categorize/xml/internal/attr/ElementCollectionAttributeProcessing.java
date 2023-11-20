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
		XmlProcessingHelper.setIf( jaxbElementCollection.getFetch(), "fetch", elementCollectionAnn );
		if ( StringHelper.isNotEmpty( jaxbElementCollection.getTargetClass() ) ) {
			elementCollectionAnn.setAttributeValue(
					"targetClass",
					XmlAnnotationHelper.resolveJavaType(
							jaxbElementCollection.getTargetClass(),
							xmlDocumentContext.getModelBuildingContext()
					)
			);
		}

		CommonAttributeProcessing.applyAttributeBasics( jaxbElementCollection, memberDetails, elementCollectionAnn, accessType, xmlDocumentContext );

		CommonPluralAttributeProcessing.applyPluralAttributeStructure( jaxbElementCollection, memberDetails, xmlDocumentContext );

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

		XmlAnnotationHelper.applyAttributeOverrides(
				jaxbElementCollection.getAttributeOverrides(),
				memberDetails,
				"value",
				xmlDocumentContext
		);

		XmlAnnotationHelper.applyAssociationOverrides(
				jaxbElementCollection.getAssociationOverrides(),
				memberDetails,
				"value",
				xmlDocumentContext
		);

		return memberDetails;
	}
}
