/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import java.beans.Introspector;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.jaxb.mapping.AttributesContainer;
import org.hibernate.boot.jaxb.mapping.JaxbBasic;
import org.hibernate.boot.jaxb.mapping.JaxbElementCollection;
import org.hibernate.boot.jaxb.mapping.JaxbEmbedded;
import org.hibernate.boot.jaxb.mapping.JaxbHbmAnyMapping;
import org.hibernate.boot.jaxb.mapping.JaxbHbmManyToAny;
import org.hibernate.boot.jaxb.mapping.JaxbManyToMany;
import org.hibernate.boot.jaxb.mapping.JaxbManyToOne;
import org.hibernate.boot.jaxb.mapping.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.JaxbOneToMany;
import org.hibernate.boot.jaxb.mapping.JaxbOneToOne;
import org.hibernate.boot.jaxb.mapping.PersistentAttribute;
import org.hibernate.models.orm.MemberResolutionException;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.spi.FieldDetails;
import org.hibernate.models.source.spi.MethodDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;
import jakarta.persistence.Embedded;

import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class XmlAttributeHelper {
	/**
	 * Find the member backing the named attribute
	 */
	public static MutableMemberDetails getAttributeMember(
			String attributeName,
			AccessType accessType,
			MutableClassDetails classDetails,
			SourceModelBuildingContext buildingContext) {
		final MutableMemberDetails result = findAttributeMember(
				attributeName,
				accessType,
				classDetails,
				buildingContext
		);
		if ( result == null ) {
			throw new MemberResolutionException(
					String.format(
							"Could not locate attribute member - %s (%s)",
							attributeName,
							classDetails.getName()
					)
			);
		}
		return result;
	}

	/**
	 * Find the member backing the named attribute
	 */
	public static MutableMemberDetails findAttributeMember(
			String attributeName,
			AccessType accessType,
			MutableClassDetails classDetails,
			SourceModelBuildingContext buildingContext) {
		if ( accessType == AccessType.PROPERTY ) {
			for ( int i = 0; i < classDetails.getMethods().size(); i++ ) {
				final MethodDetails methodDetails = classDetails.getMethods().get( i );
				if ( methodDetails.getMethodKind() == MethodDetails.MethodKind.GETTER ) {
					if ( methodDetails.getName().startsWith( "get" ) ) {
						final String stemName = methodDetails.getName().substring( 3 );
						final String decapitalizedStemName = Introspector.decapitalize( stemName );
						if ( stemName.equals( attributeName ) || decapitalizedStemName.equals( attributeName ) ) {
							return (MutableMemberDetails) methodDetails;
						}
					}
					else if ( methodDetails.getName().startsWith( "is" ) ) {
						final String stemName = methodDetails.getName().substring( 2 );
						final String decapitalizedStemName = Introspector.decapitalize( stemName );
						if ( stemName.equals( attributeName ) || decapitalizedStemName.equals( attributeName ) ) {
							return (MutableMemberDetails) methodDetails;
						}
					}
				}
			}
		}
		else {
			assert accessType == AccessType.FIELD;
			for ( int i = 0; i < classDetails.getFields().size(); i++ ) {
				final FieldDetails fieldDetails = classDetails.getFields().get( i );
				if ( fieldDetails.getName().equals( attributeName ) ) {
					return (MutableMemberDetails) fieldDetails;
				}
			}
		}

		return null;
	}

	public static void handleNaturalId(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbNaturalId == null ) {
			return;
		}

		XmlAnnotationHelper.applyNaturalIdCache( jaxbNaturalId, mutableClassDetails, sourceModelBuildingContext );

		jaxbNaturalId.getBasic().forEach( (jaxbBasic) -> {
			final MutableMemberDetails backingMember = handleBasicAttribute(
					jaxbBasic,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			XmlAnnotationHelper.applyNaturalId( jaxbNaturalId, backingMember, sourceModelBuildingContext );
		} );
		jaxbNaturalId.getEmbedded().forEach( (jaxbEmbedded) -> {
			final MutableMemberDetails backingMember = handleEmbeddedAttribute(
					jaxbEmbedded,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			XmlAnnotationHelper.applyNaturalId( jaxbNaturalId, backingMember, sourceModelBuildingContext );
		} );
		jaxbNaturalId.getManyToOne().forEach( (jaxbManyToOne) -> {
			final MutableMemberDetails backingMember = handleManyToOneAttribute(
					jaxbManyToOne,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			XmlAnnotationHelper.applyNaturalId( jaxbNaturalId, backingMember, sourceModelBuildingContext );
		} );
		jaxbNaturalId.getAny().forEach( (jaxbAny) -> {
			final MutableMemberDetails backingMember = handleDiscriminatedAssociationAttribute(
					jaxbAny,
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			XmlAnnotationHelper.applyNaturalId( jaxbNaturalId, backingMember, sourceModelBuildingContext );
		} );
	}



	public static void handleAttributes(
			AttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		for ( int i = 0; i < attributesContainer.getBasicAttributes().size(); i++ ) {
			handleBasicAttribute(
					attributesContainer.getBasicAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getEmbeddedAttributes().size(); i++ ) {
			handleEmbeddedAttribute(
					attributesContainer.getEmbeddedAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getOneToOneAttributes().size(); i++ ) {
			handleOneToOneAttribute(
					attributesContainer.getOneToOneAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getManyToOneAttributes().size(); i++ ) {
			handleManyToOneAttribute(
					attributesContainer.getManyToOneAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getDiscriminatedAssociations().size(); i++ ) {
			handleDiscriminatedAssociationAttribute(
					attributesContainer.getDiscriminatedAssociations().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getElementCollectionAttributes().size(); i++ ) {
			handleElementCollectionAttribute(
					attributesContainer.getElementCollectionAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getOneToManyAttributes().size(); i++ ) {
			handleOneToManyAttribute(
					attributesContainer.getOneToManyAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getManyToManyAttributes().size(); i++ ) {
			handleManyToManyAttribute(
					attributesContainer.getManyToManyAttributes().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

		for ( int i = 0; i < attributesContainer.getPluralDiscriminatedAssociations().size(); i++ ) {
			handlePluralDiscriminatedAssociationAttribute(
					attributesContainer.getPluralDiscriminatedAssociations().get( i ),
					mutableClassDetails,
					classAccessType,
					sourceModelBuildingContext
			);
		}

	}

	public static void applyCommonAttributeAnnotations(
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

	public static MutableMemberDetails handleBasicAttribute(
			JaxbBasic jaxbBasic,
			MutableClassDetails declarer,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbBasic.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlAttributeHelper.getAttributeMember(
				jaxbBasic.getName(),
				accessType,
				declarer,
				sourceModelBuildingContext
		);

		XmlAnnotationHelper.applyBasic( jaxbBasic, memberDetails, sourceModelBuildingContext );

		applyCommonAttributeAnnotations( jaxbBasic, memberDetails, accessType, sourceModelBuildingContext );
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

	public static MutableMemberDetails handleEmbeddedAttribute(
			JaxbEmbedded jaxbEmbedded,
			MutableClassDetails declarer,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbEmbedded.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlAttributeHelper.getAttributeMember(
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

		XmlAttributeHelper.applyCommonAttributeAnnotations( jaxbEmbedded, memberDetails, accessType, sourceModelBuildingContext );
		XmlAnnotationHelper.applyAttributeOverrides( jaxbEmbedded.getAttributeOverride(), memberDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyAssociationOverrides( jaxbEmbedded.getAssociationOverride(), memberDetails, sourceModelBuildingContext );

		return memberDetails;
	}

	public static MutableMemberDetails handleOneToOneAttribute(
			JaxbOneToOne jaxbOneToOne,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for one-to-one attributes not yet implemented" );
	}

	public static MutableMemberDetails handleManyToOneAttribute(
			JaxbManyToOne jaxbOneToOne,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for many-to-one attributes not yet implemented" );
	}

	public static MutableMemberDetails handleDiscriminatedAssociationAttribute(
			JaxbHbmAnyMapping jaxbHbmAnyMapping,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for any attributes not yet implemented" );
	}

	public static MutableMemberDetails handleElementCollectionAttribute(
			JaxbElementCollection jaxbElementCollection,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for element-collection attributes not yet implemented" );
	}

	public static MutableMemberDetails handleOneToManyAttribute(
			JaxbOneToMany jaxbOneToMany,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for one-to-many attributes not yet implemented" );
	}

	public static MutableMemberDetails handleManyToManyAttribute(
			JaxbManyToMany jaxbManyToMany,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for many-to-many attributes not yet implemented" );
	}

	public static MutableMemberDetails handlePluralDiscriminatedAssociationAttribute(
			JaxbHbmManyToAny jaxbHbmManyToAny,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		throw new UnsupportedOperationException( "Support for many-to-any attributes not yet implemented" );
	}
}