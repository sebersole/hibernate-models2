/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.xml.internal.XmlPreProcessingResultImpl;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.models.xml.spi.XmlPreProcessingResult;

/**
 * Pre-processes XML bindings before categorization.
 *
 * @author Steve Ebersole
 */
public class XmlMappingPreProcessor {
	@SuppressWarnings("unchecked")
	public static XmlPreProcessingResult preProcess(
			AvailableResources availableResources,
			PersistenceUnitMetadata persistenceUnitMetadata) {
		final var collected = new XmlPreProcessingResultImpl( persistenceUnitMetadata );
		for ( var mappingXmlBinding : availableResources.xmlMappings() ) {
			if ( mappingXmlBinding.getRoot() instanceof JaxbEntityMappingsImpl ) {
				collected.addDocument( (Binding<JaxbEntityMappingsImpl>) mappingXmlBinding );
			}
		}
		return collected;
	}

	private XmlMappingPreProcessor() {
	}
}
