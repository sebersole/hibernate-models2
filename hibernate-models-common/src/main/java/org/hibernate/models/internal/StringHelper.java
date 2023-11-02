/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.internal;

/**
 * @author Steve Ebersole
 */
public class StringHelper {
	public static final String[] EMPTY_STRINGS = new String[0];

	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	public static boolean isNotEmpty(String string) {
		return string != null && !string.isEmpty();
	}

	public static boolean isBlank(String string) {
		return string == null || string.isBlank();
	}

	public static boolean isNotBlank(String string) {
		return string != null && !string.isBlank();
	}

	public static String nullIfEmpty(String value) {
		return isEmpty( value ) ? null : value;
	}

	public static String classNameToResourceName(String className) {
		return className.replace( '.', '/' ) + ".class";
	}

	public static boolean isQualified(String name) {
		int loc = name.lastIndexOf( '.' );
		return loc > 0;
	}

	public static String qualify(String name, String qualifier) {
		assert isNotEmpty( name );
		return isEmpty( qualifier )
				? name
				: qualifier + "." + name;
	}

	public static String unqualify(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf( '.' );
		return ( loc < 0 ) ? qualifiedName : qualifiedName.substring( loc + 1 );
	}

	public static String qualifier(String qualifiedName) {
		int loc = qualifiedName.lastIndexOf( '.' );
		return ( loc < 0 ) ? "" : qualifiedName.substring( 0, loc );
	}

	public static String qualifyConditionally(String name, String qualifier) {
		assert isNotEmpty( name );
		if ( isEmpty( qualifier ) ) {
			return name;
		}

		int loc = name.indexOf( '.' );
		final String firstQualifier = loc < 0 ? name : name.substring( 0, loc );
		return firstQualifier.equals( qualifier ) ? name : qualifier + "." + name;
	}
}
