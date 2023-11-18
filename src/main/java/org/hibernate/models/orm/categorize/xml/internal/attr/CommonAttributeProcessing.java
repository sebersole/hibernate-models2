/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.internal.XmlAnnotationHelper;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public class CommonAttributeProcessing {

	public static void processCommonAttributeAnnotations(
			JaxbPersistentAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			AccessType accessType) {
		XmlAnnotationHelper.applyAccess( accessType, memberDetails );

		// todo : optimistic-lock
	}
}
