/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.NamedConsumer;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.bind.spi.TableReference;
import org.hibernate.models.orm.categorize.spi.FilterDefRegistration;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class BindingStateImpl implements BindingState {
	private final MetadataBuildingContext metadataBuildingContext;

	private Map<String, TableReference> tableMap;
	private Map<String, InLineView> virtualTableBindingMap;

	public BindingStateImpl(MetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
	}

	public MetadataBuildingContext getMetadataBuildingContext() {
		return metadataBuildingContext;
	}

	@Override
	public Database getDatabase() {
		return getMetadataBuildingContext().getMetadataCollector().getDatabase();
	}

	@Override
	public int getTableCount() {
		return tableMap == null ? 0 : tableMap.size();
	}

	@Override
	public void forEachTable(NamedConsumer<TableReference> consumer) {
		if ( tableMap != null ) {
			//noinspection unchecked
			tableMap.forEach( (BiConsumer<? super String, ? super TableReference>) consumer );
		}
	}

	@Override
	public <T extends TableReference> T getTableByName(String name) {
		if ( tableMap == null ) {
			return null;
		}
		//noinspection unchecked
		return (T) tableMap.get( name );
	}

	@Override
	public void addTable(TableReference table) {
		if ( tableMap == null ) {
			tableMap = new HashMap<>();
		}
		tableMap.put( table.getLogicalName().getCanonicalName(), table );

		if ( table instanceof PhysicalTable physicalTable ) {
			final Table addedTable = metadataBuildingContext.getMetadataCollector().addTable(
					resolveSchemaName( physicalTable.schema() ),
					resolveCatalogName( physicalTable.catalog() ),
					physicalTable.logicalName().getCanonicalName(),
					null,
					!table.isExportable(),
					metadataBuildingContext
			);
			addedTable.setComment( physicalTable.comment() );
		}
		else if ( table instanceof PhysicalView physicalView ) {
			final Table addedTable = metadataBuildingContext.getMetadataCollector().addTable(
					null,
					null,
					null,
					null,
					!physicalView.isExportable(),
					metadataBuildingContext
			);
			addedTable.setViewQuery( physicalView.query() );
		}
		else if ( table instanceof SecondaryTable secondaryTable ) {
			final Table addedTable = metadataBuildingContext.getMetadataCollector().addTable(
					resolveSchemaName( secondaryTable.schema() ),
					resolveCatalogName( secondaryTable.catalog() ),
					secondaryTable.logicalName().getCanonicalName(),
					null,
					!table.isExportable(),
					metadataBuildingContext
			);
			addedTable.setComment( secondaryTable.comment() );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// PhysicalTable

	private String resolveSchemaName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.getCanonicalName();
		}

		final Namespace defaultNamespace = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getDefaultNamespace();
		if ( defaultNamespace != null ) {
			final Identifier defaultSchemaName = defaultNamespace.getName().getSchema();
			if ( defaultSchemaName != null ) {
				return defaultSchemaName.getCanonicalName();
			}
		}
		return null;

	}

	private String resolveCatalogName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.getCanonicalName();
		}

		final Namespace defaultNamespace = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getDefaultNamespace();
		if ( defaultNamespace != null ) {
			final Identifier defaultCatalogName = defaultNamespace.getName().getCatalog();
			if ( defaultCatalogName != null ) {
				return defaultCatalogName.getCanonicalName();
			}
		}
		return null;

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// VirtualTableBinding


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Filter def

	@Override
	public void apply(FilterDefRegistration registration) {
		metadataBuildingContext.getMetadataCollector().addFilterDefinition( new FilterDefinition(
				registration.getName(),
				registration.getDefaultCondition(),
				extractParameterMap( registration )
		) );
	}

	private Map<String, JdbcMapping> extractParameterMap(FilterDefRegistration registration) {
		final Map<String, ClassDetails> parameters = registration.getParameters();
		if ( CollectionHelper.isEmpty( parameters ) ) {
			return Collections.emptyMap();
		}

		final TypeConfiguration typeConfiguration = metadataBuildingContext.getBootstrapContext().getTypeConfiguration();
		final Map<String, JdbcMapping> result = new HashMap<>();
		parameters.forEach( (name, typeDetails) -> {
			result.put( name, typeConfiguration.getBasicTypeForJavaType( typeDetails.toJavaClass() ) );
		} );
		return result;
	}
}
