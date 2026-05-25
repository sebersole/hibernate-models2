/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.orchestration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.boot.models.source.BootstrapSourceContributions;
import org.hibernate.service.ServiceRegistry;

import jakarta.persistence.FetchType;

/// Request for the current SessionFactory bootstrap slice.
///
/// This request intentionally carries early bootstrap inputs rather than
/// materialized phase outputs.  [SessionFactoryBootstrap] owns resolving settings,
/// creating source resources, creating the metadata-building context, and then
/// composing the later phases in order.
///
/// Later bootstrap slices are expected to add earlier inputs, such as resolved
/// entry-point-specific source contributions, before adding final SessionFactory
/// creation inputs.
///
/// @author Steve Ebersole
public record SessionFactoryBootstrapRequest(
		/// Mapping-source contributions supplied by the entry point.
		BootstrapSourceContributions sourceContributions,

		/// Explicit configuration values that should override environment and
		/// persistence-unit properties.
		Map<String, Object> configurationValues,

		/// Whether this bootstrap originated from a Jakarta Persistence entry point.
		boolean jpaBootstrap,

		/// Default to-one fetch type to use while resolving mapping settings.
		FetchType defaultToOneFetchType,

		/// Service registry that provides class-loading, configuration, and other
		/// early bootstrap services.
		ServiceRegistry serviceRegistry) {

	public SessionFactoryBootstrapRequest {
		sourceContributions = Objects.requireNonNull( sourceContributions );
		configurationValues = configurationValues == null
				? Collections.emptyMap()
				: Collections.unmodifiableMap( new LinkedHashMap<>( configurationValues ) );
		defaultToOneFetchType = defaultToOneFetchType == null ? FetchType.EAGER : defaultToOneFetchType;
		serviceRegistry = Objects.requireNonNull( serviceRegistry );
	}

	public SessionFactoryBootstrapRequest(
			BootstrapSourceContributions sourceContributions,
			ServiceRegistry serviceRegistry) {
		this( sourceContributions, Map.of(), false, FetchType.EAGER, serviceRegistry );
	}

}
