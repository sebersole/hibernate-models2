/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.jdbc.Expectation;

/**
 * Custom SQL mutation details.
 *
 * @author Steve Ebersole
 */
public record CustomSqlMapping(
		String sql,
		boolean callable,
		Supplier<? extends Expectation> expectation) {
}
