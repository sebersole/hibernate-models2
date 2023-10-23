/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.spi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.models.orm.spi.ManagedResources;
import org.hibernate.models.orm.xml.XmlResourceException;
import org.hibernate.models.orm.xml.internal.PersistenceUnitMetadataImpl;
import org.hibernate.models.orm.xml.internal.ResourceStreamLocatorImpl;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.ClassLoading;

import static org.hibernate.boot.jaxb.internal.MappingBinder.NON_VALIDATING;

/**
 * Keeps track of collected/aggregated {@linkplain #getPersistenceUnitMetadata() metadata}
 * across all {@linkplain #getDocuments() mapping documents} in the persistence-unit
 *
 * @author Steve Ebersole
 */
public class XmlResources {
	private final PersistenceUnitMetadataImpl persistenceUnitMetadata = new PersistenceUnitMetadataImpl();
	private final List<JaxbEntityMappingsImpl> documents = new ArrayList<>();

	public XmlResources() {
	}

	/**
	 * Build an XmlResources reference based on the given {@code managedResources}
	 */
	public static XmlResources collectXmlResources(
			ManagedResources managedResources,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final ClassLoading classLoading = sourceModelBuildingContext.getClassLoading();
		final ResourceStreamLocatorImpl resourceStreamLocator = new ResourceStreamLocatorImpl( classLoading );
		final MappingBinder mappingBinder = new MappingBinder( resourceStreamLocator, NON_VALIDATING );
		final XmlResources collected = new XmlResources();

		final List<String> xmlMappings = managedResources.getXmlMappings();
		for ( int i = 0; i < xmlMappings.size(); i++ ) {
			final String xmlMapping = xmlMappings.get( i );
			final URL resource = classLoading.locateResource( xmlMapping );
			if ( resource == null ) {
				throw new XmlResourceException( "Unable to locate XML mapping - " + xmlMapping );
			}
			try (InputStream inputStream = resource.openStream()) {
				final Binding<JaxbBindableMappingDescriptor> binding = mappingBinder.bind(
						inputStream,
						new Origin( SourceType.RESOURCE, xmlMapping )
				);
				collected.addDocument( (JaxbEntityMappingsImpl) binding.getRoot() );
			}
			catch (IOException e) {
				throw new XmlResourceException( "Unable to bind XML mapping - " + xmlMapping, e );
			}
		}

		return collected;
	}

	/**
	 * The metadata collected/aggregated across all of the {@linkplain #getDocuments() mapping documents}
	 * in the persistence-unit.
	 */
	public PersistenceUnitMetadata getPersistenceUnitMetadata() {
		return persistenceUnitMetadata;
	}

	/**
	 * All documents in the persistence-unit
	 */
	public List<JaxbEntityMappingsImpl> getDocuments() {
		return documents;
	}

	public void addDocument(JaxbEntityMappingsImpl document) {
		persistenceUnitMetadata.apply( document.getPersistenceUnitMetadata() );
		documents.add( document );
	}
}
