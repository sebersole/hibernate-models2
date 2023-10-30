/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.GlobalRegistrations;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.ClassDetailsRegistry;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class BindingContextImpl implements BindingContext {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;
	private final GlobalRegistrations globalRegistrations;
	private final ClassmateContext classmateContext;
	private final SharedCacheMode sharedCacheMode;

	public BindingContextImpl(CategorizedDomainModel categorizedDomainModel, BootstrapContext bootstrapContext) {
		this(
				categorizedDomainModel.getClassDetailsRegistry(),
				categorizedDomainModel.getAnnotationDescriptorRegistry(),
				categorizedDomainModel.getGlobalRegistrations(),
				bootstrapContext.getClassmateContext(),
				bootstrapContext.getMetadataBuildingOptions().getSharedCacheMode()
		);
	}

	public BindingContextImpl(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			GlobalRegistrations globalRegistrations,
			ClassmateContext classmateContext) {
		this( classDetailsRegistry, annotationDescriptorRegistry, globalRegistrations, classmateContext, SharedCacheMode.UNSPECIFIED );
	}

	public BindingContextImpl(
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry,
			GlobalRegistrations globalRegistrations,
			ClassmateContext classmateContext,
			SharedCacheMode sharedCacheMode) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.annotationDescriptorRegistry = annotationDescriptorRegistry;
		this.globalRegistrations = globalRegistrations;
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
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
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
