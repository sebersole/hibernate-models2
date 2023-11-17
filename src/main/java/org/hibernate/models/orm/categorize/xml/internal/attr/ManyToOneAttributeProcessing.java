/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.internal.XmlAnnotationHelper;
import org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.AccessType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.hibernate.internal.util.NullnessHelper.coalesce;
import static org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper.getOrMakeAnnotation;
import static org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper.makeAnnotation;

/**
 * @author Steve Ebersole
 */
public class ManyToOneAttributeProcessing {

	public static MutableMemberDetails processManyToOneAttribute(
			JaxbManyToOneImpl jaxbManyToOne,
			MutableClassDetails declarer,
			AccessType classAccessType,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final AccessType accessType = coalesce( jaxbManyToOne.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbManyToOne.getName(),
				accessType,
				declarer,
				sourceModelBuildingContext
		);

		final MutableAnnotationUsage<ManyToOne> manyToOneAnn = applyManyToOne(
				memberDetails,
				jaxbManyToOne,
				sourceModelBuildingContext
		);
		applyJoinColumn( memberDetails, jaxbManyToOne, manyToOneAnn, sourceModelBuildingContext );

		return memberDetails;
	}

	private static MutableAnnotationUsage<ManyToOne> applyManyToOne(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			SourceModelBuildingContext sourceModelBuildingContext) {
		// todo : apply the @ManyToOne annotation

		final MutableAnnotationUsage<ManyToOne> manyToOneAnn = getOrMakeAnnotation( ManyToOne.class, memberDetails );
		final AnnotationDescriptor<ManyToOne> manyToOneDescriptor = sourceModelBuildingContext
				.getAnnotationDescriptorRegistry()
				.getDescriptor( ManyToOne.class );

		XmlAnnotationHelper.applyOr(
				jaxbManyToOne,
				JaxbManyToOneImpl::getFetch,
				"fetch",
				manyToOneAnn,
				manyToOneDescriptor
		);

		return manyToOneAnn;
	}

	private static void applyJoinColumn(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( CollectionHelper.isNotEmpty( jaxbManyToOne.getJoinColumn() ) ) {
			final List<MutableAnnotationUsage<JoinColumn>> joinColumns = new ArrayList<>( jaxbManyToOne.getJoinColumn().size() );
			manyToOneAnn.setAttributeValue( "joinColumns", joinColumns );
			for ( int i = 0; i < jaxbManyToOne.getJoinColumn().size(); i++ ) {
				final JaxbJoinColumnImpl jaxbJoinColumn = jaxbManyToOne.getJoinColumn().get( i );
				final MutableAnnotationUsage<JoinColumn> joinColumnAnn = XmlAnnotationHelper.applyJoinColumn(
						jaxbJoinColumn,
						memberDetails,
						sourceModelBuildingContext
				);
				joinColumns.add( joinColumnAnn );
			}
		}
		else {
			manyToOneAnn.setAttributeValue( "joinColumns", List.of( makeAnnotation( JoinColumn.class, memberDetails ) ) );
		}
	}
}
