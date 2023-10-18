/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import java.util.Set;

import org.hibernate.boot.jaxb.mapping.JaxbEntityListeners;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.JaxbPersistenceUnitDefaults;
import org.hibernate.boot.jaxb.mapping.JaxbPersistenceUnitMetadata;
import org.hibernate.models.orm.spi.ProcessResult;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
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
	private final GlobalRegistrations globalRegistrations;
	private final boolean areIdGeneratorsGlobal;

	public ProcessResultCollector(boolean areIdGeneratorsGlobal, SourceModelBuildingContext sourceModelBuildingContext) {
		this.globalRegistrations = new GlobalRegistrations( sourceModelBuildingContext );
		this.areIdGeneratorsGlobal = areIdGeneratorsGlobal;
	}

	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}


	public void apply(JaxbEntityMappings jaxbRoot) {
		getGlobalRegistrations().collectJavaTypeRegistrations( jaxbRoot.getJavaTypeRegistrations() );
		getGlobalRegistrations().collectJdbcTypeRegistrations( jaxbRoot.getJdbcTypeRegistrations() );
		getGlobalRegistrations().collectConverterRegistrations( jaxbRoot.getConverterRegistrations() );
		getGlobalRegistrations().collectUserTypeRegistrations( jaxbRoot.getUserTypeRegistrations() );
		getGlobalRegistrations().collectCompositeUserTypeRegistrations( jaxbRoot.getCompositeUserTypeRegistrations() );
		getGlobalRegistrations().collectCollectionTypeRegistrations( jaxbRoot.getCollectionUserTypeRegistrations() );
		getGlobalRegistrations().collectEmbeddableInstantiatorRegistrations( jaxbRoot.getEmbeddableInstantiatorRegistrations() );

		final JaxbPersistenceUnitMetadata persistenceUnitMetadata = jaxbRoot.getPersistenceUnitMetadata();
		if ( persistenceUnitMetadata != null ) {
			final JaxbPersistenceUnitDefaults persistenceUnitDefaults = persistenceUnitMetadata.getPersistenceUnitDefaults();
			final JaxbEntityListeners entityListeners = persistenceUnitDefaults.getEntityListeners();
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

		// todo : others?
	}

	/**
	 * Builder for {@linkplain ProcessResult} based on our internal state plus
	 * the incoming set of entity hierarchies.
	 *
	 * @param entityHierarchies Hierarchies to be {@linkplain ProcessResult#getEntityHierarchies() included}
	 * in the result
	 *
	 * @see org.hibernate.models.orm.spi.Processor#process
	 */
	public ProcessResult createResult(Set<EntityHierarchy> entityHierarchies) {
		return new ProcessResultImpl(
				entityHierarchies,
				getGlobalRegistrations().getJavaTypeRegistrations(),
				getGlobalRegistrations().getJdbcTypeRegistrations(),
				getGlobalRegistrations().getConverterRegistrations(),
				getGlobalRegistrations().getAutoAppliedConverters(),
				getGlobalRegistrations().getUserTypeRegistrations(),
				getGlobalRegistrations().getCompositeUserTypeRegistrations(),
				getGlobalRegistrations().getCollectionTypeRegistrations(),
				getGlobalRegistrations().getEmbeddableInstantiatorRegistrations(),
//				jpaNamedQueries == null ? emptyMap() : jpaNamedQueries,
//				hibernateNamedHqlQueries == null ? emptyMap() : hibernateNamedHqlQueries,
//				hibernateNamedNativeQueries == null ? emptyMap() : hibernateNamedNativeQueries
				emptyMap(),
				emptyMap(),
				emptyMap()
		);
	}
}
