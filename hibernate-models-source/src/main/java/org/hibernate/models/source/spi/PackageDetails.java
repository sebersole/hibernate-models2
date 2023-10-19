/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.spi;

import java.lang.annotation.Annotation;
import java.util.function.Consumer;

/**
 * Descriptor for a Java package, mainly to act as AnnotationTarget.
 * <p/>
 * Effectively a reference to the package's {@code package-info} class, if one
 *
 * @author Steve Ebersole
 */
public interface PackageDetails extends AnnotationTarget, SharedNamedAnnotationScope {
	@Override
	default Kind getKind() {
		return Kind.PACKAGE;
	}

	String getName();

	@Override
	<X extends Annotation> void forEachAnnotationUsage(AnnotationDescriptor<X> type, Consumer<AnnotationUsage<X>> consumer);
}
