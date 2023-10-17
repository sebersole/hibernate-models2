/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.models.orm.xml.internal.PersistenceUnitMetadataImpl;

/**
 * @author Steve Ebersole
 */
public class XmlResources {
	private final PersistenceUnitMetadataImpl persistenceUnitMetadata = new PersistenceUnitMetadataImpl();
	private final List<JaxbEntityMappings> documents = new ArrayList<>();

	public XmlResources() {
	}

	public PersistenceUnitMetadata getPersistenceUnitMetadata() {
		return persistenceUnitMetadata;
	}

	public List<JaxbEntityMappings> getDocuments() {
		return documents;
	}

	public void addDocument(JaxbEntityMappings document) {
		persistenceUnitMetadata.apply( document.getPersistenceUnitMetadata() );
		documents.add( document );
	}
}
