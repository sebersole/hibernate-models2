/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import java.util.Locale;

import org.hibernate.CacheMode;

/**
 * JAXB marshalling for Hibernate's {@link CacheMode}
 *
 * @author Steve Ebersole
 */
public class CacheModeMarshalling {
	public static CacheMode fromXml(String name) {
		for ( CacheMode mode : CacheMode.values() ) {
			if ( mode.name().equalsIgnoreCase( name ) ) {
				return mode;
			}
		}
		return CacheMode.NORMAL;
	}

	public static String toXml(CacheMode cacheMode) {
		return cacheMode == null ? null : cacheMode.name().toLowerCase( Locale.ENGLISH );
	}
}
