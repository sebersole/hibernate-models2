/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.annotations.NotFoundAction;

/**
 * @author Marco Belladelli
 */
public class NotFoundActionMarshalling {
	public static NotFoundAction fromXml(String name) {
		return name == null ? null : NotFoundAction.valueOf( name );
	}

	public static String toXml(NotFoundAction notFoundAction) {
		return notFoundAction == null ? null : notFoundAction.name();
	}
}
