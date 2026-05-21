/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

/// Table reference backed by a persistent relational object.
///
/// Persistent table references have catalog and schema names and may be exported to
/// schema tooling.  Inline views are table references, but they are not persistent
/// table references.
///
/// @author Steve Ebersole
public interface PersistentTableReference extends TableReference, SchemaAware {
}
