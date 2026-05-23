/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
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
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCompositeUserTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableInstantiatorRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterDefImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJavaTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJdbcTypeRegistrationImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeRegistrationImpl;
import org.hibernate.boot.models.categorize.spi.CollectionTypeRegistration;
import org.hibernate.boot.models.categorize.spi.CompositeUserTypeRegistration;
import org.hibernate.boot.models.categorize.spi.ConversionRegistration;
import org.hibernate.boot.models.categorize.spi.EmbeddableInstantiatorRegistration;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.GenericGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.models.categorize.spi.JavaTypeRegistration;
import org.hibernate.boot.models.categorize.spi.JdbcTypeRegistration;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.categorize.spi.NamedQueryRegistration;
import org.hibernate.boot.models.categorize.spi.SequenceGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.TableGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.UserTypeRegistration;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.AttributeConverter;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hibernate.internal.util.GenericsHelper.typeArguments;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
/**
 * @author Steve Ebersole
 */
public class GlobalRegistrationsImpl implements GlobalRegistrations {
	private final ModelsContext modelsContext;
	private final ClassDetailsRegistry classDetailsRegistry;
	private final AnnotationDescriptorRegistry descriptorRegistry;

	private List<JpaEventListener> jpaEventListeners;
	private List<ConversionRegistration> converterRegistrations;
	private List<JavaTypeRegistration> javaTypeRegistrations;
	private List<JdbcTypeRegistration> jdbcTypeRegistrations;
	private List<UserTypeRegistration> userTypeRegistrations;
	private List<CompositeUserTypeRegistration> compositeUserTypeRegistrations;
	private List<CollectionTypeRegistration> collectionTypeRegistrations;
	private List<EmbeddableInstantiatorRegistration> embeddableInstantiatorRegistrations;
	private Map<String, FilterDefRegistration> filterDefRegistrations;

	private Map<String, SequenceGeneratorRegistration> sequenceGeneratorRegistrations;
	private Map<String, TableGeneratorRegistration> tableGeneratorRegistrations;
	private Map<String, GenericGeneratorRegistration> genericGeneratorRegistrations;
	private Map<String, NamedQueryRegistration> namedQueryRegistrations;
	private Map<String, NamedQueryRegistration> namedNativeQueryRegistrations;
	private Map<String, NamedQueryRegistration> namedStoredProcedureQueryRegistrations;
	private Map<String, NamedEntityGraphDefinition> namedEntityGraphRegistrations;

	public GlobalRegistrationsImpl(ModelsContext modelsContext) {
		this( modelsContext, modelsContext.getClassDetailsRegistry(), modelsContext.getAnnotationDescriptorRegistry() );
	}

	public GlobalRegistrationsImpl(
			ModelsContext modelsContext,
			ClassDetailsRegistry classDetailsRegistry,
			AnnotationDescriptorRegistry descriptorRegistry) {
		this.modelsContext = modelsContext;
		this.classDetailsRegistry = classDetailsRegistry;
		this.descriptorRegistry = descriptorRegistry;
	}

	@Override
	public List<JpaEventListener> getEntityListenerRegistrations() {
		return jpaEventListeners == null ? emptyList() : jpaEventListeners;
	}

	@Override
	public List<ConversionRegistration> getConverterRegistrations() {
		return converterRegistrations == null ? emptyList() : converterRegistrations;
	}

	@Override
	public List<JavaTypeRegistration> getJavaTypeRegistrations() {
		return javaTypeRegistrations == null ? emptyList() : javaTypeRegistrations;
	}

	@Override
	public List<JdbcTypeRegistration> getJdbcTypeRegistrations() {
		return jdbcTypeRegistrations == null ? emptyList() : jdbcTypeRegistrations;
	}

	@Override
	public List<UserTypeRegistration> getUserTypeRegistrations() {
		return userTypeRegistrations == null ? emptyList() : userTypeRegistrations;
	}

	@Override
	public List<CompositeUserTypeRegistration> getCompositeUserTypeRegistrations() {
		return compositeUserTypeRegistrations == null ? emptyList() : compositeUserTypeRegistrations;
	}

