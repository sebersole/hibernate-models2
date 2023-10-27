/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.spi;

import org.hibernate.models.orm.internal.DomainModelCategorizationCollector;
import org.hibernate.models.orm.xml.internal.ManagedTypeProcessor;
import org.hibernate.models.orm.xml.internal.XmlProcessingResultImpl;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

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

			jaxbRoot.getEmbeddables().forEach( (jaxbEmbeddable) -> {
				if ( xmlMappingsGloballyComplete || jaxbEmbeddable.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteEmbeddable( jaxbRoot, jaxbEmbeddable, xmlPreProcessingResult.getPersistenceUnitMetadata(), sourceModelBuildingContext );
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addEmbeddableOverride( new XmlProcessingResult.OverrideTuple<>( jaxbRoot, jaxbEmbeddable ) );
				}
			} );

			jaxbRoot.getMappedSuperclasses().forEach( (jaxbMappedSuperclass) -> {
				if ( xmlMappingsGloballyComplete || jaxbMappedSuperclass.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteMappedSuperclass( jaxbRoot, jaxbMappedSuperclass, xmlPreProcessingResult.getPersistenceUnitMetadata(), sourceModelBuildingContext );
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addMappedSuperclassesOverride( new XmlProcessingResult.OverrideTuple<>( jaxbRoot, jaxbMappedSuperclass ) );
				}
			});

			jaxbRoot.getEntities().forEach( (jaxbEntity) -> {
				if ( xmlMappingsGloballyComplete || jaxbEntity.isMetadataComplete() == Boolean.TRUE ) {
					// the XML mapping is complete, we can process it immediately
					ManagedTypeProcessor.processCompleteEntity( jaxbRoot, jaxbEntity, xmlPreProcessingResult.getPersistenceUnitMetadata(), sourceModelBuildingContext );
				}
				else {
					// otherwise, wait to process it until later
					xmlOverlay.addEntityOverride( new XmlProcessingResult.OverrideTuple<>( jaxbRoot, jaxbEntity ) );
				}
			} );
		} );

		return xmlOverlay;
	}
}
