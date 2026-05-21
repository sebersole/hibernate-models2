/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.lang.annotation.Annotation;

/**
 * @see JpaAnnotations#NAMED_QUERY
 * @see JpaAnnotations#NAMED_NATIVE_QUERY
 * @see JpaAnnotations#NAMED_STORED_PROCEDURE_QUERY
 * @see HibernateAnnotations#NAMED_QUERY
 * @see HibernateAnnotations#NAMED_NATIVE_QUERY
 *
 * @author Steve Ebersole
 */
public class NamedQueryRegistration {
	public enum Kind {
		HQL,
		NATIVE,
		CALLABLE
	}

	private final String name;
	private final Kind kind;
	private final boolean isJpa;
	private final Annotation configuration;

	public NamedQueryRegistration(String name, Kind kind, boolean isJpa, Annotation configuration) {
		this.name = name;
		this.kind = kind;
		this.isJpa = isJpa;
		this.configuration = configuration;
	}

	public String getName() {
		return name;
	}

	public Kind getKind() {
		return kind;
	}

	public boolean isJpa() {
		return isJpa;
	}

	public Annotation getConfiguration() {
		return configuration;
	}
}
