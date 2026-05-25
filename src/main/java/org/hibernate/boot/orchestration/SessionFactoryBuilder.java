/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration;

import java.util.Objects;

import org.hibernate.boot.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.settings.ResolvedSessionFactorySettings;
import org.hibernate.boot.settings.SessionFactorySettingsResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.ServiceRegistry;

/**
 * Builds a runtime SessionFactoryImplementor from resolved boot products.
 *
 * This is the gross target after metadata resolution.  Unlike settings and
 * metadata, the final product is not a "resolved" description; it is the runtime
 * {@link SessionFactoryImplementor} itself.
 *
 * @author Steve Ebersole
 */
public class SessionFactoryBuilder {
	private final SessionFactorySettingsResolver settingsResolver = new SessionFactorySettingsResolver();

	/**
	 * Build a SessionFactoryImplementor from the resolved bootstrap settings root.
	 *
	 * @param bootstrapSettings Resolved bootstrap settings
	 * @param resolvedMetadata Resolved ORM metadata
	 * @param serviceRegistry Service registry for the factory build
	 *
	 * @return The runtime SessionFactoryImplementor
	 */
	public SessionFactoryImplementor build(
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMetadata resolvedMetadata,
			ServiceRegistry serviceRegistry) {
		return build(
				settingsResolver.resolve( bootstrapSettings ),
				resolvedMetadata,
				serviceRegistry
		);
	}

	/**
	 * Build a SessionFactoryImplementor from resolved factory settings and metadata.
	 *
	 * @param sessionFactorySettings Resolved SessionFactory settings
	 * @param resolvedMetadata Resolved ORM metadata
	 * @param serviceRegistry Service registry for the factory build
	 *
	 * @return The runtime SessionFactoryImplementor
	 */
	public SessionFactoryImplementor build(
			ResolvedSessionFactorySettings sessionFactorySettings,
			ResolvedMetadata resolvedMetadata,
			ServiceRegistry serviceRegistry) {
		Objects.requireNonNull( sessionFactorySettings );
		Objects.requireNonNull( resolvedMetadata );
		Objects.requireNonNull( serviceRegistry );
		throw new UnsupportedOperationException(
				"SessionFactory construction is not implemented yet; SessionFactoryOptions resolution is the next slice"
		);
	}
}
