/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jdk;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.PackageDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

/**
 * @author Steve Ebersole
 */
public class JdkPackageDetailsImpl extends AbstractAnnotationTarget implements PackageDetails {
	private final Package packageInfo;

	public JdkPackageDetailsImpl(Package packageInfo, SourceModelBuildingContext buildingContext) {
		super( packageInfo::getDeclaredAnnotations, buildingContext );
		this.packageInfo = packageInfo;
	}

	@Override
	public String getName() {
		return packageInfo.getName();
	}

	@Override
	public <X extends Annotation> void forEachAnnotationUsage(
			AnnotationDescriptor<X> type,
			Consumer<AnnotationUsage<X>> consumer) {
		super.forEachAnnotationUsage( type, consumer );
	}

	@Override
	public <X extends Annotation> void forEachAnnotationUsage(Class<X> type, Consumer<AnnotationUsage<X>> consumer) {
		super.forEachAnnotationUsage( type, consumer );
	}

	@Override
	public <A extends Annotation> List<AnnotationUsage<A>> getAllAnnotationUsages(Class<A> annotationType) {
		return getAllAnnotationUsages( getBuildingContext().getAnnotationDescriptorRegistry().getDescriptor( annotationType ) );
	}
}
