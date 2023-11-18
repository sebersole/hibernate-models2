/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAnyMappingImpl;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.spi.XmlDocumentContext;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public class PluralAnyMappingAttributeProcessing {

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processPluralAnyMappingAttributes(
			JaxbPluralAnyMappingImpl jaxbHbmManyToAny,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		throw new UnsupportedOperationException( "Support for many-to-any attributes not yet implemented" );
	}
}
