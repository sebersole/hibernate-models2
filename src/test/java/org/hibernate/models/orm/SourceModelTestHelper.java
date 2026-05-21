/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ModelsContext;

/**
 * @author Steve Ebersole
 */
public class SourceModelTestHelper {
	public static ModelsContext createBuildingContext(Class<?>... modelClasses) {
		final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();
		final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );
		metadataBuildingOptions.setBootstrapContext( bootstrapContext );

		final ModelsContext modelsContext = bootstrapContext.getModelsContext();
		for ( Class<?> modelClass : modelClasses ) {
			modelsContext.getClassDetailsRegistry().resolveClassDetails( modelClass.getName() );
		}
		return modelsContext;
	}
}
