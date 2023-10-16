/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal;

import java.util.List;

import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.MethodDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

/**
 * Delayed collection of {@linkplain MethodDetails} for a particular target
 *
 * @author Steve Ebersole
 */
public interface MethodDetailsCollector {
	List<MethodDetails> collectMethods(
			ClassDetails declaringType,
			SourceModelBuildingContext buildingContext);
}
