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
import java.util.Set;

import org.hibernate.annotations.Parameter;
import org.hibernate.boot.jaxb.mapping.JaxbCollectionUserTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbCompositeUserTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbConfigurationParameter;
import org.hibernate.boot.jaxb.mapping.JaxbConverterRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddableInstantiatorRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbEntityListener;
import org.hibernate.boot.jaxb.mapping.JaxbJavaTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbJdbcTypeRegistration;
import org.hibernate.boot.jaxb.mapping.JaxbUserTypeRegistration;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.spi.ManagedResources;
import org.hibernate.models.orm.spi.ProcessResult;
import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.orm.spi.Processor;
import org.hibernate.models.source.spi.AnnotationTarget;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hibernate.models.orm.spi.HibernateAnnotations.COLLECTION_TYPE_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.COMPOSITE_TYPE_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.CONVERTER_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.EMBEDDABLE_INSTANTIATOR_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.JAVA_TYPE_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.JDBC_TYPE_REG;
import static org.hibernate.models.orm.spi.HibernateAnnotations.TYPE_REG;

/**
 * In-flight holder for various types of "global" registrations.  Also acts as the
 * {@linkplain #createResult builder} for {@linkplain ProcessResult} as returned
 * by {@linkplain org.hibernate.models.orm.spi.Processor#process}
 *
 * @author Steve Ebersole
 */
public class ProcessResultCollector {
	private final ClassDetailsRegistry classDetailsRegistry;

	private List<EntityListenerRegistration> entityListenerRegistrations;
	private List<ClassDetails> autoAppliedConverters;
	private List<ConversionRegistration> converterRegistrations;
	private List<JavaTypeRegistration> javaTypeRegistrations;
	private List<JdbcTypeRegistration> jdbcTypeRegistrations;
	private List<UserTypeRegistration> userTypeRegistrations;
	private List<CompositeUserTypeRegistration> compositeUserTypeRegistrations;
	private List<CollectionTypeRegistration> collectionTypeRegistrations;
	private List<EmbeddableInstantiatorRegistration> embeddableInstantiatorRegistrations;

	private Map<String, IdGeneratorRegistration> globalIdGeneratorRegistrations;
	private Map<String, NamedQueryRegistration> jpaNamedQueries;
	private Map<String, NamedQueryRegistration> hibernateNamedHqlQueries;
	private Map<String, NamedQueryRegistration> hibernateNamedNativeQueries;

	public ProcessResultCollector(SourceModelBuildingContext buildingContext) {
		this( buildingContext.getClassDetailsRegistry() );
	}
	public ProcessResultCollector(ClassDetailsRegistry classDetailsRegistry) {
		this.classDetailsRegistry = classDetailsRegistry;
	}

	public List<EntityListenerRegistration> getEntityListenerRegistrations() {
		return entityListenerRegistrations;
	}

	public List<ConversionRegistration> getConverterRegistrations() {
		return converterRegistrations;
	}

	public List<JavaTypeRegistration> getJavaTypeRegistrations() {
		return javaTypeRegistrations;
	}

	public List<JdbcTypeRegistration> getJdbcTypeRegistrations() {
		return jdbcTypeRegistrations;
	}

	public List<UserTypeRegistration> getUserTypeRegistrations() {
		return userTypeRegistrations;
	}

	public List<CompositeUserTypeRegistration> getCompositeUserTypeRegistrations() {
		return compositeUserTypeRegistrations;
	}

	public List<CollectionTypeRegistration> getCollectionTypeRegistrations() {
		return collectionTypeRegistrations;
	}

	public List<EmbeddableInstantiatorRegistration> getEmbeddableInstantiatorRegistrations() {
		return embeddableInstantiatorRegistrations;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JavaTypeRegistration

	public void collectJavaTypeRegistrations(AnnotationTarget annotationTarget) {
		annotationTarget.forEachUsage( JAVA_TYPE_REG, (usage) -> collectJavaTypeRegistration(
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
		annotationTarget.forEachUsage( JDBC_TYPE_REG, (usage) -> collectJdbcTypeRegistration(
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
		annotationTarget.forEachUsage( CONVERTER_REG, (usage) -> {
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
		annotationTarget.forEachUsage( TYPE_REG, (usage) -> collectUserTypeRegistration(
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
		annotationTarget.forEachUsage( COMPOSITE_TYPE_REG, (usage) -> collectCompositeUserTypeRegistration(
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
		annotationTarget.forEachUsage( COLLECTION_TYPE_REG, (usage) -> collectCollectionTypeRegistration(
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
		annotationTarget.forEachUsage( EMBEDDABLE_INSTANTIATOR_REG, (usage) -> collectEmbeddableInstantiatorRegistration(
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
	// EntityListenerRegistration

	public void collectEntityListenerRegistrations(List<JaxbEntityListener> listeners) {
		if ( CollectionHelper.isEmpty( listeners ) ) {
			return;
		}

		listeners.forEach( (listener) -> EntityListenerRegistration.from( listener, classDetailsRegistry ) );
	}


	public void collectGlobalIdGeneratorRegistration(
			String name,
			IdGeneratorRegistration.Kind kind,
			AnnotationUsage<? extends Annotation> annotation) {
		if ( globalIdGeneratorRegistrations == null ) {
			globalIdGeneratorRegistrations = new HashMap<>();
		}

		globalIdGeneratorRegistrations.put( name, new IdGeneratorRegistration( name, kind, annotation ) );
	}

	/**
	 * Builder for {@linkplain ProcessResult} based on our internal state plus
	 * the incoming set of entity hierarchies.
	 *
	 * @param entityHierarchies Hierarchies to be {@linkplain ProcessResult#getEntityHierarchies() included}
	 * in the result
	 *
	 * @see org.hibernate.models.orm.spi.Processor#process
	 */
	public ProcessResult createResult(Set<EntityHierarchy> entityHierarchies) {
		return new ProcessResultImpl(
				entityHierarchies,
				javaTypeRegistrations == null ? emptyList() : javaTypeRegistrations,
				jdbcTypeRegistrations == null ? emptyList() : jdbcTypeRegistrations,
				converterRegistrations == null ? emptyList() : converterRegistrations,
				autoAppliedConverters == null ? emptyList() : autoAppliedConverters,
				userTypeRegistrations == null ? emptyList() : userTypeRegistrations,
				compositeUserTypeRegistrations == null ? emptyList() : compositeUserTypeRegistrations,
				collectionTypeRegistrations == null ? emptyList() : collectionTypeRegistrations,
				embeddableInstantiatorRegistrations == null ? emptyList() : embeddableInstantiatorRegistrations,
				globalIdGeneratorRegistrations == null ? emptyMap() : globalIdGeneratorRegistrations,
				jpaNamedQueries == null ? emptyMap() : jpaNamedQueries,
				hibernateNamedHqlQueries == null ? emptyMap() : hibernateNamedHqlQueries,
				hibernateNamedNativeQueries == null ? emptyMap() : hibernateNamedNativeQueries
		);
	}
}
