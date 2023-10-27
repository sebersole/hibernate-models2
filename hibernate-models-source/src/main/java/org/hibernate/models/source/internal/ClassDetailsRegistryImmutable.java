/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal;

import java.util.List;
import java.util.Map;

import org.hibernate.models.ModelsException;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsBuilder;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.PackageDetails;

/**
 * @author Steve Ebersole
 */
public class ClassDetailsRegistryImmutable extends AbstractClassDetailsRegistry {
	public ClassDetailsRegistryImmutable(
			Map<String, ClassDetails> classDetailsMap,
			Map<String, List<ClassDetails>> subTypeClassDetailsMap,
			Map<String, PackageDetails> packageDetailsMap) {
		super( classDetailsMap, subTypeClassDetailsMap, packageDetailsMap );
	}

	@Override
	protected ClassDetails createClassDetails(String name, ClassDetailsBuilder creator) {
		throw new ModelsException( "ClassDetailsRegistry is immutable" );
	}

	@Override
	protected ClassDetails createClassDetails(String name, ClassDetailsCreator creator) {
		throw new ModelsException( "ClassDetailsRegistry is immutable" );
	}

	@Override
	protected PackageDetails createPackageDetails(String packageName, PackageDetailsCreator creator) {
		throw new ModelsException( "ClassDetailsRegistry is immutable" );
	}

	@Override
	public void addClassDetails(ClassDetails classDetails) {
		throw new ModelsException( "ClassDetailsRegistry is immutable" );
	}

	@Override
	public void addClassDetails(String name, ClassDetails classDetails) {
		throw new ModelsException( "ClassDetailsRegistry is immutable" );
	}

	@Override
	public ClassDetails resolveClassDetails(String name) {
		return resolveClassDetails( name, (ClassDetailsCreator) null );
	}

	@Override
	public ClassDetailsRegistry makeImmutableCopy() {
		return this;
	}
}
