/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.models.bind.internal.BindingContextImpl;
import org.hibernate.boot.models.bind.internal.BindingOptionsImpl;
import org.hibernate.boot.models.bind.internal.BindingStateImpl;
import org.hibernate.boot.models.bind.spi.BindingCoordinator;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor;

/**
 * @author Steve Ebersole
 */
public class BindingTestingHelper {
	static void checkDomainModel(
			DomainModelCheck check,
			StandardServiceRegistry serviceRegistry,
			Class<?>... domainClasses) {
		final BootstrapContextImpl bootstrapContext = buildBootstrapContext(
				serviceRegistry );
		final ManagedResources managedResources = buildManagedResources(
				domainClasses,
				bootstrapContext
		);

		final InFlightMetadataCollectorImpl metadataCollector = new InFlightMetadataCollectorImpl(
				bootstrapContext,
				bootstrapContext.getMetadataBuildingOptions()
		);

		final CategorizedDomainModel categorizedDomainModel = ManagedResourcesProcessor.processManagedResources(
				managedResources,
				bootstrapContext
		);

		final MetadataBuildingContextRootImpl metadataBuildingContext = new MetadataBuildingContextRootImpl(
				"models",
				bootstrapContext,
				bootstrapContext.getMetadataBuildingOptions(),
				metadataCollector
		);
		final BindingStateImpl bindingState = new BindingStateImpl( metadataBuildingContext );
		final BindingOptionsImpl bindingOptions = new BindingOptionsImpl( metadataBuildingContext );
		final BindingContextImpl bindingContext = new BindingContextImpl(
				categorizedDomainModel,
				bootstrapContext
		);

		BindingCoordinator.coordinateBinding(
				categorizedDomainModel,
				bindingState,
				bindingOptions,
				bindingContext
		);

		check.checkDomainModel( new DomainModelCheckContext() {
			@Override
			public InFlightMetadataCollectorImpl getMetadataCollector() {
				return metadataCollector;
			}

			@Override
			public BindingStateImpl getBindingState() {
				return bindingState;
			}
		} );
	}

	interface DomainModelCheckContext {
		InFlightMetadataCollectorImpl getMetadataCollector();
		BindingStateImpl getBindingState();
	}

	@FunctionalInterface
	interface DomainModelCheck {
		void checkDomainModel(DomainModelCheckContext context);
	}

	private static BootstrapContextImpl buildBootstrapContext(StandardServiceRegistry serviceRegistry) {
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );
		return bootstrapContext;
	}

	private static ManagedResources buildManagedResources(
			Class<?>[] domainClasses,
			BootstrapContextImpl bootstrapContext) {
		final MetadataSources metadataSources = new MetadataSources( bootstrapContext.getServiceRegistry() );
		for ( int i = 0; i < domainClasses.length; i++ ) {
			metadataSources.addAnnotatedClass( domainClasses[i] );
		}
		return MetadataBuildingProcess.prepare( metadataSources, bootstrapContext );
	}
}
