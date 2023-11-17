/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
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
public class EmbeddedIdAttributeProcessing {

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
		processCommonAttributeAnnotations(
				jaxbEmbeddedId,
				memberDetails,
				accessType,
				sourceModelBuildingContext
		);

		XmlAnnotationHelper.applyAttributeOverrides(
				jaxbEmbeddedId.getAttributeOverrides(),
				memberDetails,
				sourceModelBuildingContext
		);

		return memberDetails;
	}
}
