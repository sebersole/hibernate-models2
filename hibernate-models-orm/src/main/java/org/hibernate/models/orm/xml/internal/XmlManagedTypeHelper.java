/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.JaxbAttributes;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddable;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddableAttributes;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddedId;
import org.hibernate.boot.jaxb.mapping.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.JaxbId;
import org.hibernate.boot.jaxb.mapping.JaxbMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.ManagedType;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.spi.Processor;
import org.hibernate.models.orm.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.source.internal.MutableAnnotationUsage;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.internal.SourceModelLogging;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.orm.xml.internal.XmlAnnotationHelper.getOrMakeAnnotation;

/**
 * @author Steve Ebersole
 */
public class XmlManagedTypeHelper {

	private static String determineClassName(JaxbEntityMappings jaxbRoot, ManagedType jaxbManagedType) {
		if ( StringHelper.isQualified( jaxbManagedType.getClazz() ) ) {
			return jaxbManagedType.getClazz();
		}

		return StringHelper.qualify( jaxbManagedType.getClazz(), jaxbRoot.getPackage() );
	}

	public static void makeCompleteMappedSuperclassMapping(
			JaxbEntityMappings jaxbRoot,
			JaxbMappedSuperclass jaxbMappedSuperclass,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		// todo : should we allow mapped-superclass in dynamic models?
		//		that would need a change in XSD

		final String className = determineClassName( jaxbRoot, jaxbMappedSuperclass );
		final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
				.getClassDetailsRegistry()
				.resolveClassDetails( className );

		final AccessType classAccessType = coalesce(
				jaxbMappedSuperclass.getAccess(),
				persistenceUnitMetadata.getAccessType()
		);

		final JaxbAttributes attributes = jaxbMappedSuperclass.getAttributes();
		handleIdMappings( attributes, classAccessType, classDetails, sourceModelBuildingContext );

		XmlAttributeHelper.handleNaturalId( attributes.getNaturalId(), classDetails, classAccessType, sourceModelBuildingContext );
		XmlAttributeHelper.handleAttributes( attributes, classDetails, classAccessType, sourceModelBuildingContext );

		// todo : id-class
		// todo : entity-listeners
		// todo : callbacks
	}

	public static void makeCompleteEntityMapping(
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
			final String className = determineClassName( jaxbRoot, jaxbEntity );
			classDetails = (MutableClassDetails) sourceModelBuildingContext.getClassDetailsRegistry().resolveClassDetails( className );
		}

		classDetails.clearMemberAnnotationUsages();
		classDetails.clearAnnotationUsages();

		XmlAnnotationHelper.applyEntity( jaxbEntity, classDetails, sourceModelBuildingContext );

		applyInheritance( jaxbEntity, classDetails, sourceModelBuildingContext );

		if ( jaxbEntity.getTable() != null ) {
			XmlAnnotationHelper.applyTable( jaxbEntity.getTable(), classDetails, persistenceUnitMetadata );
		}

