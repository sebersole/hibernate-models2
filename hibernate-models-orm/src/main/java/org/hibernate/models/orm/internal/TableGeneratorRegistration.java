/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import org.hibernate.models.source.spi.AnnotationUsage;

import jakarta.persistence.TableGenerator;

/**
 * Global registration of a table generator
 *
 * @see org.hibernate.models.orm.spi.Processor.Options#areGeneratorsGlobal()
 *
 * @author Steve Ebersole
 */
public class TableGeneratorRegistration {
	private final String name;
	private final AnnotationUsage<TableGenerator> configuration;

	public TableGeneratorRegistration(String name, AnnotationUsage<TableGenerator> configuration) {
		this.name = name;
		this.configuration = configuration;
	}

	public String getName() {
		return name;
	}

	public AnnotationUsage<TableGenerator> getConfiguration() {
		return configuration;
	}
}
