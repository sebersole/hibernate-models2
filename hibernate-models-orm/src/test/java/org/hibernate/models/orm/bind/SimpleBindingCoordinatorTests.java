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
import org.hibernate.boot.internal.MetadataBuilderImpl.MetadataBuildingOptionsImpl;
import org.hibernate.boot.internal.MetadataBuildingContextRootImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.orm.bind.internal.BindingContextImpl;
import org.hibernate.models.orm.bind.internal.BindingOptionsImpl;
import org.hibernate.models.orm.bind.internal.BindingStateImpl;
import org.hibernate.models.orm.bind.internal.PhysicalTable;
import org.hibernate.models.orm.bind.internal.SecondaryTable;
import org.hibernate.models.orm.bind.spi.BindingCoordinator;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.ManagedResourcesProcessor;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleBindingCoordinatorTests {
	@Test
	@ServiceRegistry( settingProviders = @SettingProvider(
			settingName = AvailableSettings.PHYSICAL_NAMING_STRATEGY,
			provider = CustomNamingStrategyProvider.class
	) )
	void testIt(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final var bindingState = context.getBindingState();
					final var metadataCollector = context.getMetadataCollector();

					final var filterDefinition = metadataCollector.getFilterDefinition( "by-name" );
					assertThat( filterDefinition ).isNotNull();
					assertThat( filterDefinition.getDefaultFilterCondition() ).isEqualTo( "name = :name" );
					assertThat( filterDefinition.getParameterNames() ).hasSize( 1 );
					final var nameParamJdbcMapping = filterDefinition.getParameterJdbcMapping( "name" );
					assertThat( nameParamJdbcMapping ).isNotNull();
					assertThat( nameParamJdbcMapping.getJdbcJavaType().getJavaType() ).isEqualTo( String.class );

					assertThat( bindingState.getTableCount() ).isEqualTo( 2 );

					final PhysicalTable simpletonsTable = bindingState.getTableByName( "simpletons" );
					assertThat( simpletonsTable.logicalName().render() ).isEqualTo( "simpletons" );
					assertThat( simpletonsTable.logicalName().getCanonicalName() ).isEqualTo( "simpletons" );
					assertThat( simpletonsTable.physicalTableName().render() ).isEqualTo( "SIMPLETONS" );
					assertThat( simpletonsTable.physicalTableName().getCanonicalName() ).isEqualTo( "simpletons" );
					assertThat( simpletonsTable.physicalCatalogName() ).isNull();
					assertThat( simpletonsTable.getPhysicalSchemaName() ).isNull();
					assertThat( simpletonsTable.binding().getComment() ).isEqualTo( "Stupid is as stupid does" );

					final SecondaryTable simpleStuffTable = bindingState.getTableByName( "simple_stuff" );
					assertThat( simpleStuffTable.logicalName().render() ).isEqualTo( "simple_stuff" );
					assertThat( simpleStuffTable.physicalName().render() ).isEqualTo( "SIMPLE_STUFF" );
					assertThat( simpleStuffTable.logicalCatalogName().render() ).isEqualTo( "my_catalog" );
					assertThat( simpleStuffTable.physicalCatalogName().render() ).isEqualTo( "MY_CATALOG" );
					assertThat( simpleStuffTable.logicalSchemaName().render() ).isEqualTo( "my_schema" );
					assertThat( simpleStuffTable.physicalSchemaName().render() ).isEqualTo( "MY_SCHEMA" );
					assertThat( simpleStuffTable.binding().getComment() ).isEqualTo( "Don't sweat it" );

					final var database = metadataCollector.getDatabase();
					final var namespaceItr = database.getNamespaces().iterator();
					final var namespace1 = namespaceItr.next();
					final var namespace2 = namespaceItr.next();
					assertThat( namespaceItr.hasNext() ).isFalse();
					assertThat( namespace1.getTables() ).hasSize( 1 );
					assertThat( namespace2.getTables() ).hasSize( 1 );

					final RootClass entityBinding = (RootClass) context.getMetadataCollector().getEntityBinding( SimpleEntity.class.getName() );
					final Property id = entityBinding.getProperty( "id" );
					assertThat( id.getValue().getTable().getName() ).isEqualTo( "SIMPLETONS" );
					final BasicValue idValue = (BasicValue) id.getValue();
					assertThat( ( (Column) (idValue).getColumn() ).getCanonicalName() ).isEqualTo( "id" );
					assertThat( idValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );

					final Property name = entityBinding.getProperty( "name" );
					assertThat( id.getValue().getTable().getName() ).isEqualTo( "SIMPLETONS" );
					final BasicValue nameValue = (BasicValue) name.getValue();
					assertThat( ( (Column) (nameValue).getColumn() ).getCanonicalName() ).isEqualTo( "name" );
					assertThat( nameValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( String.class );

					final Property data = entityBinding.getProperty( "data" );
					assertThat( data.getValue().getTable().getName() ).isEqualTo( "SIMPLE_STUFF" );
					final BasicValue dataValue = (BasicValue) data.getValue();
					assertThat( ( (Column) (dataValue).getColumn() ).getCanonicalName() ).isEqualTo( "datum" );
					assertThat( dataValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( String.class );
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
