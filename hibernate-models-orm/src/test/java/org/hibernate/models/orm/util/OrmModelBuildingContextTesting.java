/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.util;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.models.orm.spi.OrmModelBuildingContext;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class OrmModelBuildingContextTesting implements OrmModelBuildingContext {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;
	private final ClassmateContext classmateContext;
	private final SharedCacheMode sharedCacheMode;

	public OrmModelBuildingContextTesting(SourceModelBuildingContext sourceModelBuildingContext) {
		this( sourceModelBuildingContext, null, SharedCacheMode.UNSPECIFIED );
	}

	public OrmModelBuildingContextTesting(
			SourceModelBuildingContext sourceModelBuildingContext,
			ClassmateContext classmateContext,
			SharedCacheMode sharedCacheMode) {
		classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();
		annotationDescriptorRegistry = sourceModelBuildingContext.getAnnotationDescriptorRegistry();
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
