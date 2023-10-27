/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.metamodel.CollectionClassification;

/**
 * JAXB marshalling for {@link CollectionClassification}
 *
 * @author Steve Ebersole
 */
public class CollectionClassificationMarshalling {
	public static CollectionClassification fromXml(String name) {
		return name == null ? null : CollectionClassification.interpretSetting( name.replace( '-', '_' ) );
	}

	public static String toXml(CollectionClassification classification) {
		return classification == null ? null : classification.name().replace( '_', '-' );
	}
}
