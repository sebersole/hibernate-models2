/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.annotations.EmbeddableInstantiatorRegistration;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.annotations.JdbcTypeRegistration;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbCollectionUserTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbCompositeUserTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbConfigurationParameter;
import org.hibernate.boot.jaxb.mapping.JaxbConverterRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddableInstantiatorRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbEntityListener;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.JaxbFilterDef;
import org.hibernate.boot.jaxb.mapping.JaxbGenericIdGenerator;
import org.hibernate.boot.jaxb.mapping.JaxbJavaTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbJdbcTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbSequenceGenerator;
import org.hibernate.boot.jaxb.mapping.JaxbTableGenerator;
import org.hibernate.boot.jaxb.mapping.JaxbUserTypeRegistration;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.spi.GlobalRegistrations;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.Converter;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hibernate.models.orm.spi.HibernateAnnotations.COLLECTION_TYPE_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.COMPOSITE_TYPE_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.CONVERTER_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.EMBEDDABLE_INSTANTIATOR_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.FILTER_DEF;
import static org.hibernate.models.orm.spi.HibernateAnnotations.JAVA_TYPE_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.JDBC_TYPE_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.TYPE_REG;

/**
 * @author Steve Ebersole
 */
public class GlobalRegistrationsImpl implements GlobalRegistrations {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;

	private List<EntityListenerRegistration> entityListenerRegistrations;

	private List<AnnotationUsage<Converter>> autoAppliedConverters;
	private List<AnnotationUsage<ConverterRegistration>> converterRegistrations;

	private List<AnnotationUsage<JavaTypeRegistration>> javaTypeRegistrations;
	private List<AnnotationUsage<JdbcTypeRegistration>> jdbcTypeRegistrations;
	private List<AnnotationUsage<TypeRegistration>> userTypeRegistrations;
	private List<AnnotationUsage<CompositeTypeRegistration>> compositeUserTypeRegistrations;
	private List<AnnotationUsage<CollectionTypeRegistration>> collectionTypeRegistrations;

	private List<AnnotationUsage<EmbeddableInstantiatorRegistration>> embeddableInstantiatorRegistrations;

	private Map<String,AnnotationUsage<FilterDef>> filterDefRegistrations;

	private Map<String,SequenceGeneratorRegistration> sequenceGeneratorRegistrations;
	private Map<String,TableGeneratorRegistration> tableGeneratorRegistrations;
	private Map<String,GenericGeneratorRegistration> genericGeneratorRegistrations;

	private Map<String, NamedQueryRegistration> jpaNamedQueries;
	private Map<String, NamedQueryRegistration> hibernateNamedHqlQueries;
	private Map<String, NamedQueryRegistration> hibernateNamedNativeQueries;

	public GlobalRegistrationsImpl(SourceModelBuildingContext sourceModelBuildingContext) {
		this( sourceModelBuildingContext.getClassDetailsRegistry(), sourceModelBuildingContext.getAnnotationDescriptorRegistry() );
	}

