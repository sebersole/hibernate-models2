/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.settings;

import java.util.Objects;

/**
 * Projects resolved bootstrap settings into settings needed for SessionFactory
 * construction.
 *
 * The resolver keeps SessionFactory-specific decisions separate from metadata
 * resolution.  As the prototype grows, this class should own the translation
 * from the single resolved bootstrap settings root to a narrower runtime factory
 * settings contract.
 *
 * @author Steve Ebersole
 */
public class SessionFactorySettingsResolver {
	/**
	 * Resolve SessionFactory settings from the shared bootstrap settings root.
	 *
	 * @param bootstrapSettings The resolved bootstrap settings
	 *
	 * @return The resolved SessionFactory settings
	 */
	public ResolvedSessionFactorySettings resolve(ResolvedBootstrapSettings bootstrapSettings) {
		Objects.requireNonNull( bootstrapSettings );
		return new ResolvedSessionFactorySettings(
				bootstrapSettings.configurationValues(),
				bootstrapSettings.jpaBootstrap()
		);
	}
}
