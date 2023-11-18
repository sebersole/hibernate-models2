/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal;

import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBaseAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAnyMappingImpl;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.spi.XmlDocumentContext;

import jakarta.persistence.AccessType;

import static org.hibernate.models.orm.categorize.xml.internal.attr.AnyMappingAttributeProcessing.processAnyMappingAttribute;
import static org.hibernate.models.orm.categorize.xml.internal.attr.BasicAttributeProcessing.processBasicAttribute;
import static org.hibernate.models.orm.categorize.xml.internal.attr.ElementCollectionAttributeProcessing.processElementCollectionAttribute;
import static org.hibernate.models.orm.categorize.xml.internal.attr.EmbeddedAttributeProcessing.processEmbeddedAttribute;
import static org.hibernate.models.orm.categorize.xml.internal.attr.ManyToManyAttributeProcessing.processManyToManyAttribute;
import static org.hibernate.models.orm.categorize.xml.internal.attr.ManyToOneAttributeProcessing.processManyToOneAttribute;
import static org.hibernate.models.orm.categorize.xml.internal.attr.OneToManyAttributeProcessing.processOneToManyAttribute;
import static org.hibernate.models.orm.categorize.xml.internal.attr.OneToOneAttributeProcessing.processOneToOneAttribute;
import static org.hibernate.models.orm.categorize.xml.internal.attr.PluralAnyMappingAttributeProcessing.processPluralAnyMappingAttributes;

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
			XmlDocumentContext xmlDocumentContext) {
		processNaturalId( jaxbNaturalId, mutableClassDetails, classAccessType, null, xmlDocumentContext );
	}

	public static void processNaturalId(
			JaxbNaturalId jaxbNaturalId,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			MemberAdjuster memberAdjuster,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbNaturalId == null ) {
			return;
		}

		XmlAnnotationHelper.applyNaturalIdCache( jaxbNaturalId, mutableClassDetails );

		processBaseAttributes( jaxbNaturalId, mutableClassDetails, classAccessType, memberAdjuster, xmlDocumentContext );
	}

	public static void processBaseAttributes(
			JaxbBaseAttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			MemberAdjuster memberAdjuster,
			XmlDocumentContext xmlDocumentContext) {
		for ( int i = 0; i < attributesContainer.getBasicAttributes().size(); i++ ) {
			final JaxbBasicImpl jaxbBasic = attributesContainer.getBasicAttributes().get( i );
			final MutableMemberDetails memberDetails = processBasicAttribute(
					jaxbBasic,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbBasic, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getEmbeddedAttributes().size(); i++ ) {
			final JaxbEmbeddedImpl jaxbEmbedded = attributesContainer.getEmbeddedAttributes().get( i );
			final MutableMemberDetails memberDetails = processEmbeddedAttribute(
					jaxbEmbedded,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbEmbedded, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getManyToOneAttributes().size(); i++ ) {
			final JaxbManyToOneImpl jaxbManyToOne = attributesContainer.getManyToOneAttributes().get( i );
			final MutableMemberDetails memberDetails = processManyToOneAttribute(
					jaxbManyToOne,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbManyToOne, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getAnyMappingAttributes().size(); i++ ) {
			final JaxbAnyMappingImpl jaxbAnyMapping = attributesContainer.getAnyMappingAttributes().get( i );
			final MutableMemberDetails memberDetails = processAnyMappingAttribute(
					jaxbAnyMapping,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbAnyMapping, xmlDocumentContext );
			}
		}
	}

	@FunctionalInterface
	public interface MemberAdjuster {
		<M extends MutableMemberDetails> void adjust(M member, JaxbPersistentAttribute jaxbPersistentAttribute, XmlDocumentContext xmlDocumentContext);
	}

	public static void processAttributes(
			JaxbAttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		processAttributes( attributesContainer, mutableClassDetails, classAccessType, null, xmlDocumentContext );
	}

	public static void processAttributes(
			JaxbAttributesContainer attributesContainer,
			MutableClassDetails mutableClassDetails,
			AccessType classAccessType,
			MemberAdjuster memberAdjuster,
			XmlDocumentContext xmlDocumentContext) {
		processBaseAttributes( attributesContainer, mutableClassDetails, classAccessType, memberAdjuster, xmlDocumentContext );

		for ( int i = 0; i < attributesContainer.getOneToOneAttributes().size(); i++ ) {
			final JaxbOneToOneImpl jaxbOneToOne = attributesContainer.getOneToOneAttributes().get( i );
			final MutableMemberDetails memberDetails = processOneToOneAttribute(
					jaxbOneToOne,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbOneToOne, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getElementCollectionAttributes().size(); i++ ) {
			final JaxbElementCollectionImpl jaxbElementCollection = attributesContainer.getElementCollectionAttributes().get( i );
			final MutableMemberDetails memberDetails = processElementCollectionAttribute(
					jaxbElementCollection,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbElementCollection, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getOneToManyAttributes().size(); i++ ) {
			final JaxbOneToManyImpl jaxbOneToMany = attributesContainer.getOneToManyAttributes().get( i );
			final MutableMemberDetails memberDetails = processOneToManyAttribute(
					jaxbOneToMany,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbOneToMany, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getManyToManyAttributes().size(); i++ ) {
			final JaxbManyToManyImpl jaxbManyToMany = attributesContainer.getManyToManyAttributes().get( i );
			final MutableMemberDetails memberDetails = processManyToManyAttribute(
					jaxbManyToMany,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbManyToMany, xmlDocumentContext );
			}
		}

		for ( int i = 0; i < attributesContainer.getPluralAnyMappingAttributes().size(); i++ ) {
			final JaxbPluralAnyMappingImpl jaxbPluralAnyMapping = attributesContainer.getPluralAnyMappingAttributes()
					.get( i );
			final MutableMemberDetails memberDetails = processPluralAnyMappingAttributes(
					jaxbPluralAnyMapping,
					mutableClassDetails,
					classAccessType,
					xmlDocumentContext
			);
			if ( memberAdjuster != null ) {
				memberAdjuster.adjust( memberDetails, jaxbPluralAnyMapping, xmlDocumentContext );
			}
		}
	}
}
