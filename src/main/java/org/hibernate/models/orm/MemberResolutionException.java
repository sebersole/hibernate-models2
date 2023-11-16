/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

/**
 * Indicates a problem resolving a member from {@linkplain org.hibernate.models.spi.ClassDetails}
 *
 * @author Steve Ebersole
 */
public class MemberResolutionException extends RuntimeException {
	public MemberResolutionException(String message) {
		super( message );
	}

	public MemberResolutionException(String message, Throwable cause) {
		super( message, cause );
	}
}
