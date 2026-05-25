/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.settings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resolved settings used while building the runtime SessionFactory.
 *
 * This is intentionally minimal for now.  It establishes the SessionFactory
 * settings target without prematurely copying ORM's large
 * {@code SessionFactoryOptions} surface.
 *
 * @author Steve Ebersole
 */
public record ResolvedSessionFactorySettings(
		/// The normalized raw configuration values available to runtime factory
		/// option resolution.
		Map<String, Object> configurationValues,

		/// Whether this factory build originated from a Jakarta Persistence entry
		/// point.
		boolean jpaBootstrap) {

	/// Exposes immutable snapshots.
	public ResolvedSessionFactorySettings {
		configurationValues = Collections.unmodifiableMap( new LinkedHashMap<>(
				Objects.requireNonNull( configurationValues )
		) );
	}
}
