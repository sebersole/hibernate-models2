/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.internal.IndexedConsumer;
import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.FieldDetails;
import org.hibernate.models.source.spi.MethodDetails;

/**
 * @author Steve Ebersole
 */
public interface ClassDetailsSupport extends ClassDetails, AnnotationTargetSupport {

	@Override
	default void forEachField(IndexedConsumer<FieldDetails> consumer) {
		final List<FieldDetails> fields = getFields();
		if ( fields == null ) {
			return;
		}

		for ( int i = 0; i < fields.size(); i++ ) {
			consumer.accept( i, fields.get( i ) );
		}
	}

	@Override
	default void forEachMethod(IndexedConsumer<MethodDetails> consumer) {
		final List<MethodDetails> methods = getMethods();
		if ( methods == null ) {
			return;
		}

		for ( int i = 0; i < methods.size(); i++ ) {
			consumer.accept( i, methods.get( i ) );
		}
	}

	@Override
	default <A extends Annotation> AnnotationUsage<A> getUsage(AnnotationDescriptor<A> type) {
		final AnnotationUsage<A> localUsage = AnnotationTargetSupport.super.getUsage( type );
		if ( localUsage != null ) {
			return localUsage;
		}

		if ( type.isInherited() && getSuperType() != null ) {
			return getSuperType().getUsage( type );
		}

		return null;
	}

	@Override
	default  <A extends Annotation> List<AnnotationUsage<A>> getRepeatedUsages(AnnotationDescriptor<A> type) {
		final List<AnnotationUsage<A>> localUsages = AnnotationTargetSupport.super.getRepeatedUsages( type );

		if ( type.isInherited() && getSuperType() != null ) {
			final List<AnnotationUsage<A>> inheritedUsages = getSuperType().getRepeatedUsages( type );
			return CollectionHelper.join( localUsages, inheritedUsages );
		}

		return localUsages;
	}

	@Override
	default <A extends Annotation> AnnotationUsage<A> getNamedUsage(
			AnnotationDescriptor<A> type,
			String matchValue,
			String attributeToMatch) {
		final AnnotationUsage<A> localUsage = AnnotationTargetSupport.super.getNamedUsage( type, matchValue, attributeToMatch );
		if ( localUsage != null ) {
			return localUsage;
		}

		if ( type.isInherited() && getSuperType() != null ) {
			return getSuperType().getNamedUsage( type, matchValue, attributeToMatch );
		}
		return null;
	}
}
