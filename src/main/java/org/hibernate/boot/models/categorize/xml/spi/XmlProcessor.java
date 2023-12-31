/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.spi;

import org.hibernate.boot.models.categorize.xml.internal.XmlDocumentContextImpl;
import org.hibernate.boot.models.categorize.xml.internal.XmlDocumentImpl;
import org.hibernate.boot.models.categorize.internal.DomainModelCategorizationCollector;
import org.hibernate.boot.models.categorize.xml.internal.ManagedTypeProcessor;
import org.hibernate.boot.models.categorize.xml.internal.XmlProcessingResultImpl;
import org.hibernate.models.spi.SourceModelBuildingContext;

/**
 * Processes XML mappings - applying metadata-complete mappings and collecting
 * override mappings for later processing.
 *
 * @author Steve Ebersole
 */
public class XmlProcessor {
	public static XmlProcessingResult processXml(
			XmlPreProcessingResult xmlPreProcessingResult,
			DomainModelCategorizationCollector modelCategorizationCollector,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final boolean xmlMappingsGloballyComplete = xmlPreProcessingResult.getPersistenceUnitMetadata().areXmlMappingsComplete();
		final XmlProcessingResultImpl xmlOverlay = new XmlProcessingResultImpl();

		xmlPreProcessingResult.getDocuments().forEach( (jaxbRoot) -> {
			modelCategorizationCollector.apply( jaxbRoot );
			final XmlDocumentImpl xmlDocument = XmlDocumentImpl.consume(
					jaxbRoot,
					xmlPreProcessingResult.getPersistenceUnitMetadata()
			);
			final XmlDocumentContext xmlDocumentContext = new XmlDocumentContextImpl(
					xmlDocument,
					xmlPreProcessingResult.getPersistenceUnitMetadata(),
					sourceModelBuildingContext
			);

			jaxbRoot.getEmbeddables().forEach( (jaxbEmbeddable) -> {
				if ( xmlMappingsGloballyComplete || jaxbEmbeddable.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteEmbeddable(
							jaxbRoot,
							jaxbEmbeddable,
							xmlDocumentContext
					);
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addEmbeddableOverride( new XmlProcessingResult.OverrideTuple<>( jaxbRoot, xmlDocumentContext, jaxbEmbeddable ) );
				}
			} );

			jaxbRoot.getMappedSuperclasses().forEach( (jaxbMappedSuperclass) -> {
				if ( xmlMappingsGloballyComplete || jaxbMappedSuperclass.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteMappedSuperclass( jaxbRoot, jaxbMappedSuperclass, xmlDocumentContext );
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addMappedSuperclassesOverride( new XmlProcessingResult.OverrideTuple<>( jaxbRoot, xmlDocumentContext, jaxbMappedSuperclass ) );
				}
			});

			jaxbRoot.getEntities().forEach( (jaxbEntity) -> {
				if ( xmlMappingsGloballyComplete || jaxbEntity.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteEntity( jaxbRoot, jaxbEntity, xmlDocumentContext );
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addEntityOverride( new XmlProcessingResult.OverrideTuple<>( jaxbRoot, xmlDocumentContext, jaxbEntity ) );
				}
			} );
		} );

		return xmlOverlay;
	}
}
