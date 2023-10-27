/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.models.ModelsException;
import org.hibernate.models.source.UnknownClassException;
import org.hibernate.models.source.internal.jandex.JandexBuilders;
import org.hibernate.models.source.internal.jdk.JdkBuilders;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsBuilder;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.PackageDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import org.jboss.jandex.IndexView;

/**
 * Standard ClassDetailsRegistry implementation.
 *
 * @author Steve Ebersole
 */
public class ClassDetailsRegistryStandard extends AbstractClassDetailsRegistry {
	private final StandardClassDetailsBuilder standardClassDetailsBuilder;
	private final SourceModelBuildingContext context;

	public ClassDetailsRegistryStandard(SourceModelBuildingContext context) {
		this.context = context;
		this.standardClassDetailsBuilder = new StandardClassDetailsBuilder( JdkBuilders.DEFAULT_BUILDER, context.getJandexIndex() );
	}

	@Override
	public void addClassDetails(ClassDetails classDetails) {
		addClassDetails( classDetails.getClassName(), classDetails );
	}

	@Override
	public void addClassDetails(String name, ClassDetails classDetails) {
		if ( name.endsWith( "package-info" ) ) {
			throw new ModelsException( "Register " + name + " as a package, not a class" );
		}
		classDetailsMap.put( name, classDetails );

		if ( classDetails.getSuperType() != null ) {
			List<ClassDetails> subTypes = subTypeClassDetailsMap.get( classDetails.getSuperType().getName() );
			if ( subTypes == null ) {
				subTypes = new ArrayList<>();
				subTypeClassDetailsMap.put( classDetails.getSuperType().getName(), subTypes );
			}
			subTypes.add( classDetails );
		}
	}

	@Override
	public ClassDetails resolveClassDetails(String name) {
		return resolveClassDetails( name, standardClassDetailsBuilder );
	}

	@Override
	protected ClassDetails createClassDetails(String name, ClassDetailsBuilder creator) {
		final ClassDetails created = creator.buildClassDetails( name, context );
		addClassDetails( name, created );
		return created;
	}

	@Override
	protected ClassDetails createClassDetails(String name, ClassDetailsCreator creator) {
		final ClassDetails created = creator.createClassDetails();
		addClassDetails( name, created );
		return created;
	}

	@Override
	protected PackageDetails createPackageDetails(String packageName, PackageDetailsCreator creator) {
		final PackageDetails created = creator.createPackageDetails();
		packageDetailsMap.put( packageName, created );
		return created;
	}

	private static class StandardClassDetailsBuilder implements ClassDetailsBuilder {
		private final boolean tryJandex;
		private final ClassDetailsBuilder fallbackClassDetailsBuilder;

		public StandardClassDetailsBuilder(ClassDetailsBuilder fallbackClassDetailsBuilder, IndexView jandexIndex) {
			this.fallbackClassDetailsBuilder = fallbackClassDetailsBuilder;
			this.tryJandex = jandexIndex != null;
		}

		@Override
		public ClassDetails buildClassDetails(String name, SourceModelBuildingContext buildingContext) {
			if ( tryJandex ) {
				try {
					return JandexBuilders.buildClassDetailsStatic( name, buildingContext );
				}
				catch (UnknownClassException e) {
					// generally means the class is not in the Jandex index - try the fallback
				}
			}

			return fallbackClassDetailsBuilder.buildClassDetails( name, buildingContext );
		}
	}

	@Override
	public ClassDetailsRegistry makeImmutableCopy() {
		return new ClassDetailsRegistryImmutable( classDetailsMap, subTypeClassDetailsMap, packageDetailsMap );
	}
}
