/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public class OneToManyAttributeProcessing {

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processOneToManyAttribute(
			JaxbOneToManyImpl jaxbOneToMany,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for one-to-many attributes not yet implemented" );
	}
}
