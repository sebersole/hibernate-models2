/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.boot.CacheRegionDefinition;
import org.hibernate.boot.CacheRegionDefinition.CacheRegionType;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import jakarta.persistence.FetchType;

import static org.hibernate.cfg.AvailableSettings.CLASS_CACHE_PREFIX;
import static org.hibernate.cfg.AvailableSettings.COLLECTION_CACHE_PREFIX;

/// Resolves the subset of bootstrap settings used while discovering,
/// categorizing, and binding boot-model sources.
///
/// The resolver is intentionally a normalization boundary.  Callers may arrive
/// with a plain configuration map, a Hibernate-specific
/// [HibernatePersistenceConfiguration], or a parsed [PersistenceUnitDescriptor]
/// plus integration settings.  Each form is collapsed into the same
/// [ResolvedBootstrapSettings] contract before source discovery continues.
///
/// This class should own precedence and interpretation rules for the named
/// settings in [ResolvedBootstrapSettings].  Settings that are merely carried to
/// later bootstrap stages should remain in the raw configuration map and should
/// not grow a named accessor here unless they affect early boot-model behavior.
///
/// @author Steve Ebersole
public class BootstrapSettingsResolver {
	/// Resolves settings from a raw configuration map.
	///
	/// This overload represents the native/default path.  It does not mark the
	/// result as JPA bootstrap, uses `FetchType.EAGER` as the default to-one fetch
	/// type, and resolves model-shaping settings from the supplied map.
	///
	/// @param configurationValues Raw configuration values
	///
	/// @return The resolved bootstrap settings
	public ResolvedBootstrapSettings resolve(Map<?, ?> configurationValues) {
		final var resolvedConfigurationValues = copyConfigurationValues( configurationValues );
		return resolve(
				resolvedConfigurationValues,
				false,
				FetchType.EAGER
		);
	}

	/// Resolves settings from Hibernate's programmatic JPA bootstrap
	/// configuration.
	///
	/// Values exposed directly by the configuration object, such as default
	/// to-one fetch type, are used as the named setting sources.  The raw
	/// properties map is still carried forward in
	/// [ResolvedBootstrapSettings#configurationValues()].
	///
	/// @param persistenceConfiguration The programmatic persistence-unit
	/// configuration
	///
	/// @return The resolved bootstrap settings
	public ResolvedBootstrapSettings resolve(HibernatePersistenceConfiguration persistenceConfiguration) {
		final var resolvedConfigurationValues = copyConfigurationValues( persistenceConfiguration.properties() );
		return resolve(
				resolvedConfigurationValues,
				true,
				persistenceConfiguration.defaultToOneFetchType()
		);
	}

	/// Resolves settings from a parsed persistence-unit descriptor and runtime
	/// integration settings.
	///
	/// Persistence-unit properties form the baseline, and integration settings
	/// overlay them.  This matches the JPA bootstrap shape where container or
	/// caller-provided integration values may override persistence-unit
	/// configuration.
	///
	/// @param persistenceUnitDescriptor The parsed persistence-unit descriptor
	/// @param integrationSettings Runtime integration settings to overlay
	///
	/// @return The resolved bootstrap settings
	public ResolvedBootstrapSettings resolve(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			Map<?, ?> integrationSettings) {
		final var resolvedConfigurationValues = copyConfigurationValues( persistenceUnitDescriptor.getProperties() );
		overlay( integrationSettings, resolvedConfigurationValues );
		return resolve(
				resolvedConfigurationValues,
				true,
				persistenceUnitDescriptor.getDefaultToOneFetchType()
		);
	}

	private ResolvedBootstrapSettings resolve(
			Map<String, Object> configurationValues,
			boolean jpaBootstrap,
			FetchType defaultToOneFetchType) {
		return new ResolvedBootstrapSettings(
				configurationValues,
				jpaBootstrap,
				new ResolvedMappingSettings(
						resolveXmlMappingEnabled( configurationValues ),
						resolveValidateXml( configurationValues ),
						defaultToOneFetchType,
						resolveCacheRegionDefinitions( configurationValues )
				)
		);
	}

	private static LinkedHashMap<String, Object> copyConfigurationValues(Map<?, ?> configurationValues) {
		final var result = new LinkedHashMap<String, Object>();
		overlay( configurationValues, result );
		return result;
	}

	private static void overlay(Map<?, ?> source, Map<String, Object> target) {
		if ( source == null ) {
			return;
		}
		source.forEach( (key, value) -> {
			if ( key != null ) {
				target.put( key.toString(), value );
			}
		} );
	}

	private static boolean resolveXmlMappingEnabled(Map<String, Object> configurationValues) {
		final Object enabled = configurationValues.get( MappingSettings.XML_MAPPING_ENABLED );
		return enabled == null || parseBoolean( enabled );
	}

	private static boolean resolveValidateXml(Map<String, Object> configurationValues) {
		final Object enabled = configurationValues.get( MappingSettings.VALIDATE_XML );
		return enabled != null && parseBoolean( enabled );
	}

	private static boolean parseBoolean(Object value) {
		if ( value instanceof Boolean booleanValue ) {
			return booleanValue;
		}
		return Boolean.parseBoolean( value.toString() );
	}

	private static List<CacheRegionDefinition> resolveCacheRegionDefinitions(Map<String, Object> configurationValues) {
		final var cacheRegionDefinitions = new ArrayList<CacheRegionDefinition>();
		configurationValues.forEach( (key, value) -> {
			if ( value instanceof String valueString ) {
				if ( key.startsWith( CLASS_CACHE_PREFIX + "." ) ) {
					cacheRegionDefinitions.add( parseCacheRegionDefinitionEntry(
							key.substring( CLASS_CACHE_PREFIX.length() + 1 ),
							valueString,
							CacheRegionType.ENTITY
					) );
				}
				else if ( key.startsWith( COLLECTION_CACHE_PREFIX + "." ) ) {
					cacheRegionDefinitions.add( parseCacheRegionDefinitionEntry(
							key.substring( COLLECTION_CACHE_PREFIX.length() + 1 ),
							valueString,
							CacheRegionType.COLLECTION
					) );
				}
			}
		} );
		return cacheRegionDefinitions;
	}

	private static CacheRegionDefinition parseCacheRegionDefinitionEntry(
			String role,
			String value,
			CacheRegionType cacheType) {
		final var params = new StringTokenizer( value, ";, " );
		if ( !params.hasMoreTokens() ) {
			throw illegalCacheRegionDefinitionException( role, value, cacheType );
		}

		final String usage = params.nextToken();
		final String region = params.hasMoreTokens() ? params.nextToken() : null;
		final boolean lazyProperty = cacheType == CacheRegionType.ENTITY
				&& ( !params.hasMoreTokens() || "all".equalsIgnoreCase( params.nextToken() ) );
		return new CacheRegionDefinition( cacheType, role, usage, region, lazyProperty );
	}

	private static IllegalArgumentException illegalCacheRegionDefinitionException(
			String role,
			String value,
			CacheRegionType cacheType) {
		return new IllegalArgumentException(
				"Cache region configuration `%s.%s %s` not of form `usage[,region[,lazy]]`".formatted(
						cacheType == CacheRegionType.ENTITY ? CLASS_CACHE_PREFIX : COLLECTION_CACHE_PREFIX,
						role,
						value
				)
		);
	}
}
