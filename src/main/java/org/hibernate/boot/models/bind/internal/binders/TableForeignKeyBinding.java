/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;

/// Pending foreign-key binding for a table key value.
///
/// @author Steve Ebersole
public record TableForeignKeyBinding(
		PersistentClass ownerBinding,
		KeyValue key,
		String referencedEntityName,
		ForeignKeySource foreignKeySource) {
}
