/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

import org.hibernate.boot.orchestration.SessionFactoryBuilder;
import org.hibernate.boot.settings.SessionFactorySettingsResolver;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.models.orm.bind.SimpleEntity;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class SessionFactoryBuilderTests {
	@Test
	void sessionFactoryBuildTargetIsDefined(ServiceRegistryScope registryScope) {
		final var resolvedMetadata = TestBootModelProducer.resolveMetadata(
				registryScope.getRegistry(),
				new HibernatePersistenceConfiguration( "test" )
						.managedClass( SimpleEntity.class )
		);
		final var sessionFactorySettings = new SessionFactorySettingsResolver()
				.resolve( resolvedMetadata.bootstrapSettings(), registryScope.getRegistry() );

		try (var sessionFactory = new SessionFactoryBuilder().build(
				sessionFactorySettings,
				resolvedMetadata,
				registryScope.getRegistry()
		)) {
			assertThat( sessionFactory ).isNotNull();
			assertThat( sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class ) ).isNotNull();
		}
	}
}
