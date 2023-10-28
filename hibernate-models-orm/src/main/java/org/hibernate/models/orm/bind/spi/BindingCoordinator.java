/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import org.hibernate.models.orm.bind.internal.HierarchyMetadataProcessor;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;

/**
 * Responsible for processing {@linkplain org.hibernate.boot.model.process.spi.ManagedResources managed-resources}
 * and binding them into Hibernate's {@linkplain org.hibernate.mapping boot-time model}.
 *
 * @author Steve Ebersole
 */
public class BindingCoordinator {
	/**
	 * Main entry point into this binding coordination
	 *
	 * @param categorizedDomainModel The model to be processed
	 * @param options Options for the binding
	 * @param bindingContext Access to needed information and delegates
	 */
	public static void coordinateBinding(
			CategorizedDomainModel categorizedDomainModel,
			BindingOptions options,
			BindingContext bindingContext) {
		HierarchyMetadataProcessor.preBindHierarchyAttributes( categorizedDomainModel, bindingContext );
//		Processor.process( managedResources, bindingContext, options );
	}
}
