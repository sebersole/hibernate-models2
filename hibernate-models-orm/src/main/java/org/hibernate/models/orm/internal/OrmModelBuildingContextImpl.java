/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.models.orm.spi.OrmModelBuildingContext;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.SourceModelContext;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class OrmModelBuildingContextImpl implements OrmModelBuildingContext {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;
	private final ClassmateContext classmateContext;
	private final SharedCacheMode sharedCacheMode;

	public OrmModelBuildingContextImpl(
			SourceModelContext sourceModelContext,
			ClassmateContext classmateContext) {
		this(
				sourceModelContext.getClassDetailsRegistry(),
				sourceModelContext.getAnnotationDescriptorRegistry(),
				classmateContext
		);
	}

	public OrmModelBuildingContextImpl(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			ClassmateContext classmateContext) {
		this( classDetailsRegistry, annotationDescriptorRegistry, classmateContext, SharedCacheMode.UNSPECIFIED );
	}

	public OrmModelBuildingContextImpl(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			ClassmateContext classmateContext,
			SharedCacheMode sharedCacheMode) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.annotationDescriptorRegistry = annotationDescriptorRegistry;
		this.classmateContext = classmateContext;
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
	public ClassmateContext getClassmateContext() {
		return classmateContext;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}
}
