/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.NamedConsumer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.models.ModelsException;
import org.hibernate.models.orm.bind.internal.binders.EntityTypeBinder;
import org.hibernate.models.orm.bind.internal.binders.IdentifiableTypeBinder;
import org.hibernate.models.orm.bind.internal.binders.ManagedTypeBinder;
import org.hibernate.models.orm.bind.internal.binders.MappedSuperTypeBinder;
import org.hibernate.models.orm.bind.internal.binders.TableBinder;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.bind.spi.TableReference;
import org.hibernate.models.orm.categorize.spi.FilterDefRegistration;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ManagedTypeMetadata;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class BindingStateImpl implements BindingState {
	private final MetadataBuildingContext metadataBuildingContext;

	private final Map<String, TableReference> tableMap = new HashMap<>();

	private final Map<ClassDetails, ManagedTypeBinder> typeBinders = new HashMap<>();
	private final Map<ClassDetails, IdentifiableTypeBinder> typeBindersBySuper = new HashMap<>();

	private List<TableBinder.TableSecondPass> tableSecondPasses;

	public BindingStateImpl(MetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
	}

	public void processSecondPasses() {
		processSecondPasses( tableSecondPasses );
	}

	private void processSecondPasses(List<? extends SecondPass> secondPasses) {
		int processedCount = 0;
		final Iterator<? extends SecondPass> secondPassItr = secondPasses.iterator();
		while ( secondPassItr.hasNext() ) {
			final SecondPass secondPass = secondPassItr.next();
			try {
				final boolean success = secondPass.process();
				if ( success ) {
					processedCount++;
					secondPassItr.remove();
				}
			}
			catch (Exception ignoreForNow) {
			}
		}

		if ( !secondPasses.isEmpty() ) {
			if ( processedCount == 0 ) {
				// there are second-passes in the queue, but we were not able to
				// successfully process any of them.  this is a non-changing
				// error condition - just throw an exception
				throw new ModelsException( "Unable to process second-pass list" );
			}
		}
	}

	@Override
	public MetadataBuildingContext getMetadataBuildingContext() {
		return metadataBuildingContext;
	}

	@Override
	public void registerTypeBinder(ManagedTypeMetadata type, ManagedTypeBinder binder) {
		typeBinders.put( type.getClassDetails(), binder );

		if ( type instanceof IdentifiableTypeMetadata identifiableType ) {
			if ( identifiableType.getSuperType() != null ) {
				typeBindersBySuper.put(
						identifiableType.getSuperType().getClassDetails(),
						(IdentifiableTypeBinder) binder
				);
			}
		}

		if ( binder instanceof EntityTypeBinder entityTypeBinder ) {
			metadataBuildingContext.getMetadataCollector().addEntityBinding( entityTypeBinder.getTypeBinding() );
		}
		else if ( binder instanceof MappedSuperTypeBinder mappedSuperBinder ) {
			metadataBuildingContext.getMetadataCollector().addMappedSuperclass(
					mappedSuperBinder.getManagedType().getClassDetails().toJavaClass(),
					mappedSuperBinder.getTypeBinding()
			);
		}
	}

	@Override
	public ManagedTypeBinder getTypeBinder(ClassDetails type) {
		return typeBinders.get( type );
	}

	@Override
	public IdentifiableTypeBinder getSuperTypeBinder(ClassDetails type) {
		return typeBindersBySuper.get( type );
	}

	@Override
	public void forEachType(NamedConsumer<ManagedTypeBinder> consumer) {
		typeBinders.forEach( (classDetails, managedTypeBinder) -> consumer.consume( classDetails.getName(), managedTypeBinder ) );
	}

	@Override
	public int getTableCount() {
		return tableMap.size();
	}

	@Override
	public void forEachTable(NamedConsumer<TableReference> consumer) {
		//noinspection unchecked
		tableMap.forEach( (BiConsumer<? super String, ? super TableReference>) consumer );
	}

	@Override
	public <T extends TableReference> T getTableByName(String name) {
		//noinspection unchecked
		return (T) tableMap.get( name );
	}

	@Override
	public void addTable(TableReference table) {
		tableMap.put( table.getLogicalName().getCanonicalName(), table );
	}

	@Override
	public void registerTableSecondPass(TableBinder.TableSecondPass secondPass) {
		if ( tableSecondPasses == null ) {
			tableSecondPasses = new ArrayList<>();
		}
		tableSecondPasses.add( secondPass );
	}

	private String resolveSchemaName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.getCanonicalName();
		}

		var defaultNamespace = metadataBuildingContext.getMetadataCollector()
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

		var defaultNamespace = metadataBuildingContext.getMetadataCollector()
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
