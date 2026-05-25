/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.settings;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.CacheRegionDefinition;

import jakarta.persistence.FetchType;

/// Resolved settings used while processing mapping sources.
///
/// These settings are separated from the bootstrap envelope because they apply
/// specifically to XML mapping discovery/reading and binding decisions.
///
/// @author Steve Ebersole
public record ResolvedMappingSettings(
		/// Whether XML mapping documents should be processed.
		///
		/// When disabled, explicitly named mapping files and conventionally
		/// discovered `META-INF/orm.xml` resources should be ignored by source
		/// discovery.
		///
		/// @see org.hibernate.cfg.MappingSettings#XML_MAPPING_ENABLED
		boolean xmlMappingEnabled,

		/// Whether XML mapping documents should be validated while being read.
		///
		/// This setting only matters when XML mappings are enabled and present.
		///
		/// @see org.hibernate.cfg.MappingSettings#VALIDATE_XML
		boolean validateXml,

		/// Default fetch type to apply to to-one associations that request the
		/// Jakarta Persistence `DEFAULT` fetch type.
		FetchType defaultToOneFetchType,

		/// Cache region declarations extracted from configuration settings such
		/// as `hibernate.classcache.*` and `hibernate.collectioncache.*`.
		///
		/// The list is defensively copied by the canonical constructor.
		List<CacheRegionDefinition> cacheRegionDefinitions) {

	/// Normalizes nullable collection inputs and exposes immutable snapshots.
	public ResolvedMappingSettings {
		defaultToOneFetchType = defaultToOneFetchType == null ? FetchType.EAGER : defaultToOneFetchType;
		cacheRegionDefinitions = cacheRegionDefinitions == null
				? Collections.emptyList()
				: List.copyOf( cacheRegionDefinitions );
	}
}
