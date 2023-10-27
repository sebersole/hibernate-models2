/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import java.util.Locale;

import org.hibernate.annotations.UuidGenerator;

/**
 * JAXB marshalling for {@link UuidGenerator.Style}
 */
public class UuidGeneratorStyleMarshalling {
	public static UuidGenerator.Style fromXml(String name) {
		return name == null ? null : UuidGenerator.Style.valueOf( name.toUpperCase( Locale.ROOT ) );
	}

	public static String toXml(UuidGenerator.Style style) {
		return style == null ? null : style.name().toLowerCase( Locale.ROOT );
	}
}
