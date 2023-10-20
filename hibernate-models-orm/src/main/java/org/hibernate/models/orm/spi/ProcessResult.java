/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.spi;

import java.util.Set;

/**
 * The result of {@linkplain Processor#process processing} the domain model
 *
 * @author Steve Ebersole
 */
public interface ProcessResult {
	Set<EntityHierarchy> getEntityHierarchies();

	GlobalRegistrations getGlobalRegistrations();
}
