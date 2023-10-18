/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.models.source.spi.AnnotationUsage;

/**
 * Global registration of a generic generator
 *
 * @see org.hibernate.models.orm.spi.Processor.Options#areGeneratorsGlobal()
 *
 * @author Steve Ebersole
 */
public class GenericGeneratorRegistration {
	private final String name;
	private final AnnotationUsage<GenericGenerator> configuration;

	public GenericGeneratorRegistration(String name, AnnotationUsage<GenericGenerator> configuration) {
		this.name = name;
		this.configuration = configuration;
	}

	public String getName() {
		return name;
	}

	public AnnotationUsage<GenericGenerator> getConfiguration() {
		return configuration;
	}
}