	public GlobalRegistrationsImpl(ClassDetailsRegistry classDetailsRegistry, AnnotationDescriptorRegistry annotationDescriptorRegistry) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.annotationDescriptorRegistry = annotationDescriptorRegistry;
	}

	public List<EntityListenerRegistration> getEntityListenerRegistrations() {
		return entityListenerRegistrations;
	}

	public List<AnnotationUsage<ConverterRegistration>> getConverterRegistrations() {
		return converterRegistrations == null ? emptyList() : converterRegistrations;
	}

	public List<AnnotationUsage<Converter>> getAutoAppliedConverters() {
		return autoAppliedConverters == null ? emptyList() : autoAppliedConverters;
	}

	public List<AnnotationUsage<JavaTypeRegistration>> getJavaTypeRegistrations() {
		return javaTypeRegistrations == null ? emptyList() : javaTypeRegistrations;
	}

	public List<AnnotationUsage<JdbcTypeRegistration>> getJdbcTypeRegistrations() {
		return jdbcTypeRegistrations == null ? emptyList() : jdbcTypeRegistrations;
	}

	public List<AnnotationUsage<TypeRegistration>> getUserTypeRegistrations() {
		return userTypeRegistrations == null ? emptyList() : userTypeRegistrations;
	}

	public List<AnnotationUsage<CompositeTypeRegistration>> getCompositeUserTypeRegistrations() {
		return compositeUserTypeRegistrations == null ? emptyList() : compositeUserTypeRegistrations;
	}

	public List<AnnotationUsage<CollectionTypeRegistration>> getCollectionTypeRegistrations() {
		return collectionTypeRegistrations == null ? emptyList() : collectionTypeRegistrations;
	}

	public List<AnnotationUsage<EmbeddableInstantiatorRegistration>> getEmbeddableInstantiatorRegistrations() {
		return embeddableInstantiatorRegistrations == null ? emptyList() : embeddableInstantiatorRegistrations;
	}

	public Map<String, AnnotationUsage<FilterDef>> getFilterDefRegistrations() {
		return filterDefRegistrations == null ? emptyMap() : filterDefRegistrations;
	}

	@Override
	public Map<String, NamedQueryRegistration> getJpaNamedQueries() {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public Map<String, NamedQueryRegistration> getHibernateNamedHqlQueries() {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public Map<String, NamedQueryRegistration> getHibernateNamedNativeQueries() {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JavaTypeRegistration

	public void collectJavaTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( JAVA_TYPE_REG, this::collectJavaTypeRegistration );
	}

	public void collectJavaTypeRegistrations(List<JaxbJavaTypeRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> {
			final ClassDetails descriptorClass = classDetailsRegistry.resolveClassDetails( reg.getDescriptor() );
			final ClassDetails domainTypeClass = classDetailsRegistry.resolveClassDetails( reg.getClazz() );

			final DynamicAnnotationUsage<JavaTypeRegistration> annotationUsage = new DynamicAnnotationUsage<>( JavaTypeRegistration.class );
			annotationUsage.setAttributeValue( "javaType", domainTypeClass );
			annotationUsage.setAttributeValue( "descriptorClass", descriptorClass );
			collectJavaTypeRegistration( annotationUsage );
		} );
	}

	public  void collectJavaTypeRegistration(AnnotationUsage<JavaTypeRegistration> annotationUsage) {
		if ( javaTypeRegistrations == null ) {
			javaTypeRegistrations = new ArrayList<>();
		}
		javaTypeRegistrations.add( annotationUsage );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JdbcTypeRegistration

	public void collectJdbcTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( JDBC_TYPE_REG, this::collectJdbcTypeRegistration );
	}

	public void collectJdbcTypeRegistrations(List<JaxbJdbcTypeRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> {
			final ClassDetails descriptorClassDetails = classDetailsRegistry.resolveClassDetails( reg.getDescriptor() );
			final DynamicAnnotationUsage<JdbcTypeRegistration> annotationUsage = new DynamicAnnotationUsage<>( JdbcTypeRegistration.class );
			annotationUsage.setAttributeValue( "registrationCode", reg.getCode() );
			annotationUsage.setAttributeValue( "value", descriptorClassDetails );
			collectJdbcTypeRegistration( annotationUsage );
		} );
	}

	public void collectJdbcTypeRegistration(AnnotationUsage<JdbcTypeRegistration> annotationUsage) {
		if ( jdbcTypeRegistrations == null ) {
			jdbcTypeRegistrations = new ArrayList<>();
		}
		jdbcTypeRegistrations.add( annotationUsage );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ConversionRegistration

	public void collectConverterRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( CONVERTER_REG, this::collectConverterRegistration );
	}

	public void collectConverterRegistrations(List<JaxbConverterRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (registration) -> {
			final MutableClassDetails converterType = (MutableClassDetails) classDetailsRegistry.resolveClassDetails( registration.getConverter() );
			final DynamicAnnotationUsage<ConverterRegistration> annotationUsage = new DynamicAnnotationUsage<>(
					ConverterRegistration.class,
					converterType
			);
			// technically does not need this, but...
			converterType.addAnnotationUsage( annotationUsage );

			final ClassDetails explicitDomainType;
			final String explicitDomainTypeName = registration.getClazz();
			if ( StringHelper.isNotEmpty( explicitDomainTypeName ) ) {
				explicitDomainType = classDetailsRegistry.resolveClassDetails( explicitDomainTypeName );
			}
			else {
				explicitDomainType = null;
			}

			annotationUsage.setAttributeValue( "converter", converterType );
			annotationUsage.setAttributeValue( "domainType", explicitDomainType );
			annotationUsage.setAttributeValue( "autoApply", registration.isAutoApply() );
			collectConverterRegistration( annotationUsage );
		} );
	}

	public void collectConverterRegistration(AnnotationUsage<ConverterRegistration> conversion) {
		if ( converterRegistrations == null ) {
			converterRegistrations = new ArrayList<>();
		}
		converterRegistrations.add( conversion );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UserTypeRegistration

	public void collectUserTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( TYPE_REG, this::collectUserTypeRegistration );
	}

	public void collectUserTypeRegistrations(List<JaxbUserTypeRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> {
			final ClassDetails domainTypeDetails = classDetailsRegistry.resolveClassDetails( reg.getClazz() );
			final ClassDetails descriptorDetails = classDetailsRegistry.resolveClassDetails( reg.getDescriptor() );

			final DynamicAnnotationUsage<TypeRegistration> annotationUsage = new DynamicAnnotationUsage<>( TypeRegistration.class );
			annotationUsage.setAttributeValue( "basicClass", domainTypeDetails );
			annotationUsage.setAttributeValue( "userType", descriptorDetails );
			collectUserTypeRegistration( annotationUsage );
		} );
	}

	public void collectUserTypeRegistration(AnnotationUsage<TypeRegistration> annotationUsage) {
		if ( userTypeRegistrations == null ) {
			userTypeRegistrations = new ArrayList<>();
		}
		userTypeRegistrations.add( annotationUsage );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CompositeUserTypeRegistration

	public void collectCompositeUserTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( COMPOSITE_TYPE_REG, this::collectCompositeUserTypeRegistration );
	}

	public void collectCompositeUserTypeRegistrations(List<JaxbCompositeUserTypeRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> {
			final ClassDetails compositeType = classDetailsRegistry.resolveClassDetails( reg.getClazz() );
			final ClassDetails descriptorType = classDetailsRegistry.resolveClassDetails( reg.getDescriptor() );

			final DynamicAnnotationUsage<CompositeTypeRegistration> usage = new DynamicAnnotationUsage<>( CompositeTypeRegistration.class );
			usage.setAttributeValue( "embeddableClass", compositeType );
			usage.setAttributeValue( "userType", descriptorType );
			collectCompositeUserTypeRegistration( usage );
		} );
	}

	public void collectCompositeUserTypeRegistration(AnnotationUsage<CompositeTypeRegistration> annotationUsage) {
		if ( compositeUserTypeRegistrations == null ) {
			compositeUserTypeRegistrations = new ArrayList<>();
		}
		compositeUserTypeRegistrations.add( annotationUsage );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CollectionTypeRegistration

	public void collectCollectionTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( COLLECTION_TYPE_REG, this::collectCollectionTypeRegistration );
	}

	public void collectCollectionTypeRegistrations(List<JaxbCollectionUserTypeRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> {
			final ClassDetails typeDetails = classDetailsRegistry.resolveClassDetails( reg.getDescriptor() );

			final DynamicAnnotationUsage<CollectionTypeRegistration> typeRegistration = new DynamicAnnotationUsage<>( CollectionTypeRegistration.class );
			typeRegistration.setAttributeValue( "classification", reg.getClassification() );
			typeRegistration.setAttributeValue( "type", typeDetails );
			typeRegistration.setAttributeValue( "parameters", extractParameters( reg.getParameters() ) );

			collectCollectionTypeRegistration( typeRegistration );
		} );
	}

	private List<AnnotationUsage<Parameter>> extractParameters(List<JaxbConfigurationParameter> parameters) {
		if ( CollectionHelper.isEmpty( parameters ) ) {
			return Collections.emptyList();
		}

		final List<AnnotationUsage<Parameter>> result = new ArrayList<>();
		parameters.forEach( (parameter) -> result.add( makeParameter( parameter ) ) );
		return result;
	}

	private AnnotationUsage<Parameter> makeParameter(JaxbConfigurationParameter jaxbParameter) {
		final DynamicAnnotationUsage<Parameter> parameter = new DynamicAnnotationUsage<>( Parameter.class );
		parameter.setAttributeValue( "name", jaxbParameter.getName() );
		parameter.setAttributeValue( "value", jaxbParameter.getValue() );
		return parameter;
	}

	public void collectCollectionTypeRegistration(AnnotationUsage<CollectionTypeRegistration> annotationUsage) {
		if ( collectionTypeRegistrations == null ) {
			collectionTypeRegistrations = new ArrayList<>();
		}
		collectionTypeRegistrations.add( annotationUsage );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableInstantiatorRegistration

	public void collectEmbeddableInstantiatorRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( EMBEDDABLE_INSTANTIATOR_REG, this::collectEmbeddableInstantiatorRegistration );
	}

	public void collectEmbeddableInstantiatorRegistrations(List<JaxbEmbeddableInstantiatorRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> {
			final ClassDetails embeddableClass = classDetailsRegistry.resolveClassDetails( reg.getEmbeddableClass() );
			final ClassDetails instantiatorClass = classDetailsRegistry.resolveClassDetails( reg.getInstantiator() );

			final DynamicAnnotationUsage<EmbeddableInstantiatorRegistration> usage = new DynamicAnnotationUsage<>(
					EmbeddableInstantiatorRegistration.class,
					instantiatorClass
			);
			usage.setAttributeValue( "embeddableClass", embeddableClass );
			usage.setAttributeValue( "instantiator", instantiatorClass );
			collectEmbeddableInstantiatorRegistration( usage );
		} );
	}

	public void collectEmbeddableInstantiatorRegistration(AnnotationUsage<EmbeddableInstantiatorRegistration> usage) {
		if ( embeddableInstantiatorRegistrations == null ) {
			embeddableInstantiatorRegistrations = new ArrayList<>();
		}
		embeddableInstantiatorRegistrations.add( usage );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Filter-defs

	public void collectFilterDefinitions(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( FILTER_DEF, this::collectFilterDefinition );
	}

	public void collectFilterDefinitions(List<JaxbFilterDef> filterDefinitions) {
		if ( CollectionHelper.isEmpty( filterDefinitions ) ) {
			return;
		}

		filterDefinitions.forEach( (filterDefinition) -> {
			final DynamicAnnotationUsage<FilterDef> filterDef = new DynamicAnnotationUsage<>( FilterDef.class );
			filterDef.setAttributeValue( "name", filterDefinition.getName() );
			filterDef.setAttributeValue( "defaultCondition", filterDefinition.getCondition() );
			filterDef.setAttributeValue( "parameters", extractFilterParameters( filterDefinition ) );
			collectFilterDefinition( filterDefinition.getName(), filterDef );
		} );
	}

	private List<AnnotationUsage<ParamDef>> extractFilterParameters(JaxbFilterDef source) {
		final List<JaxbFilterDef.JaxbFilterParam> parameters = source.getFilterParam();

		// todo : update the mapping.xsd to account for new @ParamDef definition
		// todo : handle simplified type names for XML, e.g. "String" instead of "java.lang.String"

		final List<AnnotationUsage<ParamDef>> result = new ArrayList<>( parameters.size() );
		for ( JaxbFilterDef.JaxbFilterParam parameter : parameters ) {
			final DynamicAnnotationUsage<ParamDef> paramDef = new DynamicAnnotationUsage<>( ParamDef.class );
			paramDef.setAttributeValue( "name", parameter.getName() );
			paramDef.setAttributeValue( "type", classDetailsRegistry.resolveClassDetails( parameter.getType() ) );
			result.add( paramDef );
		}
		return result;
	}

	public void collectFilterDefinition(AnnotationUsage<FilterDef> filterDef) {
		collectFilterDefinition( filterDef.getAttributeValue( "name" ), filterDef );
	}

	public void collectFilterDefinition(String name, AnnotationUsage<FilterDef> filterDef) {
		if ( filterDefRegistrations == null ) {
			filterDefRegistrations = new HashMap<>();
		}

		if ( filterDefRegistrations.put( name, filterDef ) != null ) {
			throw new AnnotationException( "Multiple `@FilterDef` annotations define with the same name '" + name + "'" );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityListenerRegistration

	public void collectEntityListenerRegistrations(List<JaxbEntityListener> listeners) {
		if ( CollectionHelper.isEmpty( listeners ) ) {
			return;
		}

		if ( entityListenerRegistrations == null ) {
			entityListenerRegistrations = new ArrayList<>();
		}

		listeners.forEach( (listener) -> {
			final EntityListenerRegistration listenerRegistration = EntityListenerRegistration.from( listener, classDetailsRegistry );
			entityListenerRegistrations.add( listenerRegistration );
		} );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Id generators

	public void collectIdGenerators(JaxbEntityMappings jaxbRoot) {
		collectSequenceGenerators( jaxbRoot.getSequenceGenerators() );
		collectTableGenerators( jaxbRoot.getTableGenerators() );
		collectGenericGenerators( jaxbRoot.getGenericGenerators() );

		// todo : add support for @IdGeneratorType in mapping.xsd?
	}

	public void collectIdGenerators(ClassDetails classDetails) {
		classDetails.forEachAnnotationUsage( SequenceGenerator.class, this::collectSequenceGenerator );
		classDetails.forEachAnnotationUsage( TableGenerator.class, this::collectTableGenerator );
		classDetails.forEachAnnotationUsage( GenericGenerator.class, this::collectGenericGenerator );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sequence generator

	public void collectSequenceGenerators(List<JaxbSequenceGenerator> sequenceGenerators) {
		if ( CollectionHelper.isEmpty( sequenceGenerators ) ) {
			return;
		}

		sequenceGenerators.forEach( (generator) -> {
			final DynamicAnnotationUsage<SequenceGenerator> annotationUsage = new DynamicAnnotationUsage<>( SequenceGenerator.class );
			annotationUsage.setAttributeValue( "name", generator.getName() );
			annotationUsage.setAttributeValue( "sequenceName", generator.getSequenceName() );
			annotationUsage.setAttributeValue( "catalog", generator.getCatalog() );
			annotationUsage.setAttributeValue( "schema", generator.getSchema() );
			annotationUsage.setAttributeValue( "initialValue", generator.getInitialValue() );
			annotationUsage.setAttributeValue( "allocationSize", generator.getAllocationSize() );

			collectSequenceGenerator( new SequenceGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	public void collectSequenceGenerator(AnnotationUsage<SequenceGenerator> usage) {
		collectSequenceGenerator( new SequenceGeneratorRegistration( usage.getAttributeValue( "name" ), usage ) );
	}

	public void collectSequenceGenerator(SequenceGeneratorRegistration generatorRegistration) {
		if ( sequenceGeneratorRegistrations == null ) {
			sequenceGeneratorRegistrations = new HashMap<>();
		}

		sequenceGeneratorRegistrations.put( generatorRegistration.getName(), generatorRegistration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Table generator

	public void collectTableGenerators(List<JaxbTableGenerator> tableGenerators) {
		if ( CollectionHelper.isEmpty( tableGenerators ) ) {
			return;
		}

		tableGenerators.forEach( (generator) -> {
			final DynamicAnnotationUsage<TableGenerator> annotationUsage = new DynamicAnnotationUsage<>( TableGenerator.class );
			annotationUsage.setAttributeValue( "name", generator.getName() );
			annotationUsage.setAttributeValue( "table", generator.getTable() );
			annotationUsage.setAttributeValue( "catalog", generator.getCatalog() );
			annotationUsage.setAttributeValue( "schema", generator.getSchema() );
			annotationUsage.setAttributeValue( "pkColumnName", generator.getPkColumnName() );
			annotationUsage.setAttributeValue( "valueColumnName", generator.getValueColumnName() );
			annotationUsage.setAttributeValue( "pkColumnValue", generator.getPkColumnValue() );
			annotationUsage.setAttributeValue( "initialValue", generator.getInitialValue() );
			annotationUsage.setAttributeValue( "allocationSize", generator.getAllocationSize() );

			collectTableGenerator( new TableGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	public void collectTableGenerator(AnnotationUsage<TableGenerator> usage) {
		collectTableGenerator( new TableGeneratorRegistration( usage.getAttributeValue( "name" ), usage ) );
	}

	public void collectTableGenerator(TableGeneratorRegistration generatorRegistration) {
		if ( tableGeneratorRegistrations == null ) {
			tableGeneratorRegistrations = new HashMap<>();
		}

		tableGeneratorRegistrations.put( generatorRegistration.getName(), generatorRegistration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Generic generators

	private void collectGenericGenerators(List<JaxbGenericIdGenerator> genericGenerators) {
		if ( CollectionHelper.isEmpty( genericGenerators ) ) {
			return;
		}

		genericGenerators.forEach( (generator) -> {
			final DynamicAnnotationUsage<GenericGenerator> annotationUsage = new DynamicAnnotationUsage<>( GenericGenerator.class );
			annotationUsage.setAttributeValue( "name", generator.getName() );
			annotationUsage.setAttributeValue( "strategy", generator.getClazz() );

			// todo : update the mapping.xsd to account for new @GenericGenerator definition

			collectGenericGenerator( new GenericGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	public void collectGenericGenerator(AnnotationUsage<GenericGenerator> usage) {
		collectGenericGenerator( new GenericGeneratorRegistration( usage.getAttributeValue( "name" ), usage ) );
	}

	public void collectGenericGenerator(GenericGeneratorRegistration generatorRegistration) {
		if ( genericGeneratorRegistrations == null ) {
			genericGeneratorRegistrations = new HashMap<>();
		}

		genericGeneratorRegistrations.put( generatorRegistration.getName(), generatorRegistration );
	}
}
