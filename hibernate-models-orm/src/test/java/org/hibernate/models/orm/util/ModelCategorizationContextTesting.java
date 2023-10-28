/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.util;

import org.hibernate.models.orm.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class ModelCategorizationContextTesting implements ModelCategorizationContext {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;
	private final SharedCacheMode sharedCacheMode;

	public ModelCategorizationContextTesting(SourceModelBuildingContext sourceModelBuildingContext) {
		this( sourceModelBuildingContext, SharedCacheMode.UNSPECIFIED );
	}

	public ModelCategorizationContextTesting(
			SourceModelBuildingContext sourceModelBuildingContext,
			SharedCacheMode sharedCacheMode) {
		this(
				sourceModelBuildingContext.getClassDetailsRegistry(),
				sourceModelBuildingContext.getAnnotationDescriptorRegistry(),
				sharedCacheMode
		);
	}

	public ModelCategorizationContextTesting(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry) {
		this( classDetailsRegistry, annotationDescriptorRegistry, SharedCacheMode.UNSPECIFIED );
	}

	public ModelCategorizationContextTesting(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			SharedCacheMode sharedCacheMode) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.annotationDescriptorRegistry = annotationDescriptorRegistry;
		this.sharedCacheMode = sharedCacheMode;
	}

	@Override
	public ClassDetailsRegistry getClassDetailsRegistry() {
		return classDetailsRegistry;
	}

	@Override
	public AnnotationDescriptorRegistry getAnnotationDescriptorRegistry() {
		return annotationDescriptorRegistry;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}
}
