/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import org.hibernate.boot.jaxb.mapping.JaxbAttributes;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddedId;
import org.hibernate.boot.jaxb.mapping.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.JaxbId;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.source.internal.MutableAnnotationTarget;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.internal.StringHelper.nullIfEmpty;

/**
 * @author Steve Ebersole
 */
public class XmlManagedTypeHelper {

	private static String determineClassName(JaxbEntityMappings jaxbRoot, JaxbEntity jaxbEntity) {
		if ( StringHelper.isQualified( jaxbEntity.getClazz() ) ) {
			return jaxbEntity.getClazz();
		}

		return StringHelper.qualify( jaxbEntity.getClazz(), jaxbRoot.getPackage() );
	}

	public static void makeCompleteEntityMapping(
			JaxbEntityMappings jaxbRoot,
			JaxbEntity jaxbEntity,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final ClassDetails classDetails;

		if ( StringHelper.isEmpty( jaxbEntity.getClazz() ) ) {
			// should indicate a dynamic entity
			assert StringHelper.isNotEmpty( jaxbEntity.getName() );
			classDetails = sourceModelBuildingContext.getClassDetailsRegistry().resolveClassDetails(
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
			classDetails = sourceModelBuildingContext.getClassDetailsRegistry().resolveClassDetails( className );
		}

		final MutableClassDetails mutableClassDetails = (MutableClassDetails) classDetails;
		mutableClassDetails.clearMemberAnnotationUsages();

		final MutableAnnotationTarget annotationTarget = (MutableAnnotationTarget) classDetails;
		annotationTarget.clearAnnotationUsages();

		final DynamicAnnotationUsage<Entity> entityAnn = new DynamicAnnotationUsage<>( Entity.class, classDetails );
		entityAnn.setAttributeValue( "name", jaxbEntity.getName() );
		annotationTarget.addAnnotationUsage( entityAnn );

		if ( jaxbEntity.getTable() != null ) {
			final DynamicAnnotationUsage<Table> tableAnn = new DynamicAnnotationUsage<>( Table.class, classDetails );
			tableAnn.setAttributeValue( "name", nullIfEmpty( jaxbEntity.getTable().getName() ) );
			tableAnn.setAttributeValue( "catalog", coalesce(
					nullIfEmpty( jaxbEntity.getTable().getCatalog() ),
					persistenceUnitMetadata.getDefaultCatalog()
			) );
			tableAnn.setAttributeValue( "schema",  coalesce(
					nullIfEmpty( jaxbEntity.getTable().getSchema() ),
					persistenceUnitMetadata.getDefaultSchema()
			) );
			// todo : uniqueConstraints
			// todo : indexes
			annotationTarget.addAnnotationUsage( tableAnn );
		}

		final AccessType classAccessType = coalesce(
				jaxbEntity.getAccess(),
				persistenceUnitMetadata.getAccessType()
		);

		annotationTarget.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, annotationTarget ) );

		final JaxbAttributes attributes = jaxbEntity.getAttributes();
		if ( CollectionHelper.isNotEmpty( attributes.getId() ) ) {
			for ( int i = 0; i < attributes.getId().size(); i++ ) {
				final JaxbId jaxbId = attributes.getId().get( i );
				final AccessType accessType = coalesce( jaxbId.getAccess(), classAccessType );
				final MutableMemberDetails memberDetails = XmlAttributeHelper.findAttributeMember(
						jaxbId.getName(),
						accessType,
						mutableClassDetails,
						sourceModelBuildingContext
				);

				XmlAnnotationHelper.applyAccess( accessType, memberDetails, sourceModelBuildingContext );
				XmlAnnotationHelper.applyAttributeAccessor( jaxbId.getAttributeAccessor(), memberDetails, sourceModelBuildingContext );

				XmlAnnotationHelper.applyColumn( jaxbId.getColumn(), memberDetails, sourceModelBuildingContext );

				XmlAnnotationHelper.applyUserType( jaxbId.getType(), memberDetails, sourceModelBuildingContext );
				XmlAnnotationHelper.applyJdbcTypeCode( jaxbId.getJdbcTypeCode(), memberDetails, sourceModelBuildingContext );
				XmlAnnotationHelper.applyTemporal( jaxbId.getTemporal(), memberDetails, sourceModelBuildingContext );

				XmlAnnotationHelper.applyGeneratedValue( jaxbId.getGeneratedValue(), memberDetails, sourceModelBuildingContext );
				XmlAnnotationHelper.applySequenceGenerator( jaxbId.getSequenceGenerator(), memberDetails, sourceModelBuildingContext );
				XmlAnnotationHelper.applyTableGenerator( jaxbId.getTableGenerator(), memberDetails, sourceModelBuildingContext );
				XmlAnnotationHelper.applyUuidGenerator( jaxbId.getUuidGenerator(), memberDetails, sourceModelBuildingContext );

				// todo : unsaved-value?
			}
		}
		else {
			final JaxbEmbeddedId jaxbEmbeddedId = attributes.getEmbeddedId();
			assert jaxbEmbeddedId != null;
			final AccessType accessType = coalesce( jaxbEmbeddedId.getAccess(), classAccessType );

			final MutableMemberDetails memberDetails = XmlAttributeHelper.findAttributeMember(
					jaxbEmbeddedId.getName(),
					accessType,
					mutableClassDetails,
					sourceModelBuildingContext
			);

			XmlAnnotationHelper.applyAccess( accessType, memberDetails, sourceModelBuildingContext );
			XmlAnnotationHelper.applyAttributeAccessor( jaxbEmbeddedId.getAttributeAccessor(), memberDetails, sourceModelBuildingContext );

			XmlAnnotationHelper.applyAttributeOverrides( jaxbEmbeddedId.getAttributeOverride(), memberDetails, sourceModelBuildingContext );
		}

		for ( int i = 0; i < attributes.getBasicAttributes().size(); i++ ) {
			XmlAttributeHelper.handleBasicAttribute(
					attributes.getBasicAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		if ( attributes.getNaturalId() != null ) {
			throw new UnsupportedOperationException( "Support for natural-id not yet implemented" );
		}

		for ( int i = 0; i < attributes.getEmbeddedAttributes().size(); i++ ) {
			throw new UnsupportedOperationException( "Support for embedded attributes not yet implemented" );
		}

		for ( int i = 0; i < attributes.getOneToOneAttributes().size(); i++ ) {
			throw new UnsupportedOperationException( "Support for one-to-one attributes not yet implemented" );
		}

		for ( int i = 0; i < attributes.getManyToOneAttributes().size(); i++ ) {
			throw new UnsupportedOperationException( "Support for many-to-one attributes not yet implemented" );
		}

		for ( int i = 0; i < attributes.getElementCollectionAttributes().size(); i++ ) {
			throw new UnsupportedOperationException( "Support for element-collection attributes not yet implemented" );
		}

		for ( int i = 0; i < attributes.getOneToManyAttributes().size(); i++ ) {
			throw new UnsupportedOperationException( "Support for one-to-many attributes not yet implemented" );
		}

		for ( int i = 0; i < attributes.getManyToManyAttributes().size(); i++ ) {
			throw new UnsupportedOperationException( "Support for many-to-many attributes not yet implemented" );
		}

		for ( int i = 0; i < attributes.getDiscriminatedAssociations().size(); i++ ) {
			throw new UnsupportedOperationException( "Support for any attributes not yet implemented" );
		}

		for ( int i = 0; i < attributes.getPluralDiscriminatedAssociations().size(); i++ ) {
			throw new UnsupportedOperationException( "Support for many-to-any attributes not yet implemented" );
		}
	}
}
