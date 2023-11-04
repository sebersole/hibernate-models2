/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.orm.bind.internal.PhysicalTable;
import org.hibernate.models.orm.bind.internal.SecondaryTable;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EnumType;

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
		BindingTestingHelper.checkDomainModel(
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
					assertThat( entityBinding.isCached() ).isFalse();
					final Column softDeleteColumn = entityBinding.getSoftDeleteColumn();
					assertThat( softDeleteColumn ).isNotNull();
					assertThat( softDeleteColumn.getName() ).isEqualTo( "ACTIVE" );
					assertThat( entityBinding.getFilters() ).hasSize( 1 );
					assertThat( entityBinding.getCacheRegionName() ).isEqualTo( "my-region" );
					assertThat( entityBinding.getCacheConcurrencyStrategy() ).isEqualTo( CacheConcurrencyStrategy.READ_ONLY.toAccessType().getExternalName() );

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

					final Property stuff = entityBinding.getProperty( "stuff" );
					assertThat( stuff.getValue().getTable().getName() ).isEqualTo( "SIMPLETONS" );
					final BasicValue stuffValue = (BasicValue) stuff.getValue();
					assertThat( stuffValue.getEnumerationStyle() ).isEqualTo( EnumType.STRING );
					assertThat( ( (Column) stuffValue.getColumn() ).getCanonicalName() ).isEqualTo( "stuff" );
					assertThat( stuffValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( SimpleEntity.Stuff.class );
					assertThat( stuffValue.resolve().getJdbcType().getJdbcTypeCode() ).isEqualTo( SqlTypes.VARCHAR );

					final Property tenantKey = entityBinding.getProperty( "tenantKey" );
					final BasicValue tenantKeyValue = (BasicValue) tenantKey.getValue();
					assertThat( ( (Column) tenantKeyValue.getColumn() ).getCanonicalName() ).isEqualTo( "tenantkey" );
					assertThat( tenantKeyValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( String.class );

					final Property version = entityBinding.getProperty( "version" );
					final BasicValue versionValue = (BasicValue) version.getValue();
					assertThat( ( (Column) versionValue.getColumn() ).getCanonicalName() ).isEqualTo( "version" );
					assertThat( versionValue.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );
				},
				scope.getRegistry(),
				SimpleEntity.class
		);
	}
}