	@Override
	public List<CollectionTypeRegistration> getCollectionTypeRegistrations() {
		return collectionTypeRegistrations == null ? emptyList() : collectionTypeRegistrations;
	}

	@Override
	public List<EmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations() {
		return embeddableInstantiatorRegistrations == null ? emptyList() : embeddableInstantiatorRegistrations;
	}

	@Override
	public Map<String, FilterDefRegistration> getFilterDefRegistrations() {
		return filterDefRegistrations == null ? emptyMap() : filterDefRegistrations;
	}

	@Override
	public Map<String, SequenceGeneratorRegistration> getSequenceGeneratorRegistrations() {
		return sequenceGeneratorRegistrations == null ? emptyMap() : sequenceGeneratorRegistrations;
	}

	@Override
	public Map<String, TableGeneratorRegistration> getTableGeneratorRegistrations() {
		return tableGeneratorRegistrations == null ? emptyMap() : tableGeneratorRegistrations;
	}

	@Override
	public Map<String, GenericGeneratorRegistration> getGenericGeneratorRegistrations() {
		return genericGeneratorRegistrations == null ? emptyMap() : genericGeneratorRegistrations;
	}

	@Override
	public Map<String, NamedQueryRegistration> getNamedQueryRegistrations() {
		return namedQueryRegistrations == null ? emptyMap() : namedQueryRegistrations;
	}

	@Override
	public Map<String, NamedQueryRegistration> getNamedNativeQueryRegistrations() {
		return namedNativeQueryRegistrations == null ? emptyMap() : namedNativeQueryRegistrations;
	}

	@Override
	public Map<String, NamedQueryRegistration> getNamedStoredProcedureQueryRegistrations() {
		return namedStoredProcedureQueryRegistrations == null ? emptyMap() : namedStoredProcedureQueryRegistrations;
	}

