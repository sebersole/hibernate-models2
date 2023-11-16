/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
public class ManyToOneAttributeProcessing {

	public static MutableMemberDetails processManyToOneAttribute(
			JaxbManyToOneImpl jaxbOneToOne,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for many-to-one attributes not yet implemented" );
	}
}
