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
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.NamedConsumer;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.categorize.spi.FilterDefRegistration;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class BindingStateImpl implements BindingState {
	private final MetadataBuildingContext metadataBuildingContext;
	private final InFlightMetadataCollector metadataCollector;

	private Map<String, PhysicalTable> physicalTableMap;
	private Map<String, InLineView> virtualTableBindingMap;

	public BindingStateImpl(MetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.metadataCollector = metadataBuildingContext.getMetadataCollector();
	}

	public MetadataBuildingContext getMetadataBuildingContext() {
		return metadataBuildingContext;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// PhysicalTable

	@Override
	public int getPhysicalTableCount() {
		return physicalTableMap == null ? 0 : physicalTableMap.size();
	}

	@Override
	public void forEachPhysicalTable(NamedConsumer<PhysicalTable> consumer) {
		if ( physicalTableMap != null ) {
			//noinspection unchecked
			physicalTableMap.forEach( (BiConsumer<? super String, ? super PhysicalTable>) consumer );
		}
	}

	@Override
	public PhysicalTable getPhysicalTableByName(String name) {
		if ( physicalTableMap == null ) {
			return null;
		}
		return physicalTableMap.get( name );
	}

	@Override
	public PhysicalTable getPhysicalTableByPhysicalName(String name) {
		if ( physicalTableMap != null ) {
			for ( Map.Entry<String, PhysicalTable> entry : physicalTableMap.entrySet() ) {
				if ( entry.getValue().physicalName().matches( name ) ) {
					return entry.getValue();
				}
			}
		}
		return null;
	}

	@Override
	public void addPhysicalTable(PhysicalTable physicalTable) {
		if ( physicalTableMap == null ) {
			physicalTableMap = new HashMap<>();
		}
		physicalTableMap.put( physicalTable.logicalName().render(), physicalTable );

		final Table addedTable = metadataBuildingContext.getMetadataCollector().addTable(
				resolveSchemaName( physicalTable.schema() ),
				resolveCatalogName( physicalTable.catalog() ),
				physicalTable.logicalName().render(),
				null,
				!physicalTable.isExportable(),
				metadataBuildingContext
		);
		addedTable.setComment( physicalTable.comment() );
	}

	private String resolveSchemaName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.render();
		}

		final Namespace defaultNamespace = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getDefaultNamespace();
		if ( defaultNamespace != null ) {
			final Identifier defaultSchemaName = defaultNamespace.getName().getSchema();
			if ( defaultSchemaName != null ) {
				return defaultSchemaName.render();
			}
		}
		return null;

	}

	private String resolveCatalogName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.render();
		}

		final Namespace defaultNamespace = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getDefaultNamespace();
		if ( defaultNamespace != null ) {
			final Identifier defaultCatalogName = defaultNamespace.getName().getCatalog();
			if ( defaultCatalogName != null ) {
				return defaultCatalogName.render();
			}
		}
		return null;

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// VirtualTableBinding

	@Override
	public int getNumberOfVirtualTableBindings() {
		return virtualTableBindingMap == null ? 0 : virtualTableBindingMap.size();
	}

	@Override
	public void forEachVirtualTableBinding(NamedConsumer<InLineView> consumer) {
		if ( virtualTableBindingMap != null ) {
			//noinspection unchecked
			virtualTableBindingMap.forEach( (BiConsumer<? super String, ? super InLineView>) consumer );
		}
	}

	@Override
	public InLineView getVirtualTableBindingByName(String name) {
		if ( virtualTableBindingMap == null ) {
			return null;
		}
		return virtualTableBindingMap.get( name );
	}


	@Override
	public void addVirtualTableBinding(InLineView binding) {
		if ( virtualTableBindingMap == null ) {
			virtualTableBindingMap = new HashMap<>();
		}
		virtualTableBindingMap.put( binding.logicalName().getCanonicalName(), binding );
	}


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
