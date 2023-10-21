/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import java.util.List;

import org.hibernate.boot.jaxb.mapping.JaxbAttributes;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddable;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddableAttributes;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddedId;
import org.hibernate.boot.jaxb.mapping.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.JaxbId;
import org.hibernate.boot.jaxb.mapping.JaxbMappedSuperclass;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.spi.Processor;
import org.hibernate.models.orm.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.SourceModelLogging;
import org.hibernate.models.source.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.orm.xml.internal.AttributeProcessor.processAttributes;
import static org.hibernate.models.orm.xml.internal.AttributeProcessor.processNaturalId;

/**
 * Helper for handling managed types defined in mapping XML, in either
 * metadata-complete or override mode
 *
 * @author Steve Ebersole
 */
public class ManagedTypeProcessor {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity

	public static void processCompleteEntity(
			JaxbEntityMappings jaxbRoot,
			JaxbEntity jaxbEntity,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final MutableClassDetails classDetails;

		if ( StringHelper.isEmpty( jaxbEntity.getClazz() ) ) {
			// should indicate a dynamic entity
			assert StringHelper.isNotEmpty( jaxbEntity.getName() );
			classDetails = (MutableClassDetails) sourceModelBuildingContext.getClassDetailsRegistry().resolveClassDetails(
					jaxbEntity.getName(),
					() -> new DynamicClassDetails(
							jaxbEntity.getName(),
							null,
							false,
							null,
							sourceModelBuildingContext
					)
			);
		}
		else {
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEntity );
			classDetails = (MutableClassDetails) sourceModelBuildingContext.getClassDetailsRegistry().resolveClassDetails( className );
		}

		classDetails.clearMemberAnnotationUsages();
		classDetails.clearAnnotationUsages();

