/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.util.EnumSet;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.QuotedIdentifierTarget;

/**
 * @author Steve Ebersole
 */
public class BindingOptionsImpl implements BindingOptions {
	private final Identifier defaultCatalogName;
	private final Identifier defaultSchemaName;
	private final EnumSet<QuotedIdentifierTarget> globallyQuotedIdentifierTargets;

	public BindingOptionsImpl(Identifier defaultCatalogName, Identifier defaultSchemaName) {
		this( defaultCatalogName, defaultSchemaName, EnumSet.noneOf( QuotedIdentifierTarget.class ) );
	}

	public BindingOptionsImpl(
			Identifier defaultCatalogName,
			Identifier defaultSchemaName,
			EnumSet<QuotedIdentifierTarget> globallyQuotedIdentifierTargets) {
		this.defaultCatalogName = defaultCatalogName;
		this.defaultSchemaName = defaultSchemaName;
		this.globallyQuotedIdentifierTargets = globallyQuotedIdentifierTargets;
	}

	@Override
	public Identifier getDefaultCatalogName() {
		return defaultCatalogName;
	}

	@Override
	public Identifier getDefaultSchemaName() {
		return defaultSchemaName;
	}

	@Override
	public EnumSet<QuotedIdentifierTarget> getGloballyQuotedIdentifierTargets() {
		return globallyQuotedIdentifierTargets;
	}
}
