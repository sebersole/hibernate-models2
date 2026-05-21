/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize;

import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.boot.models.categorize.internal.EntityHierarchyBuilder;
import org.hibernate.boot.models.categorize.internal.GlobalRegistrationsImpl;
import org.hibernate.boot.models.categorize.internal.CategorizationContextImpl;
import org.hibernate.boot.models.categorize.internal.ManagedTypeInheritanceState;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizationContext;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.boot.models.categorize.internal.ManagedTypeInheritanceState.MissingPersistentSuperclassHandling.WARN_AND_IGNORE;
import static org.hibernate.boot.models.categorize.internal.ManagedTypeInheritanceState.MissingPersistentSuperclassHandling.WARN_AND_USE;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class ManagedTypeInheritanceStateTests {
	@Test
	void graphOnlyConsidersAvailableClassDetails(ServiceRegistryScope registryScope) {
		final TestContext testContext = buildCategorizationContext( registryScope.getRegistry() );
		final CategorizationContext categorizationContext = testContext.categorizationContext();
		final ModelsContext modelsContext = testContext.modelsContext();

		final ClassDetails root = resolve( modelsContext, Root.class );
		final ClassDetails included = resolve( modelsContext, IncludedLeaf.class );
		resolve( modelsContext, ExcludedLeaf.class );

		final ManagedTypeInheritanceState inheritanceState = new ManagedTypeInheritanceState( Set.of( root, included ) );
		final var entityHierarchies = EntityHierarchyBuilder.createEntityHierarchies(
				inheritanceState,
				categorizationContext
		);

		assertThat( entityHierarchies ).hasSize( 1 );
		final var hierarchy = entityHierarchies.iterator().next();
		assertThat( hierarchy.getRoot().getClassDetails().getClassName() ).isEqualTo( Root.class.getName() );
		assertThat( hierarchy.getRoot().getSubTypes() )
				.extracting( IdentifiableTypeMetadata::getClassDetails )
				.extracting( ClassDetails::getClassName )
				.containsExactly( IncludedLeaf.class.getName() );
	}

	@Test
	void graphSkipsNonPersistentIntermediateSuperclasses(ServiceRegistryScope registryScope) {
		final TestContext testContext = buildCategorizationContext( registryScope.getRegistry() );
		final CategorizationContext categorizationContext = testContext.categorizationContext();
		final ModelsContext modelsContext = testContext.modelsContext();

		final ClassDetails mappedSuper = resolve( modelsContext, ModelBase.class );
		final ClassDetails entity = resolve( modelsContext, EntityPastIntermediate.class );

		final ManagedTypeInheritanceState inheritanceState = new ManagedTypeInheritanceState( Set.of( mappedSuper, entity ) );
		final var entityHierarchies = EntityHierarchyBuilder.createEntityHierarchies(
				inheritanceState,
				categorizationContext
		);

		assertThat( entityHierarchies ).hasSize( 1 );
		final var root = entityHierarchies.iterator().next().getRoot();
		assertThat( root.getClassDetails().getClassName() ).isEqualTo( EntityPastIntermediate.class.getName() );
		assertThat( root.getSuperType().getClassDetails().getClassName() ).isEqualTo( ModelBase.class.getName() );
	}

	@Test
	void graphRejectsMissingPersistentSuperclass(ServiceRegistryScope registryScope) {
		final TestContext testContext = buildCategorizationContext( registryScope.getRegistry() );
		final ModelsContext modelsContext = testContext.modelsContext();

		final ClassDetails included = resolve( modelsContext, IncludedLeaf.class );

		assertThatThrownBy( () -> new ManagedTypeInheritanceState( Set.of( included ) ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( Root.class.getName() )
				.hasMessageContaining( IncludedLeaf.class.getName() );
	}

	@Test
	void graphCanIgnoreMissingPersistentSuperclass(ServiceRegistryScope registryScope) {
		final TestContext testContext = buildCategorizationContext( registryScope.getRegistry() );
		final ModelsContext modelsContext = testContext.modelsContext();

		final ClassDetails included = resolve( modelsContext, IncludedLeaf.class );

		final ManagedTypeInheritanceState inheritanceState = new ManagedTypeInheritanceState( Set.of( included ), WARN_AND_IGNORE );

		assertThat( inheritanceState.getSuperType( included ) ).isNull();
		assertThat( inheritanceState.getRootEntities() )
				.extracting( ClassDetails::getClassName )
				.containsExactly( IncludedLeaf.class.getName() );
	}

	@Test
	void graphCanUseMissingPersistentSuperclass(ServiceRegistryScope registryScope) {
		final TestContext testContext = buildCategorizationContext( registryScope.getRegistry() );
		final ModelsContext modelsContext = testContext.modelsContext();

		final ClassDetails root = resolve( modelsContext, Root.class );
		final ClassDetails included = resolve( modelsContext, IncludedLeaf.class );

		final ManagedTypeInheritanceState inheritanceState = new ManagedTypeInheritanceState( Set.of( included ), WARN_AND_USE );

		assertThat( inheritanceState.getSuperType( included ) ).isSameAs( root );
		assertThat( inheritanceState.getRootEntities() )
				.extracting( ClassDetails::getClassName )
				.containsExactly( Root.class.getName() );
	}

	private static TestContext buildCategorizationContext(StandardServiceRegistry serviceRegistry) {
		final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
		final ModelsContext modelsContext = metadataBuildingContext.getBootstrapContext().getModelsContext();
		return new TestContext( modelsContext, new CategorizationContextImpl(
				metadataBuildingContext,
				new GlobalRegistrationsImpl( modelsContext )
		) );
	}

	private record TestContext(ModelsContext modelsContext, CategorizationContext categorizationContext) {
	}

	private static ClassDetails resolve(ModelsContext modelsContext, Class<?> javaClass) {
		return modelsContext.getClassDetailsRegistry().resolveClassDetails( javaClass.getName() );
	}

	@Entity
	public static class Root {
		@Id
		private Long id;
	}

	@Entity
	public static class IncludedLeaf extends Root {
		private String included;
	}

	@Entity
	public static class ExcludedLeaf extends Root {
		private String excluded;
	}

	@MappedSuperclass
	public static class ModelBase {
		@Id
		private Long id;
	}

	public static class NonPersistentIntermediate extends ModelBase {
	}

	@Entity
	public static class EntityPastIntermediate extends NonPersistentIntermediate {
		private String name;
	}
}
