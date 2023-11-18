/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public class ManyToManyAttributeProcessing {

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processManyToManyAttribute(
			JaxbManyToManyImpl jaxbManyToMany,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		throw new UnsupportedOperationException( "Support for many-to-many attributes not yet implemented" );
	}
}
