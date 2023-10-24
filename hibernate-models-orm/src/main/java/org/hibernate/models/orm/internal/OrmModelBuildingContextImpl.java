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
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.ClassLoading;

import org.jboss.jandex.IndexView;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class OrmModelBuildingContextImpl implements OrmModelBuildingContext {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;
	private final ClassLoading classLoading;
	private final IndexView jandexIndex;
	private final ClassmateContext classmateContext;
	private final SharedCacheMode sharedCacheMode;

	public OrmModelBuildingContextImpl(
			SourceModelBuildingContext sourceModelBuildingContext,
			ClassmateContext classmateContext) {
		this(
				sourceModelBuildingContext.getClassDetailsRegistry(),
				sourceModelBuildingContext.getAnnotationDescriptorRegistry(),
				sourceModelBuildingContext.getClassLoading(),
				sourceModelBuildingContext.getJandexIndex(),
				classmateContext,
				SharedCacheMode.UNSPECIFIED
		);
	}

	public OrmModelBuildingContextImpl(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			ClassLoading classLoading,
			IndexView jandexIndex,
			ClassmateContext classmateContext,
			SharedCacheMode sharedCacheMode) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.annotationDescriptorRegistry = annotationDescriptorRegistry;
		this.classLoading = classLoading;
		this.jandexIndex = jandexIndex;
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
	public ClassLoading getClassLoading() {
		return classLoading;
	}

	@Override
	public IndexView getJandexIndex() {
		return jandexIndex;
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
