/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.process;

import java.util.Iterator;

import org.hibernate.models.orm.internal.ManagedResourcesImpl;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.orm.spi.EntityTypeMetadata;
import org.hibernate.models.orm.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.spi.ManagedResources;
import org.hibernate.models.orm.spi.ProcessResult;
import org.hibernate.models.orm.spi.Processor;
import org.hibernate.models.source.SourceModelTestHelper;
import org.hibernate.models.source.internal.SourceModelBuildingContextImpl;
import org.hibernate.type.CharBooleanConverter;
import org.hibernate.type.YesNoConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;

import org.junit.jupiter.api.Test;

import org.jboss.jandex.Index;

import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.internal.SimpleClassLoading.SIMPLE_CLASS_LOADING;

/**
 * @author Steve Ebersole
 */
public class SimpleProcessorTests {
	@Test
	void testSimpleUsage() {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// ManagedResources is built by scanning and from explicit resources
		// during ORM bootstrap
		final ManagedResourcesImpl.Builder managedResourcesBuilder = new ManagedResourcesImpl.Builder();
		managedResourcesBuilder
				.addLoadedClasses( Person.class, Root.class, Sub.class, MyStringConverter.class, MyUuidConverter.class )
				.addPackages( "org.hibernate.models.orm.process" );
		final ManagedResources managedResources = managedResourcesBuilder.build();
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// The Jandex index would generally (1) be built by WF and passed
		// to ORM or (2) be built by ORM
		final Index jandexIndex = SourceModelTestHelper.buildJandexIndex(
				SIMPLE_CLASS_LOADING,
				Person.class,
				Root.class,
				Sub.class,
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

		final ProcessResult processResult = Processor.process(
				managedResources,
				null,
				new Processor.Options() {
					@Override
					public boolean areGeneratorsGlobal() {
						return false;
					}

					@Override
					public boolean shouldIgnoreUnlistedClasses() {
						return false;
					}
				},
				buildingContext
		);

		assertThat( processResult.getEntityHierarchies() ).hasSize( 2 );
		final Iterator<EntityHierarchy> hierarchies = processResult.getEntityHierarchies().iterator();
		final EntityHierarchy one = hierarchies.next();
		final EntityHierarchy two = hierarchies.next();

		assertThat( one.getRoot() ).isNotNull();
		assertThat( one.getRoot().getClassDetails() ).isNotNull();
		assertThat( one.getRoot().getClassDetails().getClassName() ).isNotNull();
		if ( one.getRoot().getClassDetails().getClassName().endsWith( "Person" ) ) {
			validatePersonHierarchy( one );
			validateJoinedHierarchy( two );
		}
		else {
			validatePersonHierarchy( two );
			validateJoinedHierarchy( one );
		}
	}

	private void validatePersonHierarchy(EntityHierarchy hierarchy) {
		assertThat( hierarchy.getInheritanceType() ).isEqualTo( InheritanceType.SINGLE_TABLE );
		final EntityTypeMetadata personMetadata = hierarchy.getRoot();
		assertThat( personMetadata.getClassDetails().getClassName() ).isEqualTo( Person.class.getName() );
		assertThat( personMetadata.getJpaEntityName() ).isEqualTo( "Person" );
		assertThat( personMetadata.getEntityName() ).isEqualTo( Person.class.getName() );

		assertThat( personMetadata.getSuperType() ).isNull();
		assertThat( personMetadata.hasSubTypes() ).isFalse();
		assertThat( personMetadata.getNumberOfSubTypes() ).isEqualTo( 0 );
	}

	private void validateJoinedHierarchy(EntityHierarchy hierarchy) {
		assertThat( hierarchy.getInheritanceType() ).isEqualTo( InheritanceType.JOINED );
		final EntityTypeMetadata rootMetadata = hierarchy.getRoot();
		assertThat( rootMetadata.getClassDetails().getClassName() ).isEqualTo( Root.class.getName() );
		assertThat( rootMetadata.getJpaEntityName() ).isEqualTo( "Root" );
		assertThat( rootMetadata.getEntityName() ).isEqualTo( Root.class.getName() );

		assertThat( rootMetadata.getSuperType() ).isNull();
		assertThat( rootMetadata.hasSubTypes() ).isTrue();
		assertThat( rootMetadata.getNumberOfSubTypes() ).isEqualTo( 1 );

		final EntityTypeMetadata subMetadata = (EntityTypeMetadata) rootMetadata.getSubTypes().iterator().next();
		assertThat( subMetadata ).isNotNull();
		assertThat( subMetadata.getClassDetails().getClassName() ).isEqualTo( Sub.class.getName() );
		assertThat( subMetadata.getJpaEntityName() ).isEqualTo( "Sub" );
		assertThat( subMetadata.getEntityName() ).isEqualTo( Sub.class.getName() );
		assertThat( subMetadata.getSuperType() ).isEqualTo( rootMetadata );
		assertThat( subMetadata.hasSubTypes() ).isFalse();
		assertThat( subMetadata.getNumberOfSubTypes() ).isEqualTo( 0 );
	}
}
