/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jandex;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.PackageDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;

/**
 * @author Steve Ebersole
 */
public class JandexPackageDetails extends AbstractAnnotationTarget implements PackageDetails {
	private final ClassInfo packageInfoClassInfo;

	public JandexPackageDetails(ClassInfo packageInfoClassInfo, SourceModelBuildingContext buildingContext) {
		super( buildingContext );
		this.packageInfoClassInfo = packageInfoClassInfo;
	}

	@Override
	protected AnnotationTarget getJandexAnnotationTarget() {
		return packageInfoClassInfo;
	}

	@Override
	public Kind getKind() {
		return Kind.PACKAGE;
	}

	@Override
	public String getName() {
		return packageInfoClassInfo.name().toString();
	}

	@Override
	public <A extends Annotation> List<AnnotationUsage<A>> getAllAnnotationUsages(Class<A> annotationType) {
		return getAllAnnotationUsages( getBuildingContext().getAnnotationDescriptorRegistry().getDescriptor( annotationType ) );
	}

	@Override
	public <X extends Annotation> void forEachAnnotationUsage(Class<X> type, Consumer<AnnotationUsage<X>> consumer) {
		super.forEachAnnotationUsage( type, consumer );
	}

	@Override
	public <X extends Annotation> void forEachAnnotationUsage(
			AnnotationDescriptor<X> type,
			Consumer<AnnotationUsage<X>> consumer) {
		super.forEachAnnotationUsage( type, consumer );
	}
}
