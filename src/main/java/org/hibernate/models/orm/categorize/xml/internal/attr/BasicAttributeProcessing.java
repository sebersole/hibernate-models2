/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.internal.XmlAnnotationHelper;
import org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper;
import org.hibernate.models.orm.categorize.xml.spi.XmlDocumentContext;

import jakarta.persistence.AccessType;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.orm.categorize.xml.internal.attr.CommonAttributeProcessing.processCommonAttributeAnnotations;

/**
 * @author Steve Ebersole
 */
public class BasicAttributeProcessing {

	public static MutableMemberDetails processBasicAttribute(
			JaxbBasicImpl jaxbBasic,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbBasic.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbBasic.getName(),
				accessType,
				declarer
		);

		XmlAnnotationHelper.applyBasic( jaxbBasic, memberDetails );

		processCommonAttributeAnnotations( jaxbBasic, memberDetails, accessType );
		// only semi-common
		XmlAnnotationHelper.applyOptimisticLockInclusion( jaxbBasic.isOptimisticLock(), memberDetails );

		XmlAnnotationHelper.applyColumn( jaxbBasic.getColumn(), memberDetails );
		XmlAnnotationHelper.applyFormula( jaxbBasic.getFormula(), memberDetails );

		// todo : value generation

		XmlAnnotationHelper.applyConvert( jaxbBasic.getConvert(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyBasicTypeComposition( jaxbBasic, memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyTemporal( jaxbBasic.getTemporal(), memberDetails );
		XmlAnnotationHelper.applyLob( jaxbBasic.getLob(), memberDetails );
		XmlAnnotationHelper.applyEnumerated( jaxbBasic.getEnumerated(), memberDetails );
		XmlAnnotationHelper.applyNationalized( jaxbBasic.getNationalized(), memberDetails );

		return memberDetails;
	}
}
