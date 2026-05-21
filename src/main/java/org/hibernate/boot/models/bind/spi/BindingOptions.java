/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

import java.util.EnumSet;

import org.hibernate.boot.model.naming.Identifier;

/// Immutable options that affect how categorized metadata is bound.
///
/// Options here are values computed before binding starts, such as default schema
/// names and the identifier kinds affected by global quoting.
///
/// @author Steve Ebersole
public interface BindingOptions {
	/// Default catalog name to use when a mapping does not specify one.
	Identifier getDefaultCatalogName();

	/// Default schema name to use when a mapping does not specify one.
	Identifier getDefaultSchemaName();

	/// Identifier categories that should be globally quoted during binding.
	EnumSet<QuotedIdentifierTarget> getGloballyQuotedIdentifierTargets();
}
