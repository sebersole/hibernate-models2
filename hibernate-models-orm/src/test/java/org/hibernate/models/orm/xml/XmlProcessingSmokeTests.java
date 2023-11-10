/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.models.orm.categorize.internal.DomainModelCategorizationCollector;
import org.hibernate.models.orm.categorize.internal.GlobalRegistrationsImpl;
import org.hibernate.models.orm.categorize.spi.FilterDefRegistration;
import org.hibernate.models.orm.categorize.xml.internal.XmlDocumentImpl;
import org.hibernate.models.orm.categorize.xml.internal.XmlPreProcessingResultImpl;
import org.hibernate.models.orm.categorize.xml.spi.PersistenceUnitMetadata;
import org.hibernate.models.orm.SourceModelTestHelper;
import org.hibernate.models.internal.StringTypeDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;

import org.junit.jupiter.api.Test;

import static jakarta.persistence.AccessType.FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.annotations.CascadeType.LOCK;
import static org.hibernate.annotations.CascadeType.PERSIST;
import static org.hibernate.annotations.CascadeType.REMOVE;
import static org.hibernate.models.orm.SimpleClassLoading.SIMPLE_CLASS_LOADING;
import static org.hibernate.models.orm.XmlHelper.loadMapping;

/**
 * @author Steve Ebersole
 */
public class XmlProcessingSmokeTests {
	@Test
	void testGlobals() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();
		collector.addDocument( loadMapping( "mappings/globals.xml", SIMPLE_CLASS_LOADING ) );
	}

	@Test
	void testPersistenceUnitDefaults1() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		final JaxbEntityMappingsImpl simple1 = loadMapping( "mappings/simple1.xml", SIMPLE_CLASS_LOADING );
		final JaxbEntityMappingsImpl simple2 = loadMapping( "mappings/simple2.xml", SIMPLE_CLASS_LOADING );
		collector.addDocument( simple1 );
		collector.addDocument( simple2);

		final PersistenceUnitMetadata metadata = collector.getPersistenceUnitMetadata();
		// xml-mappings-complete is a gated flag - once we see a true, it should always be considered true
		assertThat( metadata.areXmlMappingsComplete() ).isTrue();
		// same for quoted identifiers
		assertThat( metadata.useQuotedIdentifiers() ).isTrue();

		// default cascades are additive
		assertThat( metadata.getDefaultCascadeTypes() ).containsAll( List.of( PERSIST, REMOVE, LOCK ) );

		// simple2.xml should take precedence
		assertThat( metadata.getDefaultCatalog() ).isEqualTo( "catalog2" );
		assertThat( metadata.getDefaultSchema() ).isEqualTo( "schema2" );
		assertThat( metadata.getAccessType() ).isEqualTo( FIELD );
		assertThat( metadata.getDefaultAccessStrategyName() ).isEqualTo( "MIXED" );
	}

	@Test
	void testPersistenceUnitDefaults2() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		collector.addDocument( loadMapping( "mappings/simple2.xml", SIMPLE_CLASS_LOADING ) );
		collector.addDocument( loadMapping( "mappings/simple1.xml", SIMPLE_CLASS_LOADING ) );

		final PersistenceUnitMetadata metadata = collector.getPersistenceUnitMetadata();
		// xml-mappings-complete is a gated flag - once we see a true, it should always be considered true
		assertThat( metadata.areXmlMappingsComplete() ).isTrue();
		// same for quoted identifiers
		assertThat( metadata.useQuotedIdentifiers() ).isTrue();

		// default cascades are additive
		assertThat( metadata.getDefaultCascadeTypes() ).containsAll( List.of( PERSIST, REMOVE, LOCK ) );

		// simple1.xml should take precedence
		assertThat( metadata.getDefaultCatalog() ).isEqualTo( "catalog1" );
		assertThat( metadata.getDefaultSchema() ).isEqualTo( "schema1" );
		assertThat( metadata.getAccessType() ).isEqualTo( FIELD );
		assertThat( metadata.getDefaultAccessStrategyName() ).isEqualTo( "MIXED" );
	}

	@Test
	void testSimpleXmlDocumentBuilding() {
		final XmlPreProcessingResultImpl collector = new XmlPreProcessingResultImpl();

		final JaxbEntityMappingsImpl simple1 = loadMapping( "mappings/simple1.xml", SIMPLE_CLASS_LOADING );
		final JaxbEntityMappingsImpl simple2 = loadMapping( "mappings/simple2.xml", SIMPLE_CLASS_LOADING );
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
		final SourceModelBuildingContext buildingContext = SourceModelTestHelper.createBuildingContext( StringTypeDescriptor.class );
		final XmlPreProcessingResultImpl collectedXmlResources = new XmlPreProcessingResultImpl();

		final JaxbEntityMappingsImpl xmlMapping = loadMapping( "mappings/globals.xml", SIMPLE_CLASS_LOADING );
		collectedXmlResources.addDocument( xmlMapping );

		final DomainModelCategorizationCollector collector = new DomainModelCategorizationCollector( false, buildingContext.getClassDetailsRegistry() );
		collectedXmlResources.getDocuments().forEach( collector::apply );

		final GlobalRegistrationsImpl globalRegistrations = collector.getGlobalRegistrations();
		assertThat( globalRegistrations.getJavaTypeRegistrations() ).hasSize( 1 );
		assertThat( globalRegistrations.getJavaTypeRegistrations().get(0).getDescriptor().getClassName() )
				.isEqualTo( StringTypeDescriptor.class.getName() );

		assertThat( globalRegistrations.getJdbcTypeRegistrations() ).hasSize( 1 );
		assertThat( globalRegistrations.getJdbcTypeRegistrations().get(0).getDescriptor().getClassName() )
				.isEqualTo( ClobJdbcType.class.getName() );

		assertThat( globalRegistrations.getUserTypeRegistrations() ).hasSize( 1 );
		assertThat( globalRegistrations.getUserTypeRegistrations().get(0).getUserTypeClass().getClassName() )
				.isEqualTo( MyUserType.class.getName() );

		assertThat( globalRegistrations.getConverterRegistrations() ).hasSize( 1 );
		assertThat( globalRegistrations.getConverterRegistrations().get(0).getConverterType().getClassName() )
				.isEqualTo( org.hibernate.type.YesNoConverter.class.getName() );

		validateFilterDefs( globalRegistrations.getFilterDefRegistrations() );
	}

	private void validateFilterDefs(Map<String, FilterDefRegistration> filterDefRegistrations) {
		assertThat( filterDefRegistrations ).hasSize( 2 );

		final FilterDefRegistration amountFilter = filterDefRegistrations.get( "amount_filter" );
		assertThat( amountFilter.getDefaultCondition() ).isEqualTo( "amount = :amount" );
		assertThat( amountFilter.getParameters() ).hasSize( 1 );
		final ClassDetails amountParameterType = amountFilter.getParameters().get( "amount" );
		assertThat( amountParameterType.getClassName() ).isEqualTo( int.class.getName() );

		final FilterDefRegistration nameFilter = filterDefRegistrations.get( "name_filter" );
		assertThat( nameFilter.getDefaultCondition() ).isEqualTo( "name = :name" );
		assertThat( nameFilter.getParameters() ).hasSize( 1 );
		final ClassDetails nameParameterType = nameFilter.getParameters().get( "name" );
		assertThat( nameParameterType.getClassName() ).isEqualTo( String.class.getName() );
	}
}
