/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import java.lang.annotation.Annotation;

import org.hibernate.models.orm.HibernateAnnotations;
import org.hibernate.models.orm.JpaAnnotations;
import org.hibernate.models.spi.AnnotationUsage;

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
	private final AnnotationUsage<? extends Annotation> configuration;

	public NamedQueryRegistration(String name, Kind kind, boolean isJpa, AnnotationUsage<? extends Annotation> configuration) {
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

	public AnnotationUsage<? extends Annotation> getConfiguration() {
		return configuration;
	}
}
