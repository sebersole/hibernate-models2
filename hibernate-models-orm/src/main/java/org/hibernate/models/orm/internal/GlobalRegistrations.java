/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Parameter;
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
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

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
public class GlobalRegistrations {
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry annotationDescriptorRegistry;

	private List<EntityListenerRegistration> entityListenerRegistrations;
	private List<ClassDetails> autoAppliedConverters;
	private List<ConversionRegistration> converterRegistrations;
	private List<JavaTypeRegistration> javaTypeRegistrations;
	private List<JdbcTypeRegistration> jdbcTypeRegistrations;
	private List<UserTypeRegistration> userTypeRegistrations;
	private List<CompositeUserTypeRegistration> compositeUserTypeRegistrations;
	private List<CollectionTypeRegistration> collectionTypeRegistrations;
	private List<EmbeddableInstantiatorRegistration> embeddableInstantiatorRegistrations;
	private Map<String,FilterDefRegistration> filterDefRegistrations;

	private Map<String,SequenceGeneratorRegistration> sequenceGeneratorRegistrations;
	private Map<String,TableGeneratorRegistration> tableGeneratorRegistrations;
	private Map<String,GenericGeneratorRegistration> genericGeneratorRegistrations;

	private Map<String, NamedQueryRegistration> jpaNamedQueries;
	private Map<String, NamedQueryRegistration> hibernateNamedHqlQueries;
	private Map<String, NamedQueryRegistration> hibernateNamedNativeQueries;

	public GlobalRegistrations(SourceModelBuildingContext sourceModelBuildingContext) {
		this( sourceModelBuildingContext.getClassDetailsRegistry(), sourceModelBuildingContext.getAnnotationDescriptorRegistry() );
	}

	public GlobalRegistrations(ClassDetailsRegistry classDetailsRegistry, AnnotationDescriptorRegistry annotationDescriptorRegistry) {
		this.classDetailsRegistry = classDetailsRegistry;
		this.annotationDescriptorRegistry = annotationDescriptorRegistry;
	}

	public List<EntityListenerRegistration> getEntityListenerRegistrations() {
		return entityListenerRegistrations;
	}

	public List<ConversionRegistration> getConverterRegistrations() {
		return converterRegistrations == null ? emptyList() : converterRegistrations;
	}

	public List<ClassDetails> getAutoAppliedConverters() {
		return autoAppliedConverters == null ? emptyList() : autoAppliedConverters;
	}

	public List<JavaTypeRegistration> getJavaTypeRegistrations() {
		return javaTypeRegistrations == null ? emptyList() : javaTypeRegistrations;
	}

	public List<JdbcTypeRegistration> getJdbcTypeRegistrations() {
		return jdbcTypeRegistrations == null ? emptyList() : jdbcTypeRegistrations;
	}

	public List<UserTypeRegistration> getUserTypeRegistrations() {
		return userTypeRegistrations == null ? emptyList() : userTypeRegistrations;
	}

	public List<CompositeUserTypeRegistration> getCompositeUserTypeRegistrations() {
		return compositeUserTypeRegistrations == null ? emptyList() : compositeUserTypeRegistrations;
	}

	public List<CollectionTypeRegistration> getCollectionTypeRegistrations() {
		return collectionTypeRegistrations == null ? emptyList() : collectionTypeRegistrations;
	}

	public List<EmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations() {
		return embeddableInstantiatorRegistrations == null ? emptyList() : embeddableInstantiatorRegistrations;
	}

	public Map<String, FilterDefRegistration> getFilterDefRegistrations() {
		return filterDefRegistrations == null ? emptyMap() : filterDefRegistrations;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JavaTypeRegistration

	public void collectJavaTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( JAVA_TYPE_REG, (usage) -> collectJavaTypeRegistration(
				usage.getAttributeValue( "javaType" ),
				usage.getAttributeValue( "descriptorClass" )
		) );
	}

