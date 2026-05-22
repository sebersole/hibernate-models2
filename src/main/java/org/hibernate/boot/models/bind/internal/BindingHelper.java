/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.QuotedIdentifierTarget;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
/**
 * @author Steve Ebersole
 */
public class BindingHelper {
	public static Identifier toIdentifier(
			String name,
			QuotedIdentifierTarget target,
			BindingOptions options,
			JdbcEnvironment jdbcEnvironment) {
		final boolean globallyQuoted = options.getGloballyQuotedIdentifierTargets().contains( target );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name, globallyQuoted );
	}

	public static String applyGlobalQuoting(
			String text,
			QuotedIdentifierTarget target,
			BindingOptions options,
			BindingState bindingState) {
		final boolean globallyQuoted = options.getGloballyQuotedIdentifierTargets().contains( target );
		if ( !globallyQuoted ) {
			return text;
		}
		final ObjectNameNormalizer objectNameNormalizer = bindingState
				.getMetadataBuildingContext()
				.getObjectNameNormalizer();
		return objectNameNormalizer.applyGlobalQuoting( text );
	}
}
