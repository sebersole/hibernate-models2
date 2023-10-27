/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenersImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadataImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.models.orm.spi.CategorizedDomainModel;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.PackageDetails;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;

/**
 * In-flight holder for various types of "global" registrations.  Also acts as the
 * {@linkplain #createResult builder} for {@linkplain CategorizedDomainModel} as returned
 * by {@linkplain org.hibernate.models.orm.spi.ManagedResourcesProcessor#processManagedResources}
 *
 * @author Steve Ebersole
 */

public class DomainModelCategorizationCollector {
	private final boolean areIdGeneratorsGlobal;
	private final Set<ClassDetails> rootEntities = new HashSet<>();
	private final Map<String,ClassDetails> mappedSuperclasses = new HashMap<>();
	private final Map<String,ClassDetails> embeddables = new HashMap<>();
	private final GlobalRegistrationsImpl globalRegistrations;

	public DomainModelCategorizationCollector(boolean areIdGeneratorsGlobal, ClassDetailsRegistry classDetailsRegistry) {
		this.areIdGeneratorsGlobal = areIdGeneratorsGlobal;
		this.globalRegistrations = new GlobalRegistrationsImpl( classDetailsRegistry );
	}

	public Set<ClassDetails> getRootEntities() {
		return rootEntities;
	}

	public Map<String, ClassDetails> getMappedSuperclasses() {
		return mappedSuperclasses;
	}

	public Map<String, ClassDetails> getEmbeddables() {
		return embeddables;
	}

	public GlobalRegistrationsImpl getGlobalRegistrations() {
		return globalRegistrations;
	}


	public void apply(JaxbEntityMappingsImpl jaxbRoot) {
		getGlobalRegistrations().collectJavaTypeRegistrations( jaxbRoot.getJavaTypeRegistrations() );
		getGlobalRegistrations().collectJdbcTypeRegistrations( jaxbRoot.getJdbcTypeRegistrations() );
		getGlobalRegistrations().collectConverterRegistrations( jaxbRoot.getConverterRegistrations() );
		getGlobalRegistrations().collectUserTypeRegistrations( jaxbRoot.getUserTypeRegistrations() );
		getGlobalRegistrations().collectCompositeUserTypeRegistrations( jaxbRoot.getCompositeUserTypeRegistrations() );
		getGlobalRegistrations().collectCollectionTypeRegistrations( jaxbRoot.getCollectionUserTypeRegistrations() );
		getGlobalRegistrations().collectEmbeddableInstantiatorRegistrations( jaxbRoot.getEmbeddableInstantiatorRegistrations() );
		getGlobalRegistrations().collectFilterDefinitions( jaxbRoot.getFilterDefinitions() );

		final JaxbPersistenceUnitMetadataImpl persistenceUnitMetadata = jaxbRoot.getPersistenceUnitMetadata();
		if ( persistenceUnitMetadata != null ) {
			final JaxbPersistenceUnitDefaultsImpl persistenceUnitDefaults = persistenceUnitMetadata.getPersistenceUnitDefaults();
			final JaxbEntityListenersImpl entityListeners = persistenceUnitDefaults.getEntityListeners();
			if ( entityListeners != null ) {
				getGlobalRegistrations().collectEntityListenerRegistrations( entityListeners.getEntityListener() );
			}
		}

		getGlobalRegistrations().collectIdGenerators( jaxbRoot );

		// todo : named queries
		// todo : named graphs
	}

	public void apply(ClassDetails classDetails) {
		getGlobalRegistrations().collectJavaTypeRegistrations( classDetails );
		getGlobalRegistrations().collectJdbcTypeRegistrations( classDetails );
		getGlobalRegistrations().collectConverterRegistrations( classDetails );
		getGlobalRegistrations().collectUserTypeRegistrations( classDetails );
		getGlobalRegistrations().collectCompositeUserTypeRegistrations( classDetails );
		getGlobalRegistrations().collectCollectionTypeRegistrations( classDetails );
		getGlobalRegistrations().collectEmbeddableInstantiatorRegistrations( classDetails );
		getGlobalRegistrations().collectFilterDefinitions( classDetails );

		if ( areIdGeneratorsGlobal ) {
			getGlobalRegistrations().collectIdGenerators( classDetails );
		}

		// todo : named queries
		// todo : named graphs

		if ( classDetails.getAnnotationUsage( MappedSuperclass.class ) != null ) {
			if ( classDetails.getClassName() != null ) {
				mappedSuperclasses.put( classDetails.getClassName(), classDetails );
			}
		}
		else if ( classDetails.getAnnotationUsage( Entity.class ) != null ) {
			if ( EntityHierarchyBuilder.isRoot( classDetails ) ) {
				rootEntities.add( classDetails );
			}
		}
		else if ( classDetails.getAnnotationUsage( Embeddable.class ) != null ) {
			if ( classDetails.getClassName() != null ) {
				embeddables.put( classDetails.getClassName(), classDetails );
			}
		}

		// todo : converters?  - @Converter / AttributeConverter, as opposed to @ConverterRegistration which is already collected
	}

	public void apply(PackageDetails packageDetails) {
		getGlobalRegistrations().collectJavaTypeRegistrations( packageDetails );
		getGlobalRegistrations().collectJdbcTypeRegistrations( packageDetails );
		getGlobalRegistrations().collectConverterRegistrations( packageDetails );
		getGlobalRegistrations().collectUserTypeRegistrations( packageDetails );
		getGlobalRegistrations().collectCompositeUserTypeRegistrations( packageDetails );
		getGlobalRegistrations().collectCollectionTypeRegistrations( packageDetails );
		getGlobalRegistrations().collectEmbeddableInstantiatorRegistrations( packageDetails );
		getGlobalRegistrations().collectFilterDefinitions( packageDetails );

		// todo : others?
	}

	/**
	 * Builder for {@linkplain CategorizedDomainModel} based on our internal state plus
	 * the incoming set of managed types.
	 *
	 * @param entityHierarchies All entity hierarchies defined in the persistence-unit, built based
	 * on {@linkplain #getRootEntities()}
	 *
	 * @see org.hibernate.models.orm.spi.ManagedResourcesProcessor#processManagedResources
	 */
	public CategorizedDomainModel createResult(
			Set<EntityHierarchy> entityHierarchies,
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry annotationDescriptorRegistry) {
		return new CategorizedDomainModelImpl(
				classDetailsRegistry,
				annotationDescriptorRegistry,
				entityHierarchies,
				mappedSuperclasses,
				embeddables,
				getGlobalRegistrations()
		);
	}
}
