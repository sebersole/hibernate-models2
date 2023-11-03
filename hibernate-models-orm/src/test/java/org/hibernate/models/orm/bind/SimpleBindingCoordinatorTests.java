/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import java.util.Iterator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.models.orm.bind.internal.BindingContextImpl;
import org.hibernate.models.orm.bind.internal.BindingOptionsImpl;
import org.hibernate.models.orm.bind.internal.BindingStateImpl;
import org.hibernate.models.orm.bind.internal.PhysicalTable;
import org.hibernate.models.orm.bind.spi.BindingCoordinator;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.ManagedResourcesProcessor;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleBindingCoordinatorTests {
	@Test
	@ServiceRegistry
	void testIt(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final BindingStateImpl bindingState = context.getBindingState();
					final InFlightMetadataCollectorImpl metadataCollector = context.getMetadataCollector();

					final FilterDefinition filterDefinition = metadataCollector.getFilterDefinition( "by-name" );
					assertThat( filterDefinition ).isNotNull();
					assertThat( filterDefinition.getDefaultFilterCondition() ).isEqualTo( "name = :name" );
					assertThat( filterDefinition.getParameterNames() ).hasSize( 1 );
					final JdbcMapping nameParamJdbcMapping = filterDefinition.getParameterJdbcMapping( "name" );
					assertThat( nameParamJdbcMapping ).isNotNull();
					assertThat( nameParamJdbcMapping.getJdbcJavaType().getJavaType() ).isEqualTo( String.class );

					assertThat( bindingState.getPhysicalTableCount() ).isEqualTo( 2 );

					final PhysicalTable simpletonsTable = bindingState.getPhysicalTableByName( "simpletons" );
					assertThat( simpletonsTable.logicalName().render() ).isEqualTo( "simpletons" );
					assertThat( simpletonsTable.physicalName().render() ).isEqualTo( "simpletons" );
					assertThat( simpletonsTable.logicalName().getCanonicalName() ).isEqualTo( "simpletons" );
					assertThat( simpletonsTable.physicalName().getCanonicalName() ).isEqualTo( "simpletons" );
					assertThat( simpletonsTable.catalog() ).isNull();
					assertThat( simpletonsTable.schema() ).isNull();
					assertThat( simpletonsTable.comment() ).isEqualTo( "Stupid is as stupid does" );

					final PhysicalTable simpleStuffTable = bindingState.getPhysicalTableByName( "simple_stuff" );
					assertThat( simpleStuffTable.logicalName().render() ).isEqualTo( "simple_stuff" );
					assertThat( simpleStuffTable.physicalName().render() ).isEqualTo( "simple_stuff" );
					assertThat( simpleStuffTable.logicalName().getCanonicalName() ).isEqualTo( "simple_stuff" );
					assertThat( simpleStuffTable.physicalName().getCanonicalName() ).isEqualTo( "simple_stuff" );
					assertThat( simpleStuffTable.catalog() ).isEqualTo( Identifier.toIdentifier( "my_catalog" ) );
					assertThat( simpleStuffTable.schema() ).isEqualTo( Identifier.toIdentifier( "my_schema" ) );
					assertThat( simpleStuffTable.comment() ).isEqualTo( "Don't sweat it" );

					final Database database = metadataCollector.getDatabase();
					final Iterator<Namespace> namespaceItr = database.getNamespaces().iterator();
					final Namespace namespace1 = namespaceItr.next();
					final Namespace namespace2 = namespaceItr.next();
					assertThat( namespaceItr.hasNext() ).isFalse();
					assertThat( namespace1.getTables() ).hasSize( 1 );
					assertThat( namespace2.getTables() ).hasSize( 1 );
				},
				scope.getRegistry(),
				SimpleEntity.class
		);
	}

	interface DomainModelCheckContext {
		InFlightMetadataCollectorImpl getMetadataCollector();
		BindingStateImpl getBindingState();
	}

	@FunctionalInterface
	interface DomainModelCheck {
		void checkDomainModel(DomainModelCheckContext context);
	}

	private static void checkDomainModel(DomainModelCheck check, StandardServiceRegistry serviceRegistry, Class<?>... domainClasses) {
		final BootstrapContextImpl bootstrapContext = buildBootstrapContext( serviceRegistry );
		final ManagedResources managedResources = buildManagedResources( domainClasses, bootstrapContext );

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

	private static BootstrapContextImpl buildBootstrapContext(StandardServiceRegistry serviceRegistry) {
		final MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuildingOptionsImpl( serviceRegistry );
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
		final ManagedResources managedResources = MetadataBuildingProcess.prepare( metadataSources, bootstrapContext );
		return managedResources;
	}
}
