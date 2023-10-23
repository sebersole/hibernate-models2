/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import java.util.Map;
import java.util.Set;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenersImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadataImpl;
import org.hibernate.models.orm.spi.ProcessResult;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.PackageDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import static java.util.Collections.emptyMap;

/**
 * In-flight holder for various types of "global" registrations.  Also acts as the
 * {@linkplain #createResult builder} for {@linkplain ProcessResult} as returned
 * by {@linkplain org.hibernate.models.orm.spi.Processor#process}
 *
 * @author Steve Ebersole
 */
public class ProcessResultCollector {
	private final GlobalRegistrationsImpl globalRegistrations;
	private final boolean areIdGeneratorsGlobal;

	public ProcessResultCollector(boolean areIdGeneratorsGlobal, SourceModelBuildingContext sourceModelBuildingContext) {
		this.globalRegistrations = new GlobalRegistrationsImpl( sourceModelBuildingContext );
		this.areIdGeneratorsGlobal = areIdGeneratorsGlobal;
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
	 * Builder for {@linkplain ProcessResult} based on our internal state plus
	 * the incoming set of managed types.
	 *
	 * @param entityHierarchies All entity hierarchies defined in the persistence-unit
	 * @param mappedSuperclasses All mapped-superclasses defined in the persistence-unit
	 * @param embeddables All embeddables defined in the persistence-unit
	 *
	 * @see org.hibernate.models.orm.spi.Processor#process
	 */
	public ProcessResult createResult(
			Set<EntityHierarchy> entityHierarchies,
			Map<String, ClassDetails> mappedSuperclasses,
			Map<String, ClassDetails> embeddables) {
		return new ProcessResultImpl(
				entityHierarchies,
				mappedSuperclasses,
				embeddables,
				getGlobalRegistrations()
		);
	}
}
