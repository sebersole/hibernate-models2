/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.internal.XmlAnnotationHelper;
import org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper;
import org.hibernate.models.orm.categorize.xml.spi.XmlDocumentContext;

import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Id;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper.makeAnnotation;
import static org.hibernate.models.orm.categorize.xml.internal.attr.CommonAttributeProcessing.applyAttributeBasics;

/**
 * @author Steve Ebersole
 */
public class BasicIdAttributeProcessing {

	public static MutableMemberDetails processBasicIdAttribute(
			JaxbIdImpl jaxbId,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbId.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbId.getName(),
				accessType,
				declarer
		);

		final MutableAnnotationUsage<Id> idAnn = makeAnnotation( Id.class, memberDetails, xmlDocumentContext );
		final MutableAnnotationUsage<Basic> basicAnn = makeAnnotation( Basic.class, memberDetails, xmlDocumentContext );

		applyAttributeBasics( jaxbId, memberDetails, basicAnn, accessType, xmlDocumentContext );

		XmlAnnotationHelper.applyColumn( jaxbId.getColumn(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyBasicTypeComposition( jaxbId, memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyTemporal( jaxbId.getTemporal(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyGeneratedValue( jaxbId.getGeneratedValue(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applySequenceGenerator( jaxbId.getSequenceGenerator(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyTableGenerator( jaxbId.getTableGenerator(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyUuidGenerator( jaxbId.getUuidGenerator(), memberDetails, xmlDocumentContext );

		// todo : unsaved-value?
		// todo : ...

		return memberDetails;
	}
}
