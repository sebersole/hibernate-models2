/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.util.function.Supplier;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Table;
import org.hibernate.models.orm.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.source.spi.AnnotationUsage;

import static org.hibernate.internal.util.NullnessHelper.nullif;

/**
 * @author Steve Ebersole
 */
public class TableBinder {
	public static Table bindTable(
			AnnotationUsage<?> tableAnnotation,
			Supplier<String> implicitTableNameSupplier,
			boolean isAbstract,
			MetadataBuildingContext metadataBuildingContext,
			ModelCategorizationContext modelBuildingContext) {
		final InFlightMetadataCollector metadataCollector = metadataBuildingContext.getMetadataCollector();
		return metadataCollector.addTable(
				schemaName( tableAnnotation, metadataBuildingContext, modelBuildingContext ),
				catalogName( tableAnnotation, metadataBuildingContext, modelBuildingContext ),
				nullif( tableAnnotation.getString( "name" ), implicitTableNameSupplier ),
				null,
				isAbstract,
				metadataBuildingContext
		);
	}

	public static String catalogName(
			AnnotationUsage<?> tableAnnotation,
			MetadataBuildingContext metadataBuildingContext,
			ModelCategorizationContext modelBuildingContext) {
		if ( tableAnnotation != null ) {
			return tableAnnotation.getString( "catalog", metadataBuildingContext.getMappingDefaults().getImplicitCatalogName() );
		}

		return metadataBuildingContext.getMappingDefaults().getImplicitCatalogName();
	}

	public static String schemaName(
			AnnotationUsage<?> tableAnnotation,
			MetadataBuildingContext metadataBuildingContext,
			ModelCategorizationContext modelBuildingContext) {
		if ( tableAnnotation != null ) {
			return tableAnnotation.getString( "schema", metadataBuildingContext.getMappingDefaults().getImplicitSchemaName() );
		}
		return metadataBuildingContext.getMappingDefaults().getImplicitSchemaName();
	}

	private TableBinder() {
	}
}
