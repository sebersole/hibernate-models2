/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.process;

import org.hibernate.models.orm.internal.ManagedResourcesImpl;
import org.hibernate.models.orm.spi.ManagedResources;
import org.hibernate.models.source.SourceModelTestHelper;
import org.hibernate.models.source.internal.SourceModelBuildingContextImpl;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.type.CharBooleanConverter;
import org.hibernate.type.YesNoConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * @author Steve Ebersole
 */
public class ProcessorSmokeTests {
	@Test
	void simpleTest() {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ManagedResources is built by scanning and from explicit resources
		// during ORM bootstrap
		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder
				.addLoadedClasses( Person.class, MyStringConverter.class )
				.addPackages( "org.hibernate.models.orm.process" );
		final ManagedResources managedResources = managedResourcesBuilder.build();
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// The Jandex index would generally (1) be built by WF and passed
		// to ORM or (2) be built by ORM
		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				Person.class,
				MyStringConverter.class,
				MyUuidConverter.class,
				YesNoConverter.class,
				CharBooleanConverter.class,
				BasicValueConverter.class,
				StringJavaType.class,
				AbstractClassJavaType.class
		);
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Above here is work done before hibernate-models.
		// Below here is work done by hibernate-models.
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final SourceModelBuildingContextImpl buildingContext = SourceModelTestHelper.createBuildingContext(
				jandexIndex,
				SIMPLE_CLASS_LOADING
		);
		final ClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry();
		final ClassDetails personClassDetails = classDetailsRegistry.findClassDetails( Person.class.getName() );
		assertThat( personClassDetails ).isNotNull();
	}

}
