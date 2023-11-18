/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyDiscriminatorValueMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingDiscriminatorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.internal.XmlAnnotationHelper;
import org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper;
import org.hibernate.models.orm.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;

import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper.makeAnnotation;

/**
 * @author Steve Ebersole
 */
public class AnyMappingAttributeProcessing {

	public static MutableMemberDetails processAnyMappingAttribute(
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbHbmAnyMapping.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbHbmAnyMapping.getName(),
				accessType,
				declarer
		);

		final MutableAnnotationUsage<Any> anyAnn = makeAnnotation( Any.class, memberDetails );

		CommonProcessing.applyFetching( memberDetails, jaxbHbmAnyMapping, anyAnn );
		CommonProcessing.applyAttributeAccessor( memberDetails, jaxbHbmAnyMapping, anyAnn, xmlDocumentContext );
		CommonProcessing.applyOptimisticLock( memberDetails, jaxbHbmAnyMapping, anyAnn );

		applyDiscriminator( memberDetails, jaxbHbmAnyMapping, anyAnn, xmlDocumentContext );
		applyKey( memberDetails, jaxbHbmAnyMapping, anyAnn, xmlDocumentContext );

		return memberDetails;
	}

	private static void applyDiscriminator(
			MutableMemberDetails memberDetails,
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			MutableAnnotationUsage<Any> anyAnn,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbAnyMappingDiscriminatorImpl jaxbDiscriminator = jaxbHbmAnyMapping.getDiscriminator();
		final MutableAnnotationUsage<AnyDiscriminator> anyDiscriminatorAnn = makeAnnotation( AnyDiscriminator.class, memberDetails );

		if ( jaxbDiscriminator == null ) {
			return;
		}

		final String discriminatorTypeName = jaxbDiscriminator.getType();
		if ( StringHelper.isNotEmpty( discriminatorTypeName ) ) {
			final String normalizedName = discriminatorTypeName.toUpperCase( Locale.ROOT );
			final DiscriminatorType discriminatorType = DiscriminatorType.valueOf( normalizedName );
			anyDiscriminatorAnn.setAttributeValue( "value", discriminatorType );
		}

		final JaxbColumnImpl jaxbColumn = jaxbDiscriminator.getColumn();
		final MutableAnnotationUsage<Column> columnAnn = makeAnnotation( Column.class, memberDetails );
		if ( jaxbColumn != null ) {
			XmlAnnotationHelper.populateColumn( jaxbColumn, memberDetails, columnAnn );
		}

		final List<JaxbAnyDiscriminatorValueMappingImpl> valueMappings = jaxbDiscriminator.getValueMappings();
		if ( CollectionHelper.isNotEmpty( valueMappings ) ) {
			final MutableAnnotationUsage<AnyDiscriminatorValues> valuesAnn = makeAnnotation( AnyDiscriminatorValues.class, memberDetails );
			final List<MutableAnnotationUsage<AnyDiscriminatorValue>> valueList = CollectionHelper.arrayList( valueMappings.size() );
			final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry();
			valuesAnn.setAttributeValue( "value", valueList );
			valueMappings.forEach( (valueMapping) -> {
				final MutableAnnotationUsage<AnyDiscriminatorValue> valueAnn = makeAnnotation( AnyDiscriminatorValue.class );
				valueList.add( valueAnn );

				valueAnn.setAttributeValue( "discriminator", valueMapping.getDiscriminatorValue() );

				final String name = StringHelper.qualifyConditionally(
						xmlDocumentContext.getXmlDocument().getDefaults().getPackage(),
						valueMapping.getCorrespondingEntityName()
				);
				final ClassDetails entityClassDetails = classDetailsRegistry.resolveClassDetails( name );
				valueAnn.setAttributeValue( "entity", entityClassDetails );
			} );
		}
	}

	private static void applyKey(
			MutableMemberDetails memberDetails,
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			MutableAnnotationUsage<Any> anyAnn,
			XmlDocumentContext xmlDocumentContext) {

	}

}
