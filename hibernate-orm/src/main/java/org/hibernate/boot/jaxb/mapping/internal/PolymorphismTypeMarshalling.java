/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.annotations.PolymorphismType;

/**
 * @author Steve Ebersole
 */
public class PolymorphismTypeMarshalling {
	public static PolymorphismType fromXml(String value) {
		return value == null ? null : PolymorphismType.fromExternalValue( value );
	}

	public static String toXml(PolymorphismType value) {
		return value == null ? null : value.getExternalForm();
	}
}
