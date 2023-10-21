/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import org.hibernate.boot.jaxb.mapping.AttributesContainer;
import org.hibernate.boot.jaxb.mapping.JaxbBasic;
import org.hibernate.boot.jaxb.mapping.JaxbElementCollection;
import org.hibernate.boot.jaxb.mapping.JaxbEmbedded;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddedId;
import org.hibernate.boot.jaxb.mapping.JaxbHbmAnyMapping;
import org.hibernate.boot.jaxb.mapping.JaxbHbmManyToAny;
import org.hibernate.boot.jaxb.mapping.JaxbId;
import org.hibernate.boot.jaxb.mapping.JaxbManyToMany;
import org.hibernate.boot.jaxb.mapping.JaxbManyToOne;
import org.hibernate.boot.jaxb.mapping.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.JaxbOneToMany;
import org.hibernate.boot.jaxb.mapping.JaxbOneToOne;
import org.hibernate.boot.jaxb.mapping.PersistentAttribute;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;
import jakarta.persistence.Embedded;

import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * Helper for handling persistent attributes defined in mapping XML in metadata-complete mode
 *
 * @author Steve Ebersole
 */
public class AttributeProcessor {
	public static void processNaturalId(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbNaturalId == null ) {
			return;
		}

		XmlAnnotationHelper.applyNaturalIdCache( jaxbNaturalId, mutableClassDetails, sourceModelBuildingContext );