	public void collectJavaTypeRegistrations(List<JaxbJavaTypeRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectJavaTypeRegistration(
				classDetailsRegistry.resolveClassDetails( reg.getClazz() ),
				classDetailsRegistry.resolveClassDetails( reg.getDescriptor() )
		) );
	}

	public  void collectJavaTypeRegistration(ClassDetails javaType, ClassDetails descriptor) {
		if ( javaTypeRegistrations == null ) {
			javaTypeRegistrations = new ArrayList<>();
		}
		javaTypeRegistrations.add( new JavaTypeRegistration( javaType, descriptor ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JdbcTypeRegistration

	public void collectJdbcTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( JDBC_TYPE_REG, (usage) -> collectJdbcTypeRegistration(
				usage.getAttributeValue( "registrationCode" ),
				usage.getAttributeValue( "value" )
		) );
	}

	public void collectJdbcTypeRegistrations(List<JaxbJdbcTypeRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectJdbcTypeRegistration(
				reg.getCode(),
				classDetailsRegistry.resolveClassDetails( reg.getDescriptor() )
		) );
	}

	public void collectJdbcTypeRegistration(Integer registrationCode, ClassDetails descriptor) {
		if ( jdbcTypeRegistrations == null ) {
			jdbcTypeRegistrations = new ArrayList<>();
		}
		jdbcTypeRegistrations.add( new JdbcTypeRegistration( registrationCode, descriptor ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ConversionRegistration

	public void collectConverterRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( CONVERTER_REG, (usage) -> {
			final ClassDetails domainType = usage.getAttributeValue( "domainType" );
			final ClassDetails converterType = usage.getAttributeValue( "converter" );
			final boolean autoApply = usage.getAttributeValue( "autoApply" );
			collectConverterRegistration( new ConversionRegistration( domainType, converterType, autoApply ) );
		} );
	}

	public void collectConverterRegistrations(List<JaxbConverterRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (registration) -> {
			final ClassDetails explicitDomainType;
			final String explicitDomainTypeName = registration.getClazz();
			if ( StringHelper.isNotEmpty( explicitDomainTypeName ) ) {
				explicitDomainType = classDetailsRegistry.resolveClassDetails( explicitDomainTypeName );
			}
			else {
				explicitDomainType = null;
			}
			final ClassDetails converterType = classDetailsRegistry.resolveClassDetails( registration.getConverter() );
			final boolean autoApply = registration.isAutoApply();
			collectConverterRegistration( new ConversionRegistration( explicitDomainType, converterType, autoApply ) );
		} );
	}

	public void collectConverterRegistration(ConversionRegistration conversion) {
		if ( converterRegistrations == null ) {
			converterRegistrations = new ArrayList<>();
		}
		converterRegistrations.add( conversion );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UserTypeRegistration

	public void collectUserTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( TYPE_REG, (usage) -> collectUserTypeRegistration(
				usage.getAttributeValue( "basicClass" ),
				usage.getAttributeValue( "userType" )
		) );
	}

	public void collectUserTypeRegistrations(List<JaxbUserTypeRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> {
			final ClassDetails domainTypeDetails = classDetailsRegistry.resolveClassDetails( reg.getClazz() );
			final ClassDetails descriptorDetails = classDetailsRegistry.resolveClassDetails( reg.getDescriptor() );
			collectUserTypeRegistration( domainTypeDetails, descriptorDetails );
		} );
	}

	public void collectUserTypeRegistration(ClassDetails domainClass, ClassDetails userTypeClass) {
		if ( userTypeRegistrations == null ) {
			userTypeRegistrations = new ArrayList<>();
		}
		userTypeRegistrations.add( new UserTypeRegistration( domainClass, userTypeClass ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CompositeUserTypeRegistration

	public void collectCompositeUserTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( COMPOSITE_TYPE_REG, (usage) -> collectCompositeUserTypeRegistration(
				usage.getAttributeValue( "embeddableClass" ),
				usage.getAttributeValue( "userType" )
		) );
	}

	public void collectCompositeUserTypeRegistrations(List<JaxbCompositeUserTypeRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectCompositeUserTypeRegistration(
				classDetailsRegistry.resolveClassDetails( reg.getClazz() ),
				classDetailsRegistry.resolveClassDetails( reg.getDescriptor() )
		) );
	}

	public void collectCompositeUserTypeRegistration(ClassDetails domainClass, ClassDetails userTypeClass) {
		if ( compositeUserTypeRegistrations == null ) {
			compositeUserTypeRegistrations = new ArrayList<>();
		}
		compositeUserTypeRegistrations.add( new CompositeUserTypeRegistration( domainClass, userTypeClass ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CollectionTypeRegistration

	public void collectCollectionTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( COLLECTION_TYPE_REG, (usage) -> collectCollectionTypeRegistration(
				usage.getAttributeValue( "classification" ),
				usage.getAttributeValue( "type" ),
				extractParameterMap( usage )
		) );
	}

	private Map<String,String> extractParameterMap(AnnotationUsage<? extends Annotation> source) {
		final List<AnnotationUsage<Parameter>> parameters = source.getAttributeValue( "parameters" );

		final Map<String,String> result = new HashMap<>();
		for ( AnnotationUsage<Parameter> parameter : parameters ) {
			result.put(
					parameter.getAttributeValue( "name" ),
					parameter.getAttributeValue( "value" )
			);
		}
		return result;
	}

	public void collectCollectionTypeRegistrations(List<JaxbCollectionUserTypeRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectCollectionTypeRegistration(
				reg.getClassification(),
				classDetailsRegistry.resolveClassDetails( reg.getDescriptor() ),
				extractParameterMap( reg.getParameters() )
		) );
	}

	private Map<String, String> extractParameterMap(List<JaxbConfigurationParameter> parameters) {
		if ( CollectionHelper.isEmpty( parameters ) ) {
			return Collections.emptyMap();
		}

		final Map<String,String> result = new HashMap<>();
		parameters.forEach( parameter -> result.put( parameter.getName(), parameter.getValue() ) );
		return result;
	}

	public void collectCollectionTypeRegistration(
			CollectionClassification classification,
			ClassDetails userTypeClass,
			Map<String,String> parameters) {
		if ( collectionTypeRegistrations == null ) {
			collectionTypeRegistrations = new ArrayList<>();
		}
		collectionTypeRegistrations.add( new CollectionTypeRegistration( classification, userTypeClass, parameters ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableInstantiatorRegistration

	public void collectEmbeddableInstantiatorRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( EMBEDDABLE_INSTANTIATOR_REG, (usage) -> collectEmbeddableInstantiatorRegistration(
				usage.getAttributeValue( "embeddableClass" ),
				usage.getAttributeValue( "instantiator" )
		) );
	}

	public void collectEmbeddableInstantiatorRegistrations(List<JaxbEmbeddableInstantiatorRegistration> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectEmbeddableInstantiatorRegistration(
				classDetailsRegistry.resolveClassDetails( reg.getEmbeddableClass() ),
				classDetailsRegistry.resolveClassDetails( reg.getInstantiator() )
		) );
	}

	public void collectEmbeddableInstantiatorRegistration(ClassDetails embeddableClass, ClassDetails instantiator) {
		if ( embeddableInstantiatorRegistrations == null ) {
			embeddableInstantiatorRegistrations = new ArrayList<>();
		}
		embeddableInstantiatorRegistrations.add( new EmbeddableInstantiatorRegistration( embeddableClass, instantiator ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Filter-defs

	public void collectFilterDefinitions(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( FILTER_DEF, (usage) -> collectFilterDefinition(
				usage.getAttributeValue( "name" ),
				usage.getAttributeValue( "defaultCondition" ),
				extractFilterParameters( usage )
		) );
	}

	private Map<String, ClassDetails> extractFilterParameters(AnnotationUsage<FilterDef> source) {
		final List<AnnotationUsage<ParamDef>> parameters = source.getAttributeValue( "parameters" );
		final Map<String, ClassDetails> result = new HashMap<>( parameters.size() );
		for ( AnnotationUsage<ParamDef> parameter : parameters ) {
			result.put( parameter.getAttributeValue( "name" ), parameter.getAttributeValue( "type" ) );
		}
		return result;
	}

	public void collectFilterDefinitions(List<JaxbFilterDef> filterDefinitions) {
		if ( CollectionHelper.isEmpty( filterDefinitions ) ) {
			return;
		}

		filterDefinitions.forEach( (filterDefinition) -> collectFilterDefinition(
				filterDefinition.getName(),
				filterDefinition.getCondition(),
				extractFilterParameters( filterDefinition )
		) );
	}

	private Map<String, ClassDetails> extractFilterParameters(JaxbFilterDef source) {
		final List<JaxbFilterDef.JaxbFilterParam> parameters = source.getFilterParam();

		// todo : update the mapping.xsd to account for new @ParamDef definition
		// todo : handle simplified type names for XML, e.g. "String" instead of "java.lang.String"

		final Map<String, ClassDetails> result = new HashMap<>( parameters.size() );
		for ( JaxbFilterDef.JaxbFilterParam parameter : parameters ) {
			result.put( parameter.getName(), classDetailsRegistry.resolveClassDetails( parameter.getType() ) );
		}
		return result;
	}

	public void collectFilterDefinition(String name, String defaultCondition, Map<String, ClassDetails> parameters) {
		if ( filterDefRegistrations == null ) {
			filterDefRegistrations = new HashMap<>();
		}

		if ( filterDefRegistrations.put( name, new FilterDefRegistration( name, defaultCondition, parameters ) ) != null ) {
			throw new AnnotationException( "Multiple '@FilterDef' annotations define a filter named '" + name + "'" );
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
