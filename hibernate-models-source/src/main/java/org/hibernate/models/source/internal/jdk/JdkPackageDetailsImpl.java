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

import org.hibernate.models.source.internal.AnnotationTargetSupport;
import org.hibernate.models.source.spi.AnnotationDescriptor;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.PackageDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

/**
 * @author Steve Ebersole
 */
public class JdkPackageDetailsImpl extends AbstractAnnotationTarget implements PackageDetails, AnnotationTargetSupport {
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
	public <X extends Annotation> void forEachUsage(
			AnnotationDescriptor<X> type,
			Consumer<AnnotationUsage<X>> consumer) {
		super.forEachUsage( type, consumer );
	}

	@Override
	public <X extends Annotation> void forEachUsage(Class<X> type, Consumer<AnnotationUsage<X>> consumer) {
		super.forEachUsage( type, consumer );
	}

	@Override
	public <A extends Annotation> List<AnnotationUsage<A>> getAllUsages(Class<A> annotationType) {
		return getAllUsages( getBuildingContext().getAnnotationDescriptorRegistry().getDescriptor( annotationType ) );
	}
}
