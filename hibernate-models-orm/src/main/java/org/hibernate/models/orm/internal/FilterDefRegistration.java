/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import java.util.Map;

import org.hibernate.models.source.spi.ClassDetails;

/**
 * Global registration of a filter definition
 *
 * @author Marco Belladelli
 */
public class FilterDefRegistration {
	private final String name;

	private final String defaultCondition;

	private final Map<String, ClassDetails> parameters;

	public FilterDefRegistration(String name, String defaultCondition, Map<String, ClassDetails> parameters) {
		this.name = name;
		this.defaultCondition = defaultCondition;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public String getDefaultCondition() {
		return defaultCondition;
	}

	public Map<String, ClassDetails> getParameters() {
		return parameters;
	}
}
