/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.internal;

import java.beans.Introspector;
import java.lang.annotation.Annotation;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.MemberResolutionException;
import org.hibernate.models.source.internal.MutableAnnotationTarget;
import org.hibernate.models.source.internal.MutableAnnotationUsage;
import org.hibernate.models.source.internal.MutableClassDetails;
import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.internal.dynamic.DynamicAnnotationUsage;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.FieldDetails;
import org.hibernate.models.source.spi.MethodDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;

/**
 * Common helper utilities for handling mapping XML processing
 *
 * @author Steve Ebersole
 */
public class XmlProcessingHelper {
	/**
	 * Determine the name of a class defined in XML, accounting for {@code <package/>}
	 *
	 * @param jaxbRoot The {@code <entity-mappings/>} node for access to the package (if one)
	 * @param jaxbManagedType The class JAXB node
	 */
	public static String determineClassName(JaxbEntityMappingsImpl jaxbRoot, JaxbManagedType jaxbManagedType) {
		if ( StringHelper.isQualified( jaxbManagedType.getClazz() ) ) {
			return jaxbManagedType.getClazz();
		}

		return StringHelper.qualify( jaxbManagedType.getClazz(), jaxbRoot.getPackage() );
	}

	public static AccessType inverse(AccessType accessType) {
		return accessType == AccessType.FIELD ? AccessType.PROPERTY : AccessType.FIELD;
	}

	/**
	 * Find the member backing the named attribute
	 */
	public static MutableMemberDetails getAttributeMember(
			String attributeName,
			AccessType accessType,
			MutableClassDetails classDetails,
			SourceModelBuildingContext buildingContext) {
		final MutableMemberDetails result = findAttributeMember(
				attributeName,
				accessType,
				classDetails,
				buildingContext
		);
		if ( result == null ) {
			throw new MemberResolutionException(
					String.format(
							"Could not locate attribute member - %s (%s)",
							attributeName,
							classDetails.getName()
					)
			);
		}
		return result;
	}

	/**
	 * Find the member backing the named attribute
	 */
	public static MutableMemberDetails findAttributeMember(
			String attributeName,
			AccessType accessType,
			MutableClassDetails classDetails,
			SourceModelBuildingContext buildingContext) {
		if ( accessType == AccessType.PROPERTY ) {
			for ( int i = 0; i < classDetails.getMethods().size(); i++ ) {
				final MethodDetails methodDetails = classDetails.getMethods().get( i );
				if ( methodDetails.getMethodKind() == MethodDetails.MethodKind.GETTER ) {
					if ( methodDetails.getName().startsWith( "get" ) ) {
						final String stemName = methodDetails.getName().substring( 3 );
						final String decapitalizedStemName = Introspector.decapitalize( stemName );
						if ( stemName.equals( attributeName ) || decapitalizedStemName.equals( attributeName ) ) {
							return (MutableMemberDetails) methodDetails;
						}
					}
					else if ( methodDetails.getName().startsWith( "is" ) ) {
						final String stemName = methodDetails.getName().substring( 2 );
						final String decapitalizedStemName = Introspector.decapitalize( stemName );
						if ( stemName.equals( attributeName ) || decapitalizedStemName.equals( attributeName ) ) {
							return (MutableMemberDetails) methodDetails;
						}
					}
				}
			}
		}
		else {
			assert accessType == AccessType.FIELD;
			for ( int i = 0; i < classDetails.getFields().size(); i++ ) {
				final FieldDetails fieldDetails = classDetails.getFields().get( i );
				if ( fieldDetails.getName().equals( attributeName ) ) {
					return (MutableMemberDetails) fieldDetails;
				}
			}
		}

		return null;
	}

	/**
	 * Find an existing annotation, or create one.
	 * Used when applying XML in override mode.
	 */
	public static <A extends Annotation> MutableAnnotationUsage<A> getOrMakeAnnotation(
			Class<A> annotationType,
			MutableAnnotationTarget target) {
		final AnnotationUsage<A> existing = target.getAnnotationUsage( annotationType );
		if ( existing != null ) {
			return (MutableAnnotationUsage<A>) existing;
		}

		return makeAnnotation( annotationType, target );
	}

	/**
	 * Make an AnnotationUsage.
	 * Used when applying XML in complete mode or when {@linkplain #getOrMakeAnnotation}
	 * needs to make.
	 */
	public static <A extends Annotation> DynamicAnnotationUsage<A> makeAnnotation(
			Class<A> annotationType,
			MutableAnnotationTarget target) {
		final DynamicAnnotationUsage<A> created = new DynamicAnnotationUsage<>( annotationType, target );
		target.addAnnotationUsage( created );
		return created;
	}

	/**
	 * Find an existing annotation by name, or create one.
	 * Used when applying XML in override mode.
	 */
	public static <A extends Annotation> MutableAnnotationUsage<A> getOrMakeNamedAnnotation(
			Class<A> annotationType,
			String name,
			MutableAnnotationTarget target) {
		return getOrMakeNamedAnnotation( annotationType, name, "name", target );
	}

	/**
	 * Find an existing annotation by name, or create one.
	 * Used when applying XML in override mode.
	 */
	public static <A extends Annotation> MutableAnnotationUsage<A> getOrMakeNamedAnnotation(
			Class<A> annotationType,
			String name,
			String attributeToMatch,
			MutableAnnotationTarget target) {
		if ( name == null ) {
			return makeAnnotation( annotationType, target );
		}

		final AnnotationUsage<A> existing = target.getNamedAnnotationUsage( annotationType, name, attributeToMatch );
		if ( existing != null ) {
			return (MutableAnnotationUsage<A>) existing;
		}

		return makeNamedAnnotation( annotationType, name, attributeToMatch, target );
	}

	/**
	 * Make a named AnnotationUsage.
	 * Used when applying XML in complete mode or when {@linkplain #getOrMakeNamedAnnotation}
	 * needs to make.
	 */
	public static <A extends Annotation> DynamicAnnotationUsage<A> makeNamedAnnotation(
			Class<A> annotationType,
			String name,
			String nameAttributeName,
			MutableAnnotationTarget target) {
		final DynamicAnnotationUsage<A> created = new DynamicAnnotationUsage<>( annotationType, target );
		target.addAnnotationUsage( created );
		created.setAttributeValue( nameAttributeName, name );
		return created;
	}
}