	@Override
	public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphRegistrations() {
		return namedEntityGraphRegistrations == null ? emptyMap() : namedEntityGraphRegistrations;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JavaTypeRegistration

	public void collectJavaTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.JavaTypeRegistration.class, modelsContext, (usage) -> collectJavaTypeRegistration(
				classDetailsRegistry.resolveClassDetails( usage.javaType().getName() ),
				classDetailsRegistry.resolveClassDetails( usage.descriptorClass().getName() )
		) );
	}

	public void collectJavaTypeRegistrations(List<JaxbJavaTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectJavaTypeRegistration(
				classDetailsRegistry.resolveClassDetails( reg.getClazz() ),
				classDetailsRegistry.resolveClassDetails( reg.getDescriptor() )
		) );
	}

	public  void collectJavaTypeRegistration(ClassDetails javaType, ClassDetails descriptor) {
		collectJavaTypeRegistration( new JavaTypeRegistration( javaType, descriptor ) );
	}

	public  void collectJavaTypeRegistration(JavaTypeRegistration registration) {
		if ( javaTypeRegistrations == null ) {
			javaTypeRegistrations = new ArrayList<>();
		}
		javaTypeRegistrations.add( registration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JdbcTypeRegistration

	public void collectJdbcTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.JdbcTypeRegistration.class, modelsContext, (usage) -> collectJdbcTypeRegistration(
				usage.registrationCode(),
				classDetailsRegistry.resolveClassDetails( usage.value().getName() )
		) );
	}

	public void collectJdbcTypeRegistrations(List<JaxbJdbcTypeRegistrationImpl> registrations) {
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
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.ConverterRegistration.class, modelsContext, (usage) -> {
			final ClassDetails domainType = usage.domainType() == void.class
					? null
					: classDetailsRegistry.resolveClassDetails( usage.domainType().getName() );
			final ClassDetails converterType = classDetailsRegistry.resolveClassDetails( usage.converter().getName() );
			collectConverterRegistration( new ConversionRegistration( domainType, converterType, usage.autoApply(), descriptorRegistry.getDescriptor( org.hibernate.annotations.ConverterRegistration.class ) ) );
		} );
	}

	public void collectConverter(AnnotationTarget annotationTarget) {
		final Converter converter = annotationTarget.getDirectAnnotationUsage( Converter.class );
		if ( converter == null || !( annotationTarget instanceof ClassDetails converterType ) ) {
			return;
		}
		final ClassDetails domainType = converterDomainType( converterType );
		collectConverterRegistration( new ConversionRegistration(
				domainType,
				converterType,
				converter.autoApply(),
				descriptorRegistry.getDescriptor( Converter.class )
		) );
	}

	private ClassDetails converterDomainType(ClassDetails converterType) {
		final Type[] typeArguments = typeArguments( AttributeConverter.class, converterType.toJavaClass() );
		if ( typeArguments.length == 0 || !( typeArguments[0] instanceof Class<?> domainType ) ) {
			return null;
		}
		return classDetailsRegistry.resolveClassDetails( domainType.getName() );
	}

	public void collectConverterRegistrations(List<JaxbConverterRegistrationImpl> registrations) {
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
			collectConverterRegistration( new ConversionRegistration( explicitDomainType, converterType, autoApply, descriptorRegistry.getDescriptor( org.hibernate.annotations.ConverterRegistration.class ) ) );
		} );
	}

	public void collectConverterRegistration(ConversionRegistration conversion) {
		if ( converterRegistrations == null ) {
			converterRegistrations = new ArrayList<>();
		}
		converterRegistrations.add( conversion );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named queries and graphs

	public void collectNamedQueryRegistrations(AnnotationTarget annotationTarget) {
		for ( NamedQuery usage : annotationTarget.getRepeatedAnnotationUsages( NamedQuery.class, modelsContext ) ) {
			collectNamedQueryRegistration( usage.name(), NamedQueryRegistration.Kind.HQL, true, usage );
		}
		for ( NamedNativeQuery usage : annotationTarget.getRepeatedAnnotationUsages( NamedNativeQuery.class, modelsContext ) ) {
			collectNamedQueryRegistration( usage.name(), NamedQueryRegistration.Kind.NATIVE, true, usage );
		}
		for ( NamedStoredProcedureQuery usage : annotationTarget.getRepeatedAnnotationUsages( NamedStoredProcedureQuery.class, modelsContext ) ) {
			collectNamedQueryRegistration( usage.name(), NamedQueryRegistration.Kind.CALLABLE, true, usage );
		}
		for ( org.hibernate.annotations.NamedQuery usage : annotationTarget.getRepeatedAnnotationUsages(
				org.hibernate.annotations.NamedQuery.class,
				modelsContext
		) ) {
			collectNamedQueryRegistration( usage.name(), NamedQueryRegistration.Kind.HQL, false, usage );
		}
		for ( org.hibernate.annotations.NamedNativeQuery usage : annotationTarget.getRepeatedAnnotationUsages(
				org.hibernate.annotations.NamedNativeQuery.class,
				modelsContext
		) ) {
			collectNamedQueryRegistration( usage.name(), NamedQueryRegistration.Kind.NATIVE, false, usage );
		}
	}

	private void collectNamedQueryRegistration(
			String name,
			NamedQueryRegistration.Kind kind,
			boolean isJpa,
			Annotation configuration) {
		final Map<String, NamedQueryRegistration> registrations = switch ( kind ) {
			case HQL -> {
				if ( namedQueryRegistrations == null ) {
					namedQueryRegistrations = new HashMap<>();
				}
				yield namedQueryRegistrations;
			}
			case NATIVE -> {
				if ( namedNativeQueryRegistrations == null ) {
					namedNativeQueryRegistrations = new HashMap<>();
				}
				yield namedNativeQueryRegistrations;
			}
			case CALLABLE -> {
				if ( namedStoredProcedureQueryRegistrations == null ) {
					namedStoredProcedureQueryRegistrations = new HashMap<>();
				}
				yield namedStoredProcedureQueryRegistrations;
			}
		};
		registrations.put( name, new NamedQueryRegistration( name, kind, isJpa, configuration ) );
	}

	public void collectNamedEntityGraphRegistrations(ClassDetails classDetails) {
		for ( NamedEntityGraph usage : classDetails.getRepeatedAnnotationUsages( NamedEntityGraph.class, modelsContext ) ) {
			collectNamedEntityGraphRegistration( graphName( classDetails, usage ), jpaEntityName( classDetails ), usage );
		}
		for ( org.hibernate.annotations.NamedEntityGraph usage : classDetails.getRepeatedAnnotationUsages(
				org.hibernate.annotations.NamedEntityGraph.class,
				modelsContext
		) ) {
			collectNamedEntityGraphRegistration( usage.name(), null, usage );
		}
	}

	private void collectNamedEntityGraphRegistration(
			String name,
			String entityName,
			Annotation configuration) {
		if ( namedEntityGraphRegistrations == null ) {
			namedEntityGraphRegistrations = new HashMap<>();
		}
		namedEntityGraphRegistrations.put( name, new NamedEntityGraphDefinition(
				name,
				entityName,
				configuration instanceof NamedEntityGraph ? NamedEntityGraphDefinition.Source.JPA : NamedEntityGraphDefinition.Source.PARSED,
				(entityDomainClassResolver, entityDomainNameResolver, serviceRegistry) -> {
					throw new UnsupportedOperationException(
							"Named entity graph creation is deferred to ORM runtime integration - " + name
					);
				}
		) );
	}

	private static String graphName(ClassDetails classDetails, NamedEntityGraph graph) {
		return isNotEmpty( graph.name() ) ? graph.name() : jpaEntityName( classDetails );
	}

	private static String jpaEntityName(ClassDetails classDetails) {
		final Entity entity = classDetails.getDirectAnnotationUsage( Entity.class );
		if ( entity != null && isNotEmpty( entity.name() ) ) {
			return entity.name();
		}
		return StringHelper.unqualify( classDetails.getName() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UserTypeRegistration

	public void collectUserTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.TypeRegistration.class, modelsContext, (usage) -> collectUserTypeRegistration(
				classDetailsRegistry.resolveClassDetails( usage.basicClass().getName() ),
				classDetailsRegistry.resolveClassDetails( usage.userType().getName() )
		) );
	}

	public void collectUserTypeRegistrations(List<JaxbUserTypeRegistrationImpl> registrations) {
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
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.CompositeTypeRegistration.class, modelsContext, (usage) -> collectCompositeUserTypeRegistration(
				classDetailsRegistry.resolveClassDetails( usage.embeddableClass().getName() ),
				classDetailsRegistry.resolveClassDetails( usage.userType().getName() )
		) );
	}

	public void collectCompositeUserTypeRegistrations(List<JaxbCompositeUserTypeRegistrationImpl> registrations) {
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
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.CollectionTypeRegistration.class, modelsContext, (usage) -> collectCollectionTypeRegistration(
				usage.classification(),
				classDetailsRegistry.resolveClassDetails( usage.type().getName() ),
				extractParameterMap( usage )
		) );
	}

	private Map<String,String> extractParameterMap(org.hibernate.annotations.CollectionTypeRegistration source) {
		final Parameter[] parameters = source.parameters();
		if ( parameters.length == 0 ) {
			return Collections.emptyMap();
		}
		final Map<String,String> result = new HashMap<>();
		for ( Parameter parameter : parameters ) {
			result.put( parameter.name(), parameter.value() );
		}
		return result;
	}

	public void collectCollectionTypeRegistrations(List<JaxbCollectionUserTypeRegistrationImpl> registrations) {
		if ( CollectionHelper.isEmpty( registrations ) ) {
			return;
		}

		registrations.forEach( (reg) -> collectCollectionTypeRegistration(
				reg.getClassification(),
				classDetailsRegistry.resolveClassDetails( reg.getDescriptor() ),
				extractParameterMap( reg.getParameters() )
		) );
	}

	private Map<String, String> extractParameterMap(List<JaxbConfigurationParameterImpl> parameters) {
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
		annotationTarget.forEachAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiatorRegistration.class, modelsContext, (usage) -> collectEmbeddableInstantiatorRegistration(
				classDetailsRegistry.resolveClassDetails( usage.embeddableClass().getName() ),
				classDetailsRegistry.resolveClassDetails( usage.instantiator().getName() )
		) );
	}

	public void collectEmbeddableInstantiatorRegistrations(List<JaxbEmbeddableInstantiatorRegistrationImpl> registrations) {
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
		annotationTarget.forEachAnnotationUsage( FilterDef.class, modelsContext, (usage) -> collectFilterDefinition(
				usage.name(),
				usage.defaultCondition(),
				extractFilterParameters( usage )
		) );
	}

	private Map<String, ClassDetails> extractFilterParameters(FilterDef source) {
		final ParamDef[] parameters = source.parameters();
		if ( parameters.length == 0 ) {
			return null;
		}

		final Map<String, ClassDetails> result = new HashMap<>( parameters.length );
		for ( ParamDef parameter : parameters ) {
			result.put( parameter.name(), classDetailsRegistry.resolveClassDetails( parameter.type().getName() ) );
		}
		return result;
	}

	public void collectFilterDefinitions(List<JaxbFilterDefImpl> filterDefinitions) {
		if ( CollectionHelper.isEmpty( filterDefinitions ) ) {
			return;
		}

		filterDefinitions.forEach( (filterDefinition) -> collectFilterDefinition(
				filterDefinition.getName(),
				filterDefinition.getDefaultCondition(),
				extractFilterParameters( filterDefinition )
		) );
	}

	private Map<String, ClassDetails> extractFilterParameters(JaxbFilterDefImpl source) {
		final List<JaxbFilterDefImpl.JaxbFilterParamImpl> parameters = source.getFilterParams();
		if ( isEmpty( parameters ) ) {
			return null;
		}

		final Map<String, ClassDetails> result = new HashMap<>( parameters.size() );
		for ( JaxbFilterDefImpl.JaxbFilterParamImpl parameter : parameters ) {
			// for now, don't check whether nothing was specified; this situation
			// should resolve to Object - let's see how that reacts
			final ClassDetails targetClassDetails = XmlAnnotationHelper.resolveJavaType(
					parameter.getType(),
					classDetailsRegistry
			);
			result.put( parameter.getName(), targetClassDetails );
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

	public void collectEntityListenerRegistrations(List<JaxbEntityListenerImpl> listeners) {
		if ( CollectionHelper.isEmpty( listeners ) ) {
			return;
		}

		listeners.forEach( (jaxbEntityListener) -> {
			final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( jaxbEntityListener.getClazz() );
			final JpaEventListener listener = JpaEventListener.from(
					JpaEventListenerStyle.LISTENER,
					classDetails,
					jaxbEntityListener
			);
			addJpaEventListener( listener );
		} );
	}

	public void addJpaEventListener(JpaEventListener listener) {
		if ( jpaEventListeners == null ) {
			jpaEventListeners = new ArrayList<>();
		}

		jpaEventListeners.add( listener );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Id generators

	public void collectIdGenerators(JaxbEntityMappingsImpl jaxbRoot) {
		collectSequenceGenerators( jaxbRoot.getSequenceGenerators() );
		collectTableGenerators( jaxbRoot.getTableGenerators() );
		collectGenericGenerators( jaxbRoot.getGenericGenerators() );

		// todo : add support for @IdGeneratorType in mapping.xsd?
	}

	public void collectIdGenerators(ClassDetails classDetails) {
		classDetails.forEachAnnotationUsage( SequenceGenerator.class, modelsContext, this::collectSequenceGenerator );
		classDetails.forEachAnnotationUsage( TableGenerator.class, modelsContext, this::collectTableGenerator );
		classDetails.forEachAnnotationUsage( GenericGenerator.class, modelsContext, this::collectGenericGenerator );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sequence generator

	public void collectSequenceGenerators(List<JaxbSequenceGeneratorImpl> sequenceGenerators) {
		if ( CollectionHelper.isEmpty( sequenceGenerators ) ) {
			return;
		}

		sequenceGenerators.forEach( (generator) -> {
			final SequenceGenerator annotationUsage = makeAnnotation(
					SequenceGenerator.class,
					Map.of(
							"name", generator.getName(),
							"sequenceName", generator.getSequenceName(),
							"catalog", generator.getCatalog(),
							"schema", generator.getSchema(),
							"initialValue", generator.getInitialValue(),
							"allocationSize", generator.getAllocationSize()
					)
			);

			collectSequenceGenerator( new SequenceGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	public void collectSequenceGenerator(SequenceGenerator usage) {
		collectSequenceGenerator( new SequenceGeneratorRegistration( usage.name(), usage ) );
	}

	public void collectSequenceGenerator(SequenceGeneratorRegistration generatorRegistration) {
		if ( sequenceGeneratorRegistrations == null ) {
			sequenceGeneratorRegistrations = new HashMap<>();
		}

		sequenceGeneratorRegistrations.put( generatorRegistration.name(), generatorRegistration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Table generator

	public void collectTableGenerators(List<JaxbTableGeneratorImpl> tableGenerators) {
		if ( CollectionHelper.isEmpty( tableGenerators ) ) {
			return;
		}

		tableGenerators.forEach( (generator) -> {
			final TableGenerator annotationUsage = makeAnnotation(
					TableGenerator.class,
					Map.of(
							"name", generator.getName(),
							"table", generator.getTable(),
							"catalog", generator.getCatalog(),
							"schema", generator.getSchema(),
							"pkColumnName", generator.getPkColumnName(),
							"valueColumnName", generator.getValueColumnName(),
							"pkColumnValue", generator.getPkColumnValue(),
							"initialValue", generator.getInitialValue(),
							"allocationSize", generator.getAllocationSize()
					)
			);

			collectTableGenerator( new TableGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	public void collectTableGenerator(TableGenerator usage) {
		collectTableGenerator( new TableGeneratorRegistration( usage.name(), usage ) );
	}

	public void collectTableGenerator(TableGeneratorRegistration generatorRegistration) {
		if ( tableGeneratorRegistrations == null ) {
			tableGeneratorRegistrations = new HashMap<>();
		}

		tableGeneratorRegistrations.put( generatorRegistration.name(), generatorRegistration );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Generic generators

	private void collectGenericGenerators(List<JaxbGenericIdGeneratorImpl> genericGenerators) {
		if ( CollectionHelper.isEmpty( genericGenerators ) ) {
			return;
		}

		genericGenerators.forEach( (generator) -> {
			final GenericGenerator annotationUsage = makeAnnotation(
					GenericGenerator.class,
					Map.of(
							"name", generator.getName(),
							"strategy", generator.getClazz()
					)
			);

			// todo : update the mapping.xsd to account for new @GenericGenerator definition

			collectGenericGenerator( new GenericGeneratorRegistration( generator.getName(), annotationUsage ) );
		} );
	}

	public void collectGenericGenerator(GenericGenerator usage) {
		collectGenericGenerator( new GenericGeneratorRegistration( usage.name(), usage ) );
	}

	public void collectGenericGenerator(GenericGeneratorRegistration generatorRegistration) {
		if ( genericGeneratorRegistrations == null ) {
			genericGeneratorRegistrations = new HashMap<>();
		}

		genericGeneratorRegistrations.put( generatorRegistration.name(), generatorRegistration );
	}

	@SuppressWarnings("unchecked")
	private static <A extends Annotation> A makeAnnotation(Class<A> annotationType, Map<String, ?> values) {
		final InvocationHandler handler = (proxy, method, args) -> {
			final String name = method.getName();
			if ( method.getParameterCount() == 0 ) {
				if ( name.equals( "annotationType" ) ) {
					return annotationType;
				}
				if ( values.containsKey( name ) && values.get( name ) != null ) {
					return values.get( name );
				}
				final Object defaultValue = method.getDefaultValue();
				if ( defaultValue != null ) {
					return defaultValue;
				}
			}
			if ( name.equals( "toString" ) ) {
				return annotationType.getName() + values;
			}
			if ( name.equals( "hashCode" ) ) {
				return values.hashCode();
			}
			if ( name.equals( "equals" ) ) {
				return proxy == args[0];
			}
			throw new UnsupportedOperationException( "No value available for " + annotationType.getName() + "." + name );
		};
		return (A) Proxy.newProxyInstance( annotationType.getClassLoader(), new Class<?>[] { annotationType }, handler );
	}
}
