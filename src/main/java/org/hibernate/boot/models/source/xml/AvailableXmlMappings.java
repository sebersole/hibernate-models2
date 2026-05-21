/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.source.xml;

import java.util.List;

/**
 * Available XML mapping resources after XML pre-processing.
 * <p>
 * This is a narrow local wrapper around the upstream XML pre-processing result,
 * exposing only the source details needed by categorization.
 *
 * @author Steve Ebersole
 */
public record AvailableXmlMappings(
		org.hibernate.boot.models.xml.spi.XmlPreProcessingResult upstreamResult,
		org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata persistenceUnitMetadata) {
	public List<String> getMappedClasses() {
		return upstreamResult.getMappedClasses();
	}

	public List<String> getMappedNames() {
		return upstreamResult.getMappedNames();
	}
}
