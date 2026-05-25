/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bootstrap;

import java.util.LinkedHashMap;

import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.settings.ResolvedMappingSettings;
import org.hibernate.boot.settings.SessionFactorySettingsResolver;

import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Steve Ebersole
 */
public class SessionFactorySettingsResolverTests {
	private final SessionFactorySettingsResolver resolver = new SessionFactorySettingsResolver();

	@Test
	void projectsFromBootstrapSettings() {
		final var configurationValues = new LinkedHashMap<String, Object>();
		configurationValues.put( "hibernate.example", "original" );

		final var bootstrapSettings = new ResolvedBootstrapSettings(
				configurationValues,
				true,
				new ResolvedMappingSettings( true, false, FetchType.EAGER, null )
		);

		final var sessionFactorySettings = resolver.resolve( bootstrapSettings );

		configurationValues.put( "hibernate.example", "changed" );

		assertThat( sessionFactorySettings.jpaBootstrap() ).isTrue();
		assertThat( sessionFactorySettings.configurationValues() )
				.containsEntry( "hibernate.example", "original" );
		assertThatThrownBy( () -> sessionFactorySettings.configurationValues().put( "another", "value" ) )
				.isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	void rejectsNullBootstrapSettings() {
		assertThatThrownBy( () -> resolver.resolve( null ) )
				.isInstanceOf( NullPointerException.class );
	}
}