		final AccessType classAccessType = coalesce(
				jaxbEntity.getAccess(),
				persistenceUnitMetadata.getAccessType()
		);

		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails ) );

		final JaxbAttributes attributes = jaxbEntity.getAttributes();
		handleIdMappings( attributes, classAccessType, classDetails, sourceModelBuildingContext );

		XmlAttributeHelper.handleNaturalId( attributes.getNaturalId(), classDetails, classAccessType, sourceModelBuildingContext );
		XmlAttributeHelper.handleAttributes( jaxbEntity.getAttributes(), classDetails, classAccessType, sourceModelBuildingContext );

		// todo : id-class
		// todo : entity-listeners
		// todo : callbacks
		// todo : secondary-tables
	}

	private static void applyInheritance(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbEntity.getInheritance() == null ) {
			return;
		}

		final MutableAnnotationUsage<Inheritance> inheritanceAnn = getOrMakeAnnotation(
				Inheritance.class,
				classDetails
		);
		inheritanceAnn.setAttributeValue( "strategy", jaxbEntity.getInheritance().getStrategy() );
	}

	private static void handleIdMappings(
			JaxbAttributes attributes,
			AccessType classAccessType,
			MutableClassDetails classDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final List<JaxbId> jaxbIds = attributes.getId();
		final JaxbEmbeddedId jaxbEmbeddedId = attributes.getEmbeddedId();

		if ( CollectionHelper.isNotEmpty( jaxbIds ) ) {
			for ( int i = 0; i < jaxbIds.size(); i++ ) {
				final JaxbId jaxbId = jaxbIds.get( i );
				final AccessType accessType = coalesce( jaxbId.getAccess(), classAccessType );
				final MutableMemberDetails memberDetails = XmlAttributeHelper.findAttributeMember(
						jaxbId.getName(),
						accessType,
						classDetails,
						sourceModelBuildingContext
				);

				XmlAnnotationHelper.applyId( jaxbId, memberDetails, sourceModelBuildingContext );
				XmlAnnotationHelper.applyBasic( jaxbId, memberDetails, sourceModelBuildingContext );
				XmlAttributeHelper.applyCommonAttributeAnnotations(
						jaxbId,
						memberDetails,
						accessType,
						sourceModelBuildingContext
				);

				XmlAnnotationHelper.applyColumn(
						jaxbId.getColumn(),
						memberDetails,
						sourceModelBuildingContext
				);

				XmlAnnotationHelper.applyUserType(
						jaxbId.getType(),
						memberDetails,
						sourceModelBuildingContext
				);
				XmlAnnotationHelper.applyJdbcTypeCode(
						jaxbId.getJdbcTypeCode(),
						memberDetails,
						sourceModelBuildingContext
				);
				XmlAnnotationHelper.applyTemporal(
						jaxbId.getTemporal(),
						memberDetails,
						sourceModelBuildingContext
				);

				XmlAnnotationHelper.applyGeneratedValue(
						jaxbId.getGeneratedValue(),
						memberDetails,
						sourceModelBuildingContext
				);
				XmlAnnotationHelper.applySequenceGenerator(
						jaxbId.getSequenceGenerator(),
						memberDetails,
						sourceModelBuildingContext
				);
				XmlAnnotationHelper.applyTableGenerator(
						jaxbId.getTableGenerator(),
						memberDetails,
						sourceModelBuildingContext
				);
				XmlAnnotationHelper.applyUuidGenerator(
						jaxbId.getUuidGenerator(),
						memberDetails,
						sourceModelBuildingContext
				);

				// todo : unsaved-value?
			}
		}
		else if ( jaxbEmbeddedId != null ) {
			final AccessType accessType = coalesce( jaxbEmbeddedId.getAccess(), classAccessType );
			final MutableMemberDetails memberDetails = XmlAttributeHelper.findAttributeMember(
					jaxbEmbeddedId.getName(),
					accessType,
					classDetails,
					sourceModelBuildingContext
			);

			XmlAnnotationHelper.applyEmbeddedId( jaxbEmbeddedId, memberDetails, sourceModelBuildingContext );
			XmlAttributeHelper.applyCommonAttributeAnnotations(
					jaxbEmbeddedId,
					memberDetails,
					accessType,
					sourceModelBuildingContext
			);

			XmlAnnotationHelper.applyAttributeOverrides(
					jaxbEmbeddedId.getAttributeOverride(),
					memberDetails,
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

	public static void makeCompleteEmbeddableMapping(
			JaxbEntityMappings jaxbRoot,
			JaxbEmbeddable jaxbEmbeddable,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		// todo : add support for dynamic embeddables in XSD
		final String className = determineClassName( jaxbRoot, jaxbEmbeddable );
		final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
				.getClassDetailsRegistry()
				.resolveClassDetails( className );

		classDetails.clearMemberAnnotationUsages();
		classDetails.clearAnnotationUsages();

		final DynamicAnnotationUsage<Embeddable> embeddableAnn = new DynamicAnnotationUsage<>( Embeddable.class, classDetails );
		classDetails.addAnnotationUsage( embeddableAnn );

		final AccessType classAccessType = coalesce(
				jaxbEmbeddable.getAccess(),
				persistenceUnitMetadata.getAccessType()
		);

		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails ) );

		final JaxbEmbeddableAttributes attributes = jaxbEmbeddable.getAttributes();
		XmlAttributeHelper.handleAttributes( attributes, classDetails, classAccessType, sourceModelBuildingContext );

	}

	public static void applyMappedSuperclassOverrides(
			Map<String, ClassDetails> mappedSuperClasses,
			List<Processor.OverrideTuple<JaxbMappedSuperclass>> mappedSuperclassesOverrides,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( CollectionHelper.isNotEmpty( mappedSuperclassesOverrides ) ) {
			throw new UnsupportedOperationException( "<mapped-superclass/> override support not yet implemented" );
		}
	}

	public static void applyEntityOverrides(
			Map<String, ClassDetails> allEntities,
			List<Processor.OverrideTuple<JaxbEntity>> entityOverrides,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		entityOverrides.forEach( (overrideTuple) -> {
			final JaxbEntityMappings jaxbRoot = overrideTuple.getJaxbRoot();
			final JaxbEntity jaxbEntity = overrideTuple.getManagedType();
			final String className = determineClassName( jaxbRoot, jaxbEntity );
			final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( className );

			XmlAnnotationHelper.applyEntity( jaxbEntity, classDetails, sourceModelBuildingContext );

			applyInheritanceOverride( jaxbEntity, classDetails, sourceModelBuildingContext );
			XmlAnnotationHelper.applyTableOverride( jaxbEntity.getTable(), classDetails, persistenceUnitMetadata );

			final AccessType classAccessType = coalesce(
					jaxbEntity.getAccess(),
					persistenceUnitMetadata.getAccessType()
			);
			if ( classAccessType != null ) {
				final MutableAnnotationUsage<Access> accessAnnotation = getOrMakeAnnotation(
						Access.class,
						classDetails
				);
				accessAnnotation.setAttributeValue( "value", classAccessType );
			}

			final JaxbAttributes attributes = jaxbEntity.getAttributes();
			applyIdMappingOverrides( attributes, classAccessType, classDetails, sourceModelBuildingContext );

			XmlAttributeHelper.handleNaturalId( attributes.getNaturalId(), classDetails, classAccessType, sourceModelBuildingContext );
			XmlAttributeHelper.handleAttributes( jaxbEntity.getAttributes(), classDetails, classAccessType, sourceModelBuildingContext );

			// todo : id-class
			// todo : entity-listeners
			// todo : callbacks
			// todo : secondary-tables
		} );

	}

	private static void applyInheritanceOverride(
			JaxbEntity jaxbEntity,
			MutableClassDetails classDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbEntity.getInheritance() == null ) {
			return;
		}

		final MutableAnnotationUsage<Inheritance> annotationUsage = getOrMakeAnnotation( Inheritance.class, classDetails );
		annotationUsage.setAttributeValue( "strategy", jaxbEntity.getInheritance().getStrategy() );
	}

	private static void applyIdMappingOverrides(
			JaxbAttributes attributes,
			AccessType classAccessType,
			MutableClassDetails classDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final List<JaxbId> jaxbIds = attributes.getId();
		final JaxbEmbeddedId jaxbEmbeddedId = attributes.getEmbeddedId();

		if ( CollectionHelper.isNotEmpty( jaxbIds ) ) {
			for ( int i = 0; i < jaxbIds.size(); i++ ) {
				final JaxbId jaxbId = jaxbIds.get( i );
				final AccessType accessType = coalesce( jaxbId.getAccess(), classAccessType );
				final MutableMemberDetails memberDetails = XmlAttributeHelper.findAttributeMember(
						jaxbId.getName(),
						accessType,
						classDetails,
						sourceModelBuildingContext
				);

				final MutableMemberDetails inverseMemberDetails = XmlAttributeHelper.findAttributeMember(
						jaxbId.getName(),
						inverse( accessType ),
						classDetails,
						sourceModelBuildingContext
				);
				if ( inverseMemberDetails != null ) {
					inverseMemberDetails.removeAnnotationUsage( Access.class );
				}

				XmlAnnotationHelper.applyIdOverride( jaxbId, memberDetails, sourceModelBuildingContext );
				XmlAnnotationHelper.applyBasic( jaxbId, memberDetails, sourceModelBuildingContext );
				XmlAttributeHelper.applyCommonAttributeAnnotations(
						jaxbId,
						memberDetails,
						accessType,
						sourceModelBuildingContext
				);

				XmlAnnotationHelper.applyColumn(
						jaxbId.getColumn(),
						memberDetails,
						sourceModelBuildingContext
				);

				XmlAnnotationHelper.applyUserType(
						jaxbId.getType(),
						memberDetails,
						sourceModelBuildingContext
				);
				XmlAnnotationHelper.applyJdbcTypeCode(
						jaxbId.getJdbcTypeCode(),
						memberDetails,
						sourceModelBuildingContext
				);
				XmlAnnotationHelper.applyTemporal(
						jaxbId.getTemporal(),
						memberDetails,
						sourceModelBuildingContext
				);

				XmlAnnotationHelper.applyGeneratedValue(
						jaxbId.getGeneratedValue(),
						memberDetails,
						sourceModelBuildingContext
				);
				XmlAnnotationHelper.applySequenceGenerator(
						jaxbId.getSequenceGenerator(),
						memberDetails,
						sourceModelBuildingContext
				);
				XmlAnnotationHelper.applyTableGenerator(
						jaxbId.getTableGenerator(),
						memberDetails,
						sourceModelBuildingContext
				);
				XmlAnnotationHelper.applyUuidGenerator(
						jaxbId.getUuidGenerator(),
						memberDetails,
						sourceModelBuildingContext
				);

				// todo : unsaved-value?
			}
		}
		else if ( jaxbEmbeddedId != null ) {
			final AccessType accessType = coalesce( jaxbEmbeddedId.getAccess(), classAccessType );
			final MutableMemberDetails memberDetails = XmlAttributeHelper.findAttributeMember(
					jaxbEmbeddedId.getName(),
					accessType,
					classDetails,
					sourceModelBuildingContext
			);

			XmlAnnotationHelper.applyEmbeddedId( jaxbEmbeddedId, memberDetails, sourceModelBuildingContext );
			XmlAttributeHelper.applyCommonAttributeAnnotations(
					jaxbEmbeddedId,
					memberDetails,
					accessType,
					sourceModelBuildingContext
			);

			XmlAnnotationHelper.applyAttributeOverrides(
					jaxbEmbeddedId.getAttributeOverride(),
					memberDetails,
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

	private static AccessType inverse(AccessType accessType) {
		return accessType == AccessType.FIELD ? AccessType.PROPERTY : AccessType.FIELD;
	}

	public static void applyEmbeddableOverrides(
			Map<String, ClassDetails> embeddables,
			List<Processor.OverrideTuple<JaxbEmbeddable>> embeddableOverrides,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( CollectionHelper.isNotEmpty( embeddableOverrides ) ) {
			throw new UnsupportedOperationException( "<embeddable/> override support not yet implemented" );
		}
	}
}