		// from here, processing is the same between override and metadata-complete modes
		processEntityMetadata( jaxbEntity, classDetails, persistenceUnitMetadata, sourceModelBuildingContext );
	}

	public static void processOverrideEntity(
			List<Processor.OverrideTuple<JaxbEntity>> entityOverrides,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		entityOverrides.forEach( (overrideTuple) -> {
			final JaxbEntityMappings jaxbRoot = overrideTuple.getJaxbRoot();
			final JaxbEntity jaxbEntity = overrideTuple.getManagedType();
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEntity );
			final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( className );

			// from here, processing is the same between override and metadata-complete modes
			processEntityMetadata( jaxbEntity, classDetails, persistenceUnitMetadata, sourceModelBuildingContext );
		} );

	}

	private static void processEntityMetadata(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlAnnotationHelper.applyEntity( jaxbEntity, classDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyInheritance( jaxbEntity, classDetails, sourceModelBuildingContext );

		if ( jaxbEntity.getTable() != null ) {
			XmlAnnotationHelper.applyTable( jaxbEntity.getTable(), classDetails, persistenceUnitMetadata );
		}

		final AccessType classAccessType = coalesce(
				jaxbEntity.getAccess(),
				persistenceUnitMetadata.getAccessType()
		);
		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails ) );

		final JaxbAttributes attributes = jaxbEntity.getAttributes();
		processIdMappings( attributes, classAccessType, classDetails, sourceModelBuildingContext );
		processNaturalId( attributes.getNaturalId(), classDetails, classAccessType, sourceModelBuildingContext );
		processAttributes( attributes, classDetails, classAccessType, sourceModelBuildingContext );

		// todo : id-class
		// todo : entity-listeners
		// todo : callbacks
		// todo : secondary-tables
	}

	private static void processIdMappings(
			JaxbAttributes attributes,
			AccessType classAccessType,
			MutableClassDetails classDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final List<JaxbId> jaxbIds = attributes.getId();
		final JaxbEmbeddedId jaxbEmbeddedId = attributes.getEmbeddedId();

		if ( CollectionHelper.isNotEmpty( jaxbIds ) ) {
			for ( int i = 0; i < jaxbIds.size(); i++ ) {
				final JaxbId jaxbId = jaxbIds.get( i );
				AttributeProcessor.processBasicIdAttribute(
						jaxbId,
						classDetails,
						classAccessType,
						sourceModelBuildingContext
				);
			}
		}
		else if ( jaxbEmbeddedId != null ) {
			AttributeProcessor.processEmbeddedIdAttribute(
					jaxbEmbeddedId,
					classDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}
		else {
			SourceModelLogging.SOURCE_MODEL_LOGGER.debugf(
					"Identifiable type [%s] contained no <id/> nor <embedded-id/>",
					classDetails.getName()
			);
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// MappedSuperclass

	public static void processCompleteMappedSuperclass(
			JaxbEntityMappings jaxbRoot,
			JaxbMappedSuperclass jaxbMappedSuperclass,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		// todo : should we allow mapped-superclass in dynamic models?
		//		that would need a change in XSD

		final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbMappedSuperclass );
		final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
				.getClassDetailsRegistry()
				.resolveClassDetails( className );

		classDetails.clearMemberAnnotationUsages();
		classDetails.clearAnnotationUsages();

		processMappedSuperclassMetadata( jaxbMappedSuperclass, classDetails, persistenceUnitMetadata, sourceModelBuildingContext );
	}

	private static void processMappedSuperclassMetadata(
			JaxbMappedSuperclass jaxbMappedSuperclass,
			MutableClassDetails classDetails,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlProcessingHelper.getOrMakeAnnotation( MappedSuperclass.class, classDetails );

		final AccessType classAccessType = coalesce(
				jaxbMappedSuperclass.getAccess(),
				persistenceUnitMetadata.getAccessType()
		);
		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails ) );

		final JaxbAttributes attributes = jaxbMappedSuperclass.getAttributes();
		processAttributes( attributes, classDetails, classAccessType, sourceModelBuildingContext );

		// todo : id-class
		// todo : entity-listeners
		// todo : callbacks
	}

	public static void processOverrideMappedSuperclass(
			List<Processor.OverrideTuple<JaxbMappedSuperclass>> mappedSuperclassesOverrides,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		mappedSuperclassesOverrides.forEach( (overrideTuple) -> {
			final JaxbEntityMappings jaxbRoot = overrideTuple.getJaxbRoot();
			final JaxbMappedSuperclass jaxbMappedSuperclass = overrideTuple.getManagedType();
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbMappedSuperclass );
			final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( className );

			processMappedSuperclassMetadata( jaxbMappedSuperclass, classDetails, persistenceUnitMetadata, sourceModelBuildingContext );
		} );
	}

	public static void processCompleteEmbeddable(
			JaxbEntityMappings jaxbRoot,
			JaxbEmbeddable jaxbEmbeddable,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		// todo : add support for dynamic embeddables in XSD
		final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEmbeddable );
		final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
				.getClassDetailsRegistry()
				.resolveClassDetails( className );

		classDetails.clearMemberAnnotationUsages();
		classDetails.clearAnnotationUsages();

		processEmbeddableMetadata( jaxbEmbeddable, classDetails, persistenceUnitMetadata, sourceModelBuildingContext );
	}

	/**
	 * Process common between complete and override metadata
	 */
	private static void processEmbeddableMetadata(
			JaxbEmbeddable jaxbEmbeddable,
			MutableClassDetails classDetails,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlProcessingHelper.getOrMakeAnnotation( Embeddable.class, classDetails );

		final AccessType classAccessType = coalesce(
				jaxbEmbeddable.getAccess(),
				persistenceUnitMetadata.getAccessType()
		);
		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails ) );

		final JaxbEmbeddableAttributes attributes = jaxbEmbeddable.getAttributes();
		processAttributes( attributes, classDetails, classAccessType, sourceModelBuildingContext );
	}

	public static void processOverrideEmbeddable(
			List<Processor.OverrideTuple<JaxbEmbeddable>> embeddableOverrides,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		embeddableOverrides.forEach( (overrideTuple) -> {
			final JaxbEntityMappings jaxbRoot = overrideTuple.getJaxbRoot();
			final JaxbEmbeddable jaxbEmbeddable = overrideTuple.getManagedType();
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEmbeddable );
			final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( className );

			processEmbeddableMetadata( jaxbEmbeddable, classDetails, persistenceUnitMetadata, sourceModelBuildingContext );
		} );
	}
}
