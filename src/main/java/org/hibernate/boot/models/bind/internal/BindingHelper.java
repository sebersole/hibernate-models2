/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.QuotedIdentifierTarget;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.models.ModelsException;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;

import static org.hibernate.boot.models.bind.ModelBindingLogging.MODEL_BINDING_LOGGER;

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

	public static void processSecondPassQueue(List<? extends SecondPass> secondPasses) {
		if ( secondPasses == null ) {
			return;
		}

		int processedCount = 0;
		final Iterator<? extends SecondPass> secondPassItr = secondPasses.iterator();
		while ( secondPassItr.hasNext() ) {
			final SecondPass secondPass = secondPassItr.next();
			try {
				final boolean success = secondPass.process();
				if ( success ) {
					processedCount++;
					secondPassItr.remove();
				}
			}
			catch (Exception e) {
				MODEL_BINDING_LOGGER.debug( "Error processing second pass", e );
			}
		}

		if ( !secondPasses.isEmpty() ) {
			if ( processedCount == 0 ) {
				// there are second-passes in the queue, but we were not able to
				// successfully process any of them.  this is a non-changing
				// error condition - just throw an exception
				throw new ModelsException( "Unable to process second-pass list" );
			}

			processSecondPassQueue( secondPasses );
		}
	}
}
