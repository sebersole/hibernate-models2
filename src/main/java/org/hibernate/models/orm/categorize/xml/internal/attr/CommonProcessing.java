/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLockableAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularAssociationAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularFetchModeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbStandardAttribute;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.FetchType;

import static org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper.makeAnnotation;

/**
 * @author Steve Ebersole
 */
public class CommonProcessing {
	public static <A extends Annotation> void applyOptimisticLock(
			MutableMemberDetails memberDetails,
			JaxbLockableAttribute jaxbAttribute,
			MutableAnnotationUsage<A> attributeAnn) {
		final boolean includeInOptimisticLock = jaxbAttribute.isOptimisticLock();
		final MutableAnnotationUsage<OptimisticLock> optLockAnn = makeAnnotation( OptimisticLock.class, memberDetails );
		optLockAnn.setAttributeValue( "excluded", !includeInOptimisticLock );
	}

	public static <A extends Annotation> void applyAttributeAccessor(
			MutableMemberDetails memberDetails,
			JaxbPersistentAttribute jaxbAttribute,
			MutableAnnotationUsage<A> attributeAnn,
			XmlDocumentContext xmlDocumentContext) {
		final String attributeAccessor = jaxbAttribute.getAttributeAccessor();
		if ( attributeAccessor == null ) {
			return;
		}

		final ClassDetails strategyClassDetails = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry()
				.getClassDetails( attributeAccessor );
		final MutableAnnotationUsage<AttributeAccessor> accessAnn = makeAnnotation( AttributeAccessor.class, memberDetails );
		accessAnn.setAttributeValue( "strategy", strategyClassDetails );
	}

	public static <A extends Annotation> void applyFetching(
			MutableMemberDetails memberDetails,
			JaxbStandardAttribute jaxbAttribute,
			MutableAnnotationUsage<A> attributeAnn) {
		final FetchType fetchType = jaxbAttribute.getFetch();
		if ( fetchType != null ) {
			attributeAnn.setAttributeValue( "fetch", fetchType );
		}

		if ( jaxbAttribute instanceof JaxbSingularAssociationAttribute jaxbSingularAttribute ) {
			final JaxbSingularFetchModeImpl jaxbFetchMode = jaxbSingularAttribute.getFetchMode();
			applyFetchMode( memberDetails, jaxbFetchMode );
		}
		else if ( jaxbAttribute instanceof JaxbAnyMappingImpl jaxbAnyAttribute ) {
			final JaxbSingularFetchModeImpl jaxbFetchMode = jaxbAnyAttribute.getFetchMode();
			applyFetchMode( memberDetails, jaxbFetchMode );
		}
	}

	private static void applyFetchMode(MutableMemberDetails memberDetails, JaxbSingularFetchModeImpl jaxbFetchMode) {
		if ( jaxbFetchMode != null ) {
			final FetchMode fetchMode = FetchMode.valueOf( jaxbFetchMode.value() );
			final MutableAnnotationUsage<Fetch> fetchAnn = makeAnnotation( Fetch.class, memberDetails );
			fetchAnn.setAttributeValue( "value", fetchMode );
		}
	}
}