		jaxbNaturalId.getBasic().forEach( (jaxbBasic) -> {
			final MutableMemberDetails backingMember = processBasicAttribute(
					jaxbBasic,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			XmlAnnotationHelper.applyNaturalId( jaxbNaturalId, backingMember, sourceModelBuildingContext );
		} );
		jaxbNaturalId.getEmbedded().forEach( (jaxbEmbedded) -> {
			final MutableMemberDetails backingMember = processEmbeddedAttribute(
					jaxbEmbedded,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			XmlAnnotationHelper.applyNaturalId( jaxbNaturalId, backingMember, sourceModelBuildingContext );
		} );
		jaxbNaturalId.getManyToOne().forEach( (jaxbManyToOne) -> {
			final MutableMemberDetails backingMember = processManyToOneAttribute(
					jaxbManyToOne,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			XmlAnnotationHelper.applyNaturalId( jaxbNaturalId, backingMember, sourceModelBuildingContext );
		} );
		jaxbNaturalId.getAny().forEach( (jaxbAny) -> {
			final MutableMemberDetails backingMember = processDiscriminatedAssociationAttribute(
					jaxbAny,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			XmlAnnotationHelper.applyNaturalId( jaxbNaturalId, backingMember, sourceModelBuildingContext );
		} );
	}

	public static void processAttributes(
			AttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		for ( int i = 0; i < attributesContainer.getBasicAttributes().size(); i++ ) {
			processBasicAttribute(
					attributesContainer.getBasicAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getEmbeddedAttributes().size(); i++ ) {
			processEmbeddedAttribute(
					attributesContainer.getEmbeddedAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getOneToOneAttributes().size(); i++ ) {
			processOneToOneAttribute(
					attributesContainer.getOneToOneAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getManyToOneAttributes().size(); i++ ) {
			processManyToOneAttribute(
					attributesContainer.getManyToOneAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getDiscriminatedAssociations().size(); i++ ) {
			processDiscriminatedAssociationAttribute(
					attributesContainer.getDiscriminatedAssociations().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getElementCollectionAttributes().size(); i++ ) {
			processElementCollectionAttribute(
					attributesContainer.getElementCollectionAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getOneToManyAttributes().size(); i++ ) {
			processOneToManyAttribute(
					attributesContainer.getOneToManyAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getManyToManyAttributes().size(); i++ ) {
			processManyToManyAttribute(
					attributesContainer.getManyToManyAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getPluralDiscriminatedAssociations().size(); i++ ) {
			processPluralDiscriminatedAssociationAttribute(
					attributesContainer.getPluralDiscriminatedAssociations().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

	}

	public static void processCommonAttributeAnnotations(
			PersistentAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			AccessType accessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlAnnotationHelper.applyAccess( accessType, memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyAttributeAccessor(
				jaxbAttribute.getAttributeAccessor(),
				memberDetails,
				sourceModelBuildingContext
		);
	}

	public static void processBasicIdAttribute(
			JaxbId jaxbId,
			MutableClassDetails declarer,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbId.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbId.getName(),
				accessType,
				declarer,
				sourceModelBuildingContext
		);
		XmlAnnotationHelper.applyId( jaxbId, memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyBasic( jaxbId, memberDetails, sourceModelBuildingContext );
		AttributeProcessor.processCommonAttributeAnnotations(
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

	public static void processEmbeddedIdAttribute(
			JaxbEmbeddedId jaxbEmbeddedId,
			MutableClassDetails classDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbEmbeddedId.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.findAttributeMember(
				jaxbEmbeddedId.getName(),
				accessType,
				classDetails,
				sourceModelBuildingContext
		);

		XmlAnnotationHelper.applyEmbeddedId( jaxbEmbeddedId, memberDetails, sourceModelBuildingContext );
		AttributeProcessor.processCommonAttributeAnnotations(
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

	public static MutableMemberDetails processBasicAttribute(
			JaxbBasic jaxbBasic,
			MutableClassDetails declarer,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbBasic.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbBasic.getName(),
				accessType,
				declarer,
				sourceModelBuildingContext
		);

		XmlAnnotationHelper.applyBasic( jaxbBasic, memberDetails, sourceModelBuildingContext );

		processCommonAttributeAnnotations( jaxbBasic, memberDetails, accessType, sourceModelBuildingContext );
		// only semi-common
		XmlAnnotationHelper.applyOptimisticLockInclusion( jaxbBasic.isOptimisticLock(), memberDetails, sourceModelBuildingContext );

		XmlAnnotationHelper.applyColumn( jaxbBasic.getColumn(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyFormula( jaxbBasic.getFormula(), memberDetails, sourceModelBuildingContext );

		// todo : value generation

		XmlAnnotationHelper.applyConvert( jaxbBasic.getConvert(), memberDetails, sourceModelBuildingContext );

		XmlAnnotationHelper.applyUserType( jaxbBasic.getType(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyTemporal( jaxbBasic.getTemporal(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyLob( jaxbBasic.getLob(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyEnumerated( jaxbBasic.getEnumerated(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyNationalized( jaxbBasic.getNationalized(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyJdbcTypeCode( jaxbBasic.getJdbcTypeCode(), memberDetails, sourceModelBuildingContext );

		return memberDetails;
	}

	public static MutableMemberDetails processEmbeddedAttribute(
			JaxbEmbedded jaxbEmbedded,
			MutableClassDetails declarer,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbEmbedded.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbEmbedded.getName(),
				accessType,
				declarer,
				sourceModelBuildingContext
		);

		final DynamicAnnotationUsage<Embedded> annotationUsage = new DynamicAnnotationUsage<>(
				Embedded.class,
				memberDetails
		);
		memberDetails.addAnnotationUsage( annotationUsage );

		processCommonAttributeAnnotations( jaxbEmbedded, memberDetails, accessType, sourceModelBuildingContext );
		XmlAnnotationHelper.applyAttributeOverrides( jaxbEmbedded.getAttributeOverride(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyAssociationOverrides( jaxbEmbedded.getAssociationOverride(), memberDetails, sourceModelBuildingContext );

		return memberDetails;
	}

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processOneToOneAttribute(
			JaxbOneToOne jaxbOneToOne,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for one-to-one attributes not yet implemented" );
	}

	public static MutableMemberDetails processManyToOneAttribute(
			JaxbManyToOne jaxbOneToOne,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for many-to-one attributes not yet implemented" );
	}

	public static MutableMemberDetails processDiscriminatedAssociationAttribute(
			JaxbHbmAnyMapping jaxbHbmAnyMapping,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for any attributes not yet implemented" );
	}

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processElementCollectionAttribute(
			JaxbElementCollection jaxbElementCollection,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for element-collection attributes not yet implemented" );
	}

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processOneToManyAttribute(
			JaxbOneToMany jaxbOneToMany,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for one-to-many attributes not yet implemented" );
	}

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processManyToManyAttribute(
			JaxbManyToMany jaxbManyToMany,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for many-to-many attributes not yet implemented" );
	}

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processPluralDiscriminatedAssociationAttribute(
			JaxbHbmManyToAny jaxbHbmManyToAny,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for many-to-any attributes not yet implemented" );
	}
}
