/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.callbacks;

import jakarta.persistence.PostLoad;

/**
 * @author Steve Ebersole
 */
public class Listener1 {
	@PostLoad
	public void wasLoaded(HierarchySuper entity) {}
}
