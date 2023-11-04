/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.models.ModelsException;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.QuotedIdentifierTarget;
import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.AttributeDescriptor;

import static org.hibernate.models.orm.bind.ModelBindingLogging.MODEL_BINDING_LOGGER;

/**
 * @author Steve Ebersole
 */
public class BindingHelper {
	public static <T, A extends Annotation> T getValue(
			AnnotationUsage<A> annotationUsage,
			String attributeName,
			Class<A> annotationType,
			BindingContext context) {
		final T valueOrNull = getValueOrNull( annotationUsage, attributeName );
		if ( valueOrNull != null ) {
			return valueOrNull;
		}

		// resolve its default
		return getDefaultValue( attributeName, annotationType, context );
	}

	public static <T, A extends Annotation> T getValueOrNull(
			AnnotationUsage<A> annotationUsage,
			String attributeName) {
		if ( annotationUsage != null ) {
			// allow to return null if missing
			return annotationUsage.getAttributeValue( attributeName );
		}

		// there was no annotation...
		return null;
	}

	public static <T,A extends Annotation> T getDefaultValue(
			String attributeName,
			Class<A> annotationType,
			BindingContext context) {
		final AnnotationDescriptor<A> annotationDescriptor = context.getAnnotationDescriptorRegistry().getDescriptor( annotationType );
		final AttributeDescriptor<Object> attributeDescriptor = annotationDescriptor.getAttribute( attributeName );
		//noinspection unchecked
		return (T) attributeDescriptor.getAttributeMethod().getDefaultValue();

	}

	public static <A extends Annotation> String getString(
			AnnotationUsage<A> annotationUsage,
			String attributeName,
			Class<A> annotationType,
			BindingContext context) {
		return getValue( annotationUsage, attributeName, annotationType, context );
	}

	public static <A extends Annotation> String getStringOrNull(
			AnnotationUsage<A> annotationUsage,
			String attributeName) {
		return getValueOrNull( annotationUsage, attributeName );
	}

	public static <A extends Annotation> Identifier getIdentifier(
			AnnotationUsage<A> annotationUsage,
			String attributeName,
			Class<A> annotationType,
			QuotedIdentifierTarget target,
			BindingOptions options,
			JdbcEnvironment jdbcEnvironment,
			BindingContext context) {
		final String name = getString( annotationUsage, attributeName, annotationType, context );
		final boolean globallyQuoted = options.getGloballyQuotedIdentifierTargets().contains( target );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name, globallyQuoted );
	}

	public static <A extends Annotation> Identifier toIdentifier(
			String name,
			QuotedIdentifierTarget target,
			BindingOptions options,
			JdbcEnvironment jdbcEnvironment) {
		final boolean globallyQuoted = options.getGloballyQuotedIdentifierTargets().contains( target );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name, globallyQuoted );
	}

	public static <T,A extends Annotation> T getValue(AnnotationUsage<A> ann, String attributeName, T defaultValue) {
		if ( ann == null ) {
			return defaultValue;
		}

		return ann.getAttributeValue( attributeName, defaultValue );
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
