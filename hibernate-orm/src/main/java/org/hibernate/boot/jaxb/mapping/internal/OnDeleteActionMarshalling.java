/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.annotations.OnDeleteAction;

/**
 * @author Steve Ebersole
 */
public class OnDeleteActionMarshalling {
	public static OnDeleteAction fromXml(String name) {
		return name == null ? null : OnDeleteAction.fromExternalForm( name );
	}

	public static String toXml(OnDeleteAction accessType) {
		return accessType == null ? null : accessType.getAlternativeName();
	}
}
