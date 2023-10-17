/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CascadeType;
import org.hibernate.boot.jaxb.mapping.JaxbEntityListener;
import org.hibernate.boot.jaxb.mapping.JaxbPersistenceUnitDefaults;
import org.hibernate.boot.jaxb.mapping.JaxbPersistenceUnitMetadata;
import org.hibernate.models.orm.internal.OrmModelLogging;
import org.hibernate.models.orm.xml.spi.PersistenceUnitMetadata;

import jakarta.persistence.AccessType;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.splitTrimmingTokens;

/**
 * @author Steve Ebersole
 */
public final class PersistenceUnitMetadataImpl implements PersistenceUnitMetadata {
	private boolean quoteIdentifiers;
	private boolean xmlComplete;

	private String defaultSchema;
	private String defaultCatalog;
	private AccessType accessType;
	private String defaultAccessStrategy;

	private final EnumSet<CascadeType> defaultCascadeTypes = EnumSet.noneOf( CascadeType.class );
	private final Set<JaxbEntityListener> globalEntityListeners = new HashSet<>();

	@Override
	public boolean areXmlMappingsComplete() {
		return xmlComplete;
	}

	@Override
	public String getDefaultSchema() {
		return defaultSchema;
	}

	@Override
	public String getDefaultCatalog() {
		return defaultCatalog;
	}

	@Override
	public AccessType getAccessType() {
		return accessType;
	}

	@Override
	public String getDefaultAccessStrategyName() {
		return defaultAccessStrategy;
	}

	@Override
	public EnumSet<CascadeType> getDefaultCascadeTypes() {
		return defaultCascadeTypes;
	}

	@Override
	public boolean useQuotedIdentifiers() {
		return quoteIdentifiers;
	}

	@Override
	public Set<JaxbEntityListener> getEntityListeners() {
		return globalEntityListeners;
	}

	public void apply(JaxbPersistenceUnitMetadata metadata) {
		if ( metadata == null ) {
			return;
		}

		xmlComplete = xmlComplete || metadata.getXmlMappingMetadataComplete() != null;

		final JaxbPersistenceUnitDefaults defaults = metadata.getPersistenceUnitDefaults();
		if ( defaults == null ) {
			return;
		}

		quoteIdentifiers = quoteIdentifiers || defaults.getDelimitedIdentifiers() != null;

		if ( isNotEmpty( defaults.getCatalog() ) ) {
			if ( defaultCatalog != null ) {
				OrmModelLogging.ORM_MODEL_LOGGER.debugf(
						"Setting already set default catalog : %s, %s",
						defaultCatalog,
						defaults.getCatalog()
				);
			}
			defaultCatalog = defaults.getCatalog();
		}

		if ( isNotEmpty( defaults.getSchema() ) ) {
			if ( defaultSchema != null ) {
				OrmModelLogging.ORM_MODEL_LOGGER.debugf(
						"Setting already set default schema : %s, %s",
						defaultSchema,
						defaults.getSchema()
				);
			}
			defaultSchema = defaults.getSchema();
		}

		if ( defaults.getAccess() != null ) {
			if ( accessType != null ) {
				OrmModelLogging.ORM_MODEL_LOGGER.debugf(
						"Overriding already set default AccessType : %s, %s",
						defaults.getAccess(),
						accessType
				);
			}
			accessType = defaults.getAccess();
		}

		if ( isNotEmpty( defaults.getDefaultAccess() ) ) {
			if ( defaultAccessStrategy != null ) {
				OrmModelLogging.ORM_MODEL_LOGGER.debugf(
						"Overriding already set default access strategy : %s, %s",
						accessType,
						defaultAccessStrategy
				);
			}
			defaultAccessStrategy = defaults.getDefaultAccess();
		}

		if ( defaults.getCascadePersist() != null
				|| isNotEmpty( defaults.getDefaultCascade() ) ) {
			if ( !defaultCascadeTypes.isEmpty() ) {
				OrmModelLogging.ORM_MODEL_LOGGER.debugf( "Adding cascades to already defined set of default cascades" );
			}

			if ( defaults.getCascadePersist() != null ) {
				defaultCascadeTypes.add( CascadeType.PERSIST );
			}

			if ( isNotEmpty( defaults.getDefaultCascade() ) ) {
				final String[] actions = splitTrimmingTokens( ",", defaults.getDefaultCascade(), false );
				assert actions.length > 0;

				for ( int i = 0; i < actions.length; i++ ) {
					defaultCascadeTypes.add( CascadeType.valueOf( actions[i] ) );
				}
			}
		}
	}
}
