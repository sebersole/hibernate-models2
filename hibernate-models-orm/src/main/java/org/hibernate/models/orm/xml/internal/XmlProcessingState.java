/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.ManagedType;
import org.hibernate.models.orm.xml.spi.XmlProcessingContext;

/**
 * @author Steve Ebersole
 */
public class XmlProcessingState {
	private final XmlProcessingContext processingContext;
	private final PersistenceUnitMetadataImpl persistenceUnitMetadata = new PersistenceUnitMetadataImpl();

	private final List<ManagedType> xmlOverrideMappings = new ArrayList<>();
	private final List<ManagedType> completeMappings = new ArrayList<>();

	public XmlProcessingState(XmlProcessingContext processingContext) {
		this.processingContext = processingContext;
	}

}
