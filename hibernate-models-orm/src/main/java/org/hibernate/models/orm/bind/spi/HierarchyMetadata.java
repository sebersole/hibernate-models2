/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;

/**
 * @author Steve Ebersole
 */
public interface HierarchyMetadata {
	EntityHierarchy getEntityHierarchy();

	Object getCollectedIdAttributes();

	IdMapping getIdMapping();

	AttributeMetadata getVersionAttribute();

	AttributeMetadata getTenantIdAttribute();
}
