/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.source.xml;

import java.util.function.BiConsumer;

import org.hibernate.boot.internal.RootMappingDefaults;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.models.spi.ModelsContext;

/**
 * Adapts available XML mappings to the upstream XML processor.
 * <p>
 * Metadata-complete mappings are applied immediately.  Overlay/override mappings
 * are returned as an {@link XmlProcessingResult} so they can be applied after
 * annotation processing.
 *
 * @author Steve Ebersole
 */
public class AvailableXmlMappingsProcessor {
	public static XmlProcessingResult process(
			AvailableXmlMappings xmlMappings,
			BootstrapContext bootstrapContext,
			RootMappingDefaults mappingDefaults,
			BiConsumer<JaxbEntityMappingsImpl, org.hibernate.boot.models.xml.spi.XmlDocumentContext> jaxbRootConsumer) {
		final ModelsContext modelsContext = bootstrapContext.getModelsContext();
		final org.hibernate.boot.models.xml.spi.XmlProcessingResult upstreamResult =
				org.hibernate.boot.models.xml.spi.XmlProcessor.processXml(
						xmlMappings.upstreamResult(),
						xmlMappings.persistenceUnitMetadata(),
						jaxbRootConsumer,
						modelsContext,
						bootstrapContext,
						mappingDefaults
				);
		return new UpstreamXmlProcessingResultAdapter( upstreamResult );
	}

	private record UpstreamXmlProcessingResultAdapter(
			org.hibernate.boot.models.xml.spi.XmlProcessingResult delegate) implements XmlProcessingResult {
		@Override
		public void apply() {
			delegate.apply();
		}
	}
}
