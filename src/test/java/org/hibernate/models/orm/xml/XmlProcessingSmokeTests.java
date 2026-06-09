/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.DatabaseObjectRegistration;
import org.hibernate.boot.models.categorize.spi.DialectScopeRegistration;
import org.hibernate.boot.models.categorize.spi.DomainModelCategorizer;
import org.hibernate.boot.models.categorize.spi.FetchProfileRegistration;
import org.hibernate.boot.models.source.AvailableResources;
import org.hibernate.boot.models.source.AvailableResourcesContext;
import org.hibernate.boot.models.xml.internal.XmlDocumentImpl;
import org.hibernate.boot.models.xml.internal.XmlPreProcessingResultImpl;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.models.categorize.internal.GlobalRegistrationsImpl;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.models.internal.StringTypeDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;

import org.junit.jupiter.api.Test;

import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.SqlResultSetMapping;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.AccessType.FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.models.orm.XmlHelper.loadMapping;

/**
 * @author Steve Ebersole
 */
public class XmlProcessingSmokeTests {
	@Test
	void testGlobals() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();
		collector.addDocument( loadMapping( "mappings/globals.xml" ) );
	}

	@Test
	void testPersistenceUnitDefaults1() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		final Binding<JaxbEntityMappingsImpl> simple1 = loadMapping( "mappings/simple1.xml" );
		final Binding<JaxbEntityMappingsImpl> simple2 = loadMapping( "mappings/simple2.xml" );
		collector.addDocument( simple1 );
		collector.addDocument( simple2);

		final PersistenceUnitMetadata metadata = collector.getPersistenceUnitMetadata();
		// xml-mappings-complete is a gated flag - once we see a true, it should always be considered true
		assertThat( metadata.areXmlMappingsComplete() ).isTrue();
		// same for quoted identifiers
		assertThat( metadata.useQuotedIdentifiers() ).isTrue();

		// default cascades are additive
		assertThat( metadata.getDefaultCascadeTypes() ).containsAll( List.of( PERSIST, REMOVE ) );

		// simple2.xml should take precedence
		assertThat( metadata.getDefaultCatalog() ).isEqualTo( "catalog2" );
		assertThat( metadata.getDefaultSchema() ).isEqualTo( "schema2" );
		assertThat( metadata.getAccessType() ).isEqualTo( FIELD );
		assertThat( metadata.getDefaultAccessStrategyName() ).isEqualTo( "MIXED" );
	}

	@Test
	void testPersistenceUnitDefaults2() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		collector.addDocument( loadMapping( "mappings/simple2.xml" ) );
		collector.addDocument( loadMapping( "mappings/simple1.xml" ) );

		final PersistenceUnitMetadata metadata = collector.getPersistenceUnitMetadata();
		// xml-mappings-complete is a gated flag - once we see a true, it should always be considered true
		assertThat( metadata.areXmlMappingsComplete() ).isTrue();
		// same for quoted identifiers
		assertThat( metadata.useQuotedIdentifiers() ).isTrue();

		// default cascades are additive
		assertThat( metadata.getDefaultCascadeTypes() ).containsAll( List.of( PERSIST, REMOVE ) );

		// simple1.xml should take precedence
		assertThat( metadata.getDefaultCatalog() ).isEqualTo( "catalog1" );
		assertThat( metadata.getDefaultSchema() ).isEqualTo( "schema1" );
		assertThat( metadata.getAccessType() ).isEqualTo( FIELD );
		assertThat( metadata.getDefaultAccessStrategyName() ).isEqualTo( "MIXED" );
	}

	@Test
	void testSimpleXmlDocumentBuilding() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		final Binding<JaxbEntityMappingsImpl> simple1 = loadMapping( "mappings/simple1.xml" );
		final Binding<JaxbEntityMappingsImpl> simple2 = loadMapping( "mappings/simple2.xml" );
		collector.addDocument( simple1 );
		collector.addDocument( simple2 );

		final PersistenceUnitMetadata metadata = collector.getPersistenceUnitMetadata();

		final XmlDocumentImpl simple1Doc = XmlDocumentImpl.consume( simple1, metadata );
		assertThat( simple1Doc.getDefaults().getPackage() ).isEqualTo( "org.hibernate.models.orm.xml" );
		assertThat( simple1Doc.getEntityMappings() ).hasSize( 1 );
		assertThat( simple1Doc.getEntityMappings().get( 0 ).getClazz() ).isEqualTo( "SimpleEntity" );
		assertThat( simple1Doc.getEntityMappings().get( 0 ).getName() ).isNull();
		assertThat( simple1Doc.getEntityMappings().get( 0 ).isMetadataComplete() ).isNull();

		final XmlDocumentImpl simple2Doc = XmlDocumentImpl.consume( simple2, metadata );
		assertThat( simple2Doc.getDefaults().getPackage() ).isNull();
		assertThat( simple2Doc.getEntityMappings() ).hasSize( 1 );
		assertThat( simple2Doc.getEntityMappings().get( 0 ).getClazz() ).isNull();
		assertThat( simple2Doc.getEntityMappings().get( 0 ).getName() ).isEqualTo( "DynamicEntity" );
		assertThat( simple2Doc.getEntityMappings().get( 0 ).isMetadataComplete() ).isTrue();
	}

	@Test
	void testSimpleGlobalXmlProcessing() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final ModelsContext buildingContext = metadataBuildingContext.getBootstrapContext().getModelsContext();
			buildingContext.getClassDetailsRegistry().resolveClassDetails( StringTypeDescriptor.class.getName() );

			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/globals.xml" );
			final AvailableResources availableResources = AvailableResources.from(
					persistenceConfiguration,
					new AvailableResourcesContext(
							metadataBuildingContext.getBootstrapContext().getModelsContext(),
							metadataBuildingContext.getBootstrapContext().getServiceRegistry()
					)
			);
			final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
					availableResources,
					metadataBuildingContext
			);

			final GlobalRegistrationsImpl globalRegistrations = (GlobalRegistrationsImpl) categorizedDomainModel.getGlobalRegistrations();
			assertThat( globalRegistrations.getJavaTypeRegistrations() ).hasSize( 1 );
			assertThat( globalRegistrations.getJavaTypeRegistrations().get(0).descriptor().getClassName() )
					.isEqualTo( StringTypeDescriptor.class.getName() );

			assertThat( globalRegistrations.getJdbcTypeRegistrations() ).hasSize( 1 );
			assertThat( globalRegistrations.getJdbcTypeRegistrations().get(0).descriptor().getClassName() )
					.isEqualTo( ClobJdbcType.class.getName() );

			assertThat( globalRegistrations.getUserTypeRegistrations() ).hasSize( 1 );
			assertThat( globalRegistrations.getUserTypeRegistrations().get(0).userTypeClass().getClassName() )
					.isEqualTo( MyUserType.class.getName() );

			assertThat( globalRegistrations.getConverterRegistrations() ).hasSize( 1 );
			assertThat( globalRegistrations.getConverterRegistrations().get(0).converterType().getClassName() )
					.isEqualTo( org.hibernate.type.YesNoConverter.class.getName() );

			validateFilterDefs( globalRegistrations.getFilterDefRegistrations() );
		}
	}

	@Test
	void testRootGlobalXmlProcessing() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataBuildingContextTestingImpl metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
			final HibernatePersistenceConfiguration persistenceConfiguration = new HibernatePersistenceConfiguration( "test" );
			persistenceConfiguration.mappingFile( "mappings/xml-global-objects.xml" );
			final AvailableResources availableResources = AvailableResources.from(
					persistenceConfiguration,
					new AvailableResourcesContext(
							metadataBuildingContext.getBootstrapContext().getModelsContext(),
							metadataBuildingContext.getBootstrapContext().getServiceRegistry()
					)
			);
			final CategorizedDomainModel categorizedDomainModel = DomainModelCategorizer.categorize(
					availableResources,
					metadataBuildingContext
			);

			final GlobalRegistrationsImpl globalRegistrations = (GlobalRegistrationsImpl) categorizedDomainModel.getGlobalRegistrations();

			assertThat( globalRegistrations.getConverterRegistrations() ).hasSize( 2 );
			assertThat( globalRegistrations.getImportedRenames() )
					.containsEntry( "XmlSimpleEntity", "org.hibernate.models.orm.xml.SimpleEntity" );

			final FetchProfileRegistration fetchProfile = globalRegistrations.getFetchProfileRegistrations().get( 0 );
			assertThat( fetchProfile.getName() ).isEqualTo( "customer-with-orders" );
			assertThat( fetchProfile.getFetchOverrides() ).singleElement().satisfies( (fetchOverride) -> {
				assertThat( fetchOverride.entityName() ).isEqualTo( "Customer" );
				assertThat( fetchOverride.association() ).isEqualTo( "orders" );
				assertThat( fetchOverride.style() ).isEqualTo( "join" );
			} );

			final NamedQuery namedQuery = (NamedQuery) globalRegistrations.getNamedQueryRegistrations()
					.get( "rootHqlQuery" )
					.getConfiguration();
			assertThat( namedQuery.query() ).isEqualTo( "from Customer" );
			assertThat( namedQuery.hints() ).singleElement().satisfies( (hint) -> {
				assertThat( hint.name() ).isEqualTo( "root.hint" );
				assertThat( hint.value() ).isEqualTo( "root-value" );
			} );

			final NamedNativeQuery nativeQuery = (NamedNativeQuery) globalRegistrations.getNamedNativeQueryRegistrations()
					.get( "rootNativeQuery" )
					.getConfiguration();
			assertThat( nativeQuery.query() ).isEqualTo( "select id, name from customers" );
			assertThat( nativeQuery.columns() ).singleElement().satisfies( (columnResult) -> {
				assertThat( columnResult.name() ).isEqualTo( "id" );
				assertThat( columnResult.type() ).isEqualTo( Long.class );
			} );

			final NamedStoredProcedureQuery storedProcedureQuery = (NamedStoredProcedureQuery) globalRegistrations
					.getNamedStoredProcedureQueryRegistrations()
					.get( "rootStoredProcedure" )
					.getConfiguration();
			assertThat( storedProcedureQuery.procedureName() ).isEqualTo( "sp_customers" );
			assertThat( storedProcedureQuery.parameters() ).singleElement().satisfies( (parameter) -> {
				assertThat( parameter.name() ).isEqualTo( "name" );
				assertThat( parameter.type() ).isEqualTo( String.class );
			} );

			final SqlResultSetMapping sqlResultSetMapping = globalRegistrations.getSqlResultSetMappingRegistrations()
					.get( "rootResultSetMapping" )
					.configuration();
			assertThat( sqlResultSetMapping.columns() ).singleElement().satisfies( (columnResult) -> {
				assertThat( columnResult.name() ).isEqualTo( "name" );
				assertThat( columnResult.type() ).isEqualTo( String.class );
			} );

			final DatabaseObjectRegistration databaseObject = globalRegistrations.getDatabaseObjectRegistrations().get( 0 );
			assertThat( databaseObject.create() ).isEqualTo( "create sequence xml_global_sequence" );
			assertThat( databaseObject.drop() ).isEqualTo( "drop sequence xml_global_sequence" );
			final DialectScopeRegistration dialectScope = databaseObject.dialectScopes().get( 0 );
			assertThat( dialectScope.name() ).isEqualTo( "org.hibernate.dialect.H2Dialect" );
		}
	}

	private void validateFilterDefs(Map<String, FilterDefRegistration> filterDefRegistrations) {
		assertThat( filterDefRegistrations ).hasSize( 2 );

		final FilterDefRegistration amountFilter = filterDefRegistrations.get( "amount_filter" );
		assertThat( amountFilter.defaultCondition() ).isEqualTo( "amount = :amount" );
		assertThat( amountFilter.parameters() ).hasSize( 1 );
		final ClassDetails amountParameterType = amountFilter.parameters().get( "amount" );
		assertThat( amountParameterType.getClassName() ).isEqualTo( int.class.getName() );

		final FilterDefRegistration nameFilter = filterDefRegistrations.get( "name_filter" );
		assertThat( nameFilter.defaultCondition() ).isEqualTo( "name = :name" );
		assertThat( nameFilter.parameters() ).hasSize( 1 );
		final ClassDetails nameParameterType = nameFilter.parameters().get( "name" );
		assertThat( nameParameterType.getClassName() ).isEqualTo( String.class.getName() );
	}
}
