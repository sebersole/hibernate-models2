/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.internal.XmlAnnotationHelper;
import org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.orm.categorize.xml.internal.attr.CommonAttributeProcessing.processCommonAttributeAnnotations;

/**
 * @author Steve Ebersole
 */
public class BasicIdAttributeProcessing {

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
		processCommonAttributeAnnotations(
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
}
