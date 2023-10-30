/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.internal.Abstract;
import org.hibernate.boot.internal.Extends;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityOrMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.models.ModelsException;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.categorize.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.orm.categorize.xml.spi.XmlProcessingResult;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.internal.SourceModelLogging;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.source.internal.dynamic.MapModeFieldDetails;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.SourceModelBuildingContext;
import org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies;

import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.internal.util.NullnessHelper.nullif;
import static org.hibernate.models.orm.categorize.xml.internal.AttributeProcessor.processAttributes;
import static org.hibernate.models.orm.categorize.xml.internal.AttributeProcessor.processNaturalId;

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
			JaxbEntityMappingsImpl jaxbRoot,
			JaxbEntityImpl jaxbEntity,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final MutableClassDetails classDetails;
		final AccessType classAccessType;
		final AttributeProcessor.MemberAdjuster memberAdjuster;
		final boolean isDynamic;

		if ( StringHelper.isEmpty( jaxbEntity.getClazz() ) ) {
			// no class == dynamic
			if ( StringHelper.isEmpty( jaxbEntity.getName() ) ) {
				throw new ModelsException( "Assumed dynamic entity did not define entity-name" );
			}

			memberAdjuster = ManagedTypeProcessor::adjustDynamicTypeMember;
			classAccessType = AccessType.FIELD;
			classDetails = (MutableClassDetails) sourceModelBuildingContext.getClassDetailsRegistry().resolveClassDetails(
					jaxbEntity.getName(),
					() -> {
						final DynamicClassDetails dynamicClassDetails = new DynamicClassDetails(
								jaxbEntity.getName(),
								null,
								false,
								null,
								sourceModelBuildingContext
						);
						return dynamicClassDetails;
					}
			);

			prepareDynamicClass( classDetails, jaxbEntity, persistenceUnitMetadata, sourceModelBuildingContext );
		}
		else {
			memberAdjuster = ManagedTypeProcessor::adjustNonDynamicTypeMember;
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEntity );
			classDetails = (MutableClassDetails) sourceModelBuildingContext.getClassDetailsRegistry().resolveClassDetails( className );
			classAccessType = coalesce(
					jaxbEntity.getAccess(),
					persistenceUnitMetadata.getAccessType()
			);
		}

		classDetails.clearMemberAnnotationUsages();
		classDetails.clearAnnotationUsages();

		// from here, processing is the same between override and metadata-complete modes (aside from the dynamic model handling)

		processEntityMetadata(
				classDetails,
				jaxbEntity,
				AccessType.FIELD,
				ManagedTypeProcessor::adjustDynamicTypeMember,
				persistenceUnitMetadata,
				sourceModelBuildingContext
		);
	}

	/**
	 * Creates fake FieldDetails for each attribute defined in the XML
	 */
	private static void prepareDynamicClass(
			MutableClassDetails classDetails,
			JaxbManagedType jaxbManagedType,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( jaxbManagedType instanceof JaxbEntityImpl ) {
			final JaxbEntityImpl jaxbDynamicEntity = (JaxbEntityImpl) jaxbManagedType;
			final JaxbAttributesContainerImpl attributes = jaxbDynamicEntity.getAttributes();

			if ( CollectionHelper.isNotEmpty( attributes.getIdAttributes() ) ) {
				// <id/>
				attributes.getIdAttributes().forEach( (jaxbId) -> {
					final ClassDetails attributeJavaType = determineDynamicAttributeJavaType(
							jaxbId,
							sourceModelBuildingContext
					);
					final MapModeFieldDetails member = new MapModeFieldDetails(
							jaxbId.getName(),
							attributeJavaType,
							sourceModelBuildingContext
					);
					classDetails.addField( member );
				} );
			}
			else {
				// <embedded-id/>
				final JaxbEmbeddedIdImpl embeddedId = attributes.getEmbeddedIdAttribute();
				final ClassDetails attributeJavaType = determineDynamicAttributeJavaType(
						embeddedId,
						sourceModelBuildingContext
				);
				final MapModeFieldDetails member = new MapModeFieldDetails(
						embeddedId.getName(),
						attributeJavaType,
						sourceModelBuildingContext
				);
				classDetails.addField( member );
			}

			// <natural-id/>
			if ( attributes.getNaturalId() != null ) {
				attributes.getNaturalId().getBasicAttributes().forEach( (jaxbBasic) -> {
					final ClassDetails attributeJavaType = determineDynamicAttributeJavaType(
							jaxbBasic,
							sourceModelBuildingContext
					);
					final MapModeFieldDetails member = new MapModeFieldDetails(
							jaxbBasic.getName(),
							attributeJavaType,
							sourceModelBuildingContext
					);
					classDetails.addField( member );
				} );

				attributes.getNaturalId().getEmbeddedAttributes().forEach( (jaxbEmbedded) -> {
					final ClassDetails attributeJavaType = determineDynamicAttributeJavaType(
							jaxbEmbedded,
							sourceModelBuildingContext
					);
					final MapModeFieldDetails member = new MapModeFieldDetails(
							jaxbEmbedded.getName(),
							attributeJavaType,
							sourceModelBuildingContext
					);
					classDetails.addField( member );
				} );

				attributes.getNaturalId().getManyToOneAttributes().forEach( (jaxbManyToOne) -> {
					final ClassDetails attributeJavaType = determineDynamicAttributeJavaType(
							jaxbManyToOne,
							sourceModelBuildingContext
					);
					final MapModeFieldDetails member = new MapModeFieldDetails(
							jaxbManyToOne.getName(),
							attributeJavaType,
							sourceModelBuildingContext
					);
					classDetails.addField( member );
				} );

				attributes.getNaturalId().getAnyMappingAttributes().forEach( (jaxbAnyMapping) -> {
					final ClassDetails attributeJavaType = determineDynamicAttributeJavaType(
							jaxbAnyMapping,
							sourceModelBuildingContext
					);
					final MapModeFieldDetails member = new MapModeFieldDetails(
							jaxbAnyMapping.getName(),
							attributeJavaType,
							sourceModelBuildingContext
					);
					classDetails.addField( member );
				} );
			}
		}

		final JaxbAttributesContainer attributes = jaxbManagedType.getAttributes();
		attributes.getBasicAttributes().forEach( (jaxbBasic) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbBasic.getName(),
					determineDynamicAttributeJavaType( jaxbBasic, sourceModelBuildingContext ),
					sourceModelBuildingContext
			);
			classDetails.addField( member );
		} );

		attributes.getEmbeddedAttributes().forEach( (jaxbEmbedded) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbEmbedded.getName(),
					determineDynamicAttributeJavaType( jaxbEmbedded, sourceModelBuildingContext ),
					sourceModelBuildingContext
			);
			classDetails.addField( member );
		} );

		attributes.getOneToOneAttributes().forEach( (jaxbOneToOne) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbOneToOne.getName(),
					determineDynamicAttributeJavaType( jaxbOneToOne, sourceModelBuildingContext ),
					sourceModelBuildingContext
			);
			classDetails.addField( member );
		} );

		attributes.getManyToOneAttributes().forEach( (jaxbManyToOne) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbManyToOne.getName(),
					determineDynamicAttributeJavaType( jaxbManyToOne, sourceModelBuildingContext ),
					sourceModelBuildingContext
			);
			classDetails.addField( member );
		} );

		attributes.getAnyMappingAttributes().forEach( (jaxbAnyMapping) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbAnyMapping.getName(),
					determineDynamicAttributeJavaType( jaxbAnyMapping, sourceModelBuildingContext ),
					sourceModelBuildingContext
			);
			classDetails.addField( member );
		} );

		attributes.getElementCollectionAttributes().forEach( (jaxbElementCollection) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbElementCollection.getName(),
					determineDynamicAttributeJavaType( jaxbElementCollection, sourceModelBuildingContext ),
					sourceModelBuildingContext
			);
			classDetails.addField( member );
		} );

		attributes.getOneToManyAttributes().forEach( (jaxbOneToMany) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbOneToMany.getName(),
					determineDynamicAttributeJavaType( jaxbOneToMany, sourceModelBuildingContext ),
					sourceModelBuildingContext
			);
			classDetails.addField( member );
		} );

		attributes.getManyToManyAttributes().forEach( (jaxbManyToMany) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbManyToMany.getName(),
					determineDynamicAttributeJavaType( jaxbManyToMany, sourceModelBuildingContext ),
					sourceModelBuildingContext
			);
			classDetails.addField( member );
		} );

		attributes.getPluralAnyMappingAttributes().forEach( (jaxbPluralAnyMapping) -> {
			final MapModeFieldDetails member = new MapModeFieldDetails(
					jaxbPluralAnyMapping.getName(),
					determineDynamicAttributeJavaType( jaxbPluralAnyMapping, sourceModelBuildingContext ),
					sourceModelBuildingContext
			);
			classDetails.addField( member );
		} );
	}

	private static ClassDetails determineDynamicAttributeJavaType(
			JaxbPersistentAttribute jaxbPersistentAttribute,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final ClassDetailsRegistry classDetailsRegistry = sourceModelBuildingContext.getClassDetailsRegistry();

		if ( jaxbPersistentAttribute instanceof JaxbIdImpl ) {
			final JaxbIdImpl jaxbId = (JaxbIdImpl) jaxbPersistentAttribute;
			return XmlAnnotationHelper.resolveJavaType( jaxbId.getTarget(), sourceModelBuildingContext );
		}

		if ( jaxbPersistentAttribute instanceof JaxbEmbeddedIdImpl ) {
			final JaxbEmbeddedIdImpl jaxbEmbeddedId = (JaxbEmbeddedIdImpl) jaxbPersistentAttribute;
			final String target = jaxbEmbeddedId.getTarget();
			if ( StringHelper.isEmpty( target ) ) {
				return null;
			}
			return classDetailsRegistry.resolveClassDetails(
					target,
					() -> new DynamicClassDetails( target, sourceModelBuildingContext )
			);
		}

		if ( jaxbPersistentAttribute instanceof JaxbBasicImpl ) {
			final JaxbBasicImpl jaxbBasic = (JaxbBasicImpl) jaxbPersistentAttribute;
			return XmlAnnotationHelper.resolveJavaType( jaxbBasic.getTarget(), sourceModelBuildingContext );
		}

		if ( jaxbPersistentAttribute instanceof JaxbEmbeddedImpl ) {
			final JaxbEmbeddedImpl jaxbEmbedded = (JaxbEmbeddedImpl) jaxbPersistentAttribute;
			final String target = jaxbEmbedded.getTarget();
			if ( StringHelper.isEmpty( target ) ) {
				return null;
			}
			return classDetailsRegistry.resolveClassDetails(
					target,
					() -> new DynamicClassDetails( target, sourceModelBuildingContext )
			);
		}

		if ( jaxbPersistentAttribute instanceof JaxbOneToOneImpl ) {
			final JaxbOneToOneImpl jaxbOneToOne = (JaxbOneToOneImpl) jaxbPersistentAttribute;
			final String target = jaxbOneToOne.getTargetEntity();
			if ( StringHelper.isEmpty( target ) ) {
				return null;
			}
			return classDetailsRegistry.resolveClassDetails(
					target,
					() -> new DynamicClassDetails(
							target,
							null,
							false,
							null,
							sourceModelBuildingContext
					)
			);
		}

		if ( jaxbPersistentAttribute instanceof JaxbAnyMappingImpl ) {
			return classDetailsRegistry.getClassDetails( Object.class.getName() );
		}

		if ( jaxbPersistentAttribute instanceof JaxbPluralAttribute ) {
			final JaxbPluralAttribute jaxbPluralAttribute = (JaxbPluralAttribute) jaxbPersistentAttribute;
			final LimitedCollectionClassification classification = nullif( jaxbPluralAttribute.getClassification(), LimitedCollectionClassification.BAG );
			switch ( classification ) {
				case BAG: {
					return classDetailsRegistry.resolveClassDetails( Collection.class.getName() );
				}
				case LIST: {
					return classDetailsRegistry.resolveClassDetails( List.class.getName() );
				}
				case SET: {
					return classDetailsRegistry.resolveClassDetails( Set.class.getName() );
				}
				case MAP: {
					return classDetailsRegistry.resolveClassDetails( Map.class.getName() );
				}
			}
		}
		throw new UnsupportedOperationException( "Resolution of dynamic attribute Java type not yet implemented for " + jaxbPersistentAttribute );
	}

	private static void adjustDynamicTypeMember(
			MutableMemberDetails memberDetails,
			JaxbPersistentAttribute jaxbAttribute,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlAnnotationHelper.applyAttributeAccessor(
				BuiltInPropertyAccessStrategies.MAP.getExternalName(),
				memberDetails,
				sourceModelBuildingContext
		);
	}

	private static void processEntityMetadata(
			MutableClassDetails classDetails,
			JaxbEntityImpl jaxbEntity,
			AccessType classAccessType,
			AttributeProcessor.MemberAdjuster memberAdjuster,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlAnnotationHelper.applyEntity( jaxbEntity, classDetails, sourceModelBuildingContext );
		XmlAnnotationHelper.applyInheritance( jaxbEntity, classDetails, sourceModelBuildingContext );
		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails ) );

		if ( jaxbEntity.isAbstract() != null ) {
			XmlProcessingHelper.makeAnnotation( Abstract.class, classDetails );
		}

		if ( StringHelper.isNotEmpty( jaxbEntity.getExtends() ) ) {
			final DynamicAnnotationUsage<Extends> extendsAnn = XmlProcessingHelper.makeAnnotation(
					Extends.class,
					classDetails
			);
			extendsAnn.setAttributeValue( "superType", jaxbEntity.getExtends() );
		}

		if ( jaxbEntity.getTable() != null ) {
			XmlAnnotationHelper.applyTable( jaxbEntity.getTable(), classDetails, persistenceUnitMetadata );
		}

		final JaxbAttributesContainerImpl attributes = jaxbEntity.getAttributes();
		processIdMappings(
				attributes,
				classAccessType,
				classDetails,
				memberAdjuster,
				sourceModelBuildingContext
		);
		AttributeProcessor.processNaturalId(
				attributes.getNaturalId(),
				classDetails,
				classAccessType,
				memberAdjuster,
				sourceModelBuildingContext
		);
		AttributeProcessor.processAttributes(
				attributes,
				classDetails,
				classAccessType,
				memberAdjuster,
				sourceModelBuildingContext
		);

		jaxbEntity.getFilters().forEach( jaxbFilter -> XmlAnnotationHelper.applyFilter(
				jaxbFilter,
				classDetails,
				sourceModelBuildingContext
		) );

		XmlAnnotationHelper.applySqlRestriction( jaxbEntity.getSqlRestriction(), classDetails, sourceModelBuildingContext );

		processEntityOrMappedSuperclass( jaxbEntity, classDetails, sourceModelBuildingContext );

		XmlAnnotationHelper.applyRowId( jaxbEntity.getRowid(), classDetails, sourceModelBuildingContext );

		// todo : secondary-tables
	}


	private static void adjustNonDynamicTypeMember(
			MutableMemberDetails memberDetails,
			JaxbPersistentAttribute jaxbAttribute,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlAnnotationHelper.applyAttributeAccessor(
				jaxbAttribute.getAttributeAccessor(),
				memberDetails,
				sourceModelBuildingContext
		);
	}

	public static void processOverrideEntity(
			List<XmlProcessingResult.OverrideTuple<JaxbEntityImpl>> entityOverrides,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		entityOverrides.forEach( (overrideTuple) -> {
			final JaxbEntityMappingsImpl jaxbRoot = overrideTuple.getJaxbRoot();
			final JaxbEntityImpl jaxbEntity = overrideTuple.getManagedType();
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEntity );
			final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( className );

			final AccessType classAccessType = coalesce(
					jaxbEntity.getAccess(),
					persistenceUnitMetadata.getAccessType()
			);

			// from here, processing is the same between override and metadata-complete modes
			processEntityMetadata(
					classDetails,
					jaxbEntity,
					classAccessType,
					ManagedTypeProcessor::adjustNonDynamicTypeMember,
					persistenceUnitMetadata,
					sourceModelBuildingContext
			);
		} );

	}

	private static void processIdMappings(
			JaxbAttributesContainerImpl attributes,
			AccessType classAccessType,
			MutableClassDetails classDetails,
			AttributeProcessor.MemberAdjuster memberAdjuster,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final List<JaxbIdImpl> jaxbIds = attributes.getIdAttributes();
		final JaxbEmbeddedIdImpl jaxbEmbeddedId = attributes.getEmbeddedIdAttribute();

		if ( CollectionHelper.isNotEmpty( jaxbIds ) ) {
			for ( int i = 0; i < jaxbIds.size(); i++ ) {
				final JaxbIdImpl jaxbId = jaxbIds.get( i );
				final MutableMemberDetails memberDetails = AttributeProcessor.processBasicIdAttribute(
						jaxbId,
						classDetails,
						classAccessType,
						sourceModelBuildingContext
				);
				memberAdjuster.adjust( memberDetails, jaxbId, sourceModelBuildingContext );
			}
		}
		else if ( jaxbEmbeddedId != null ) {
			final MutableMemberDetails memberDetails = AttributeProcessor.processEmbeddedIdAttribute(
					jaxbEmbeddedId,
					classDetails,
					classAccessType,
					sourceModelBuildingContext
			);
			memberAdjuster.adjust( memberDetails, jaxbEmbeddedId, sourceModelBuildingContext );
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
			JaxbEntityMappingsImpl jaxbRoot,
			JaxbMappedSuperclassImpl jaxbMappedSuperclass,
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
			JaxbMappedSuperclassImpl jaxbMappedSuperclass,
			MutableClassDetails classDetails,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlProcessingHelper.getOrMakeAnnotation( MappedSuperclass.class, classDetails );

		final AccessType classAccessType = coalesce(
				jaxbMappedSuperclass.getAccess(),
				persistenceUnitMetadata.getAccessType()
		);
		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails ) );

		final JaxbAttributesContainer attributes = jaxbMappedSuperclass.getAttributes();
		AttributeProcessor.processAttributes( attributes, classDetails, classAccessType, sourceModelBuildingContext );

		processEntityOrMappedSuperclass( jaxbMappedSuperclass, classDetails, sourceModelBuildingContext );
	}

	public static void processOverrideMappedSuperclass(
			List<XmlProcessingResult.OverrideTuple<JaxbMappedSuperclassImpl>> mappedSuperclassesOverrides,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		mappedSuperclassesOverrides.forEach( (overrideTuple) -> {
			final JaxbEntityMappingsImpl jaxbRoot = overrideTuple.getJaxbRoot();
			final JaxbMappedSuperclassImpl jaxbMappedSuperclass = overrideTuple.getManagedType();
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbMappedSuperclass );
			final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( className );

			processMappedSuperclassMetadata( jaxbMappedSuperclass, classDetails, persistenceUnitMetadata, sourceModelBuildingContext );
		} );
	}

	private static void processEntityOrMappedSuperclass(
			JaxbEntityOrMappedSuperclass jaxbEntity,
			MutableClassDetails classDetails,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlAnnotationHelper.applyIdClass( jaxbEntity.getIdClass(), classDetails, sourceModelBuildingContext );

		XmlAnnotationHelper.applyLifecycleCallbacks( jaxbEntity, classDetails, sourceModelBuildingContext );

		if ( jaxbEntity.getEntityListeners() != null ) {
			jaxbEntity.getEntityListeners().getEntityListener().forEach( ( jaxbEntityListener -> {
				XmlAnnotationHelper.applyEntityListener( jaxbEntityListener, classDetails, sourceModelBuildingContext );
			} ) );
		}
	}

	public static void processCompleteEmbeddable(
			JaxbEntityMappingsImpl jaxbRoot,
			JaxbEmbeddableImpl jaxbEmbeddable,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final MutableClassDetails classDetails;
		final AccessType classAccessType;
		final AttributeProcessor.MemberAdjuster memberAdjuster;

		if ( StringHelper.isEmpty( jaxbEmbeddable.getClazz() ) ) {
			if ( StringHelper.isEmpty( jaxbEmbeddable.getName() ) ) {
				throw new ModelsException( "Embeddable did not define class nor name" );
			}
			// no class == dynamic...
			classDetails = (MutableClassDetails) sourceModelBuildingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( jaxbEmbeddable.getName(), DynamicClassDetails::new );
			classAccessType = AccessType.FIELD;
			memberAdjuster = ManagedTypeProcessor::adjustDynamicTypeMember;

			prepareDynamicClass( classDetails, jaxbEmbeddable, persistenceUnitMetadata, sourceModelBuildingContext );
		}
		else {
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEmbeddable );
			classDetails = (MutableClassDetails) sourceModelBuildingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( className );
			classAccessType = coalesce(
					jaxbEmbeddable.getAccess(),
					persistenceUnitMetadata.getAccessType()
			);
			memberAdjuster = ManagedTypeProcessor::adjustNonDynamicTypeMember;
		}

		classDetails.clearMemberAnnotationUsages();
		classDetails.clearAnnotationUsages();

		processEmbeddableMetadata(
				jaxbEmbeddable,
				classDetails,
				classAccessType,
				memberAdjuster,
				persistenceUnitMetadata,
				sourceModelBuildingContext
		);
	}

	private static void processEmbeddableMetadata(
			JaxbEmbeddableImpl jaxbEmbeddable,
			MutableClassDetails classDetails,
			AccessType classAccessType,
			AttributeProcessor.MemberAdjuster memberAdjuster,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		XmlProcessingHelper.getOrMakeAnnotation( Embeddable.class, classDetails );
		classDetails.addAnnotationUsage( XmlAnnotationHelper.createAccessAnnotation( classAccessType, classDetails ) );

		AttributeProcessor.processAttributes(
				jaxbEmbeddable.getAttributes(),
				classDetails,
				AccessType.FIELD,
				memberAdjuster,
				sourceModelBuildingContext
		);
	}

	public static void processOverrideEmbeddable(
			List<XmlProcessingResult.OverrideTuple<JaxbEmbeddableImpl>> embeddableOverrides,
			PersistenceUnitMetadata persistenceUnitMetadata,
			SourceModelBuildingContext sourceModelBuildingContext) {
		embeddableOverrides.forEach( (overrideTuple) -> {
			final JaxbEntityMappingsImpl jaxbRoot = overrideTuple.getJaxbRoot();
			final JaxbEmbeddableImpl jaxbEmbeddable = overrideTuple.getManagedType();
			final String className = XmlProcessingHelper.determineClassName( jaxbRoot, jaxbEmbeddable );
			final MutableClassDetails classDetails = (MutableClassDetails) sourceModelBuildingContext
					.getClassDetailsRegistry()
					.resolveClassDetails( className );

			AttributeProcessor.processAttributes(
					jaxbEmbeddable.getAttributes(),
					classDetails,
					AccessType.FIELD,
					ManagedTypeProcessor::adjustNonDynamicTypeMember,
					sourceModelBuildingContext
			);
		} );
	}
}
