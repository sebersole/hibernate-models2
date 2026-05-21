/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.source.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.spi.BootstrapContext;

/**
 * Adapts available resources to the upstream XML pre-processor.
 * <p>
 * The upstream pre-processor still accepts ORM {@code ManagedResources}; this
 * class keeps that adapter local to the XML facade.
 *
 * @author Steve Ebersole
 */
public class AvailableXmlMappingsPreProcessor {
	public static AvailableXmlMappings preProcess(
			AvailableResources availableResources,
			org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata persistenceUnitMetadata,
			BootstrapContext bootstrapContext) {
		final org.hibernate.boot.models.xml.spi.XmlPreProcessingResult upstreamResult =
				org.hibernate.boot.models.xml.spi.XmlPreProcessor.preProcessXmlResources(
						new AvailableResourcesManagedResources( availableResources ),
						persistenceUnitMetadata
				);
		return new AvailableXmlMappings( upstreamResult, persistenceUnitMetadata );
	}

	private record AvailableResourcesManagedResources(AvailableResources availableResources) implements ManagedResources {
		@Override
		public Collection<ConverterDescriptor<?, ?>> getAttributeConverterDescriptors() {
			return Collections.emptyList();
		}

		@Override
		public Collection<Class<?>> getAnnotatedClassReferences() {
			return Collections.emptyList();
		}

		@Override
		public Collection<String> getAnnotatedClassNames() {
			return Collections.emptyList();
		}

		@Override
		public Collection<String> getAnnotatedPackageNames() {
			return Collections.emptyList();
		}

		@Override
		public Collection<Binding<? extends JaxbBindableMappingDescriptor>> getXmlMappingBindings() {
			return availableResources.xmlMappings();
		}

		@Override
		public Map<String, Class<?>> getExtraQueryImports() {
			return Collections.emptyMap();
		}
	}
}
