/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class MappedSuperBindingTests {
	@Test
	@ServiceRegistry
	void testMappedSuper(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var bindingState = context.getBindingState();
					final var metadataCollector = context.getMetadataCollector();

					final MappedSuperclass mappedSuper = metadataCollector.getMappedSuperclass( HierarchySuper.class );
					assertThat( mappedSuper ).isNotNull();

					final PersistentClass entityBinding = metadataCollector.getEntityBinding( HierarchyRoot.class.getName() );
					assertThat( entityBinding ).isNotNull();
					assertThat( entityBinding.getSuperPersistentClass() ).isNull();
					assertThat( entityBinding.getSuperType() ).isEqualTo( mappedSuper );
					assertThat( entityBinding.getSuperMappedSuperclass() ).isEqualTo( mappedSuper );
					assertThat( entityBinding.getCallbackDefinitions() ).hasSize( 3 );
				},
				scope.getRegistry(),
				HierarchySuper.class,
				HierarchyRoot.class
		);

	}
}
