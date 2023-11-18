/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.xml.internal.attr;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCascadeTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.orm.categorize.xml.internal.XmlAnnotationHelper;
import org.hibernate.models.orm.categorize.xml.internal.XmlProcessingHelper;
import org.hibernate.models.orm.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.AnnotationDescriptor;

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
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbManyToOne.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbManyToOne.getName(),
				accessType,
				declarer
		);

		final MutableAnnotationUsage<ManyToOne> manyToOneAnn = applyManyToOne(
				memberDetails,
				jaxbManyToOne,
				xmlDocumentContext
		);

		CommonProcessing.applyFetching( memberDetails, jaxbManyToOne, manyToOneAnn );
		CommonProcessing.applyOptimisticLock( memberDetails, jaxbManyToOne, manyToOneAnn );
		CommonProcessing.applyAttributeAccessor( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		applyJoinColumns( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		applyNotFound( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		applyOnDelete( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		applyTarget( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		applyCascading( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );

		return memberDetails;
	}

	private static MutableAnnotationUsage<ManyToOne> applyManyToOne(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			XmlDocumentContext xmlDocumentContext) {
		// todo : apply the @ManyToOne annotation

		final MutableAnnotationUsage<ManyToOne> manyToOneAnn = getOrMakeAnnotation( ManyToOne.class, memberDetails );
		final AnnotationDescriptor<ManyToOne> manyToOneDescriptor = xmlDocumentContext
				.getModelBuildingContext()
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

	private static void applyJoinColumns(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isNotEmpty( jaxbManyToOne.getJoinColumns() ) ) {
			final List<MutableAnnotationUsage<JoinColumn>> joinColumns = new ArrayList<>( jaxbManyToOne.getJoinColumns().size() );
			manyToOneAnn.setAttributeValue( "joinColumns", joinColumns );
			for ( int i = 0; i < jaxbManyToOne.getJoinColumns().size(); i++ ) {
				final JaxbJoinColumnImpl jaxbJoinColumn = jaxbManyToOne.getJoinColumns().get( i );
				final MutableAnnotationUsage<JoinColumn> joinColumnAnn = XmlAnnotationHelper.applyJoinColumn(
						jaxbJoinColumn,
						memberDetails,
						xmlDocumentContext
				);
				joinColumns.add( joinColumnAnn );
			}
		}
		else {
			manyToOneAnn.setAttributeValue( "joinColumns", List.of( makeAnnotation( JoinColumn.class, memberDetails ) ) );
		}
	}

	private static void applyNotFound(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		final NotFoundAction notFoundAction = jaxbManyToOne.getNotFound();
		if ( notFoundAction == null ) {
			return;
		}

		final MutableAnnotationUsage<NotFound> notFoundAnn = makeAnnotation( NotFound.class, memberDetails );
		notFoundAnn.setAttributeValue( "action", notFoundAction );
	}

	@SuppressWarnings("unused")
	private static void applyOnDelete(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		final OnDeleteAction action = jaxbManyToOne.getOnDelete();
		if ( action == null ) {
			return;
		}

		final MutableAnnotationUsage<OnDelete> notFoundAnn = makeAnnotation( OnDelete.class, memberDetails );
		notFoundAnn.setAttributeValue( "action", action );
	}

	@SuppressWarnings("unused")
	private static void applyTarget(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		final String targetEntityName = jaxbManyToOne.getTargetEntity();
		if ( StringHelper.isEmpty( targetEntityName ) ) {
			return;
		}

		final MutableAnnotationUsage<Target> targetAnn = makeAnnotation( Target.class, memberDetails );
		targetAnn.setAttributeValue( "value", targetEntityName );
	}

	@SuppressWarnings("unused")
	private static void applyCascading(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbCascadeTypeImpl cascadeContainer = jaxbManyToOne.getCascade();
		if ( cascadeContainer == null ) {
			return;
		}

		final EnumSet<CascadeType> cascadeTypes;

		if ( cascadeContainer.getCascadeAll() != null ) {
			cascadeTypes = EnumSet.allOf( CascadeType.class );
		}
		else {
			cascadeTypes = EnumSet.noneOf( CascadeType.class );
			if ( cascadeContainer.getCascadePersist() != null ) {
				cascadeTypes.add( CascadeType.PERSIST );
			}
			if ( cascadeContainer.getCascadeMerge() != null ) {
				cascadeTypes.add( CascadeType.MERGE );
			}
			if ( cascadeContainer.getCascadeRemove() != null ) {
				cascadeTypes.add( CascadeType.REMOVE );
			}
			if ( cascadeContainer.getCascadeLock() != null ) {
				cascadeTypes.add( CascadeType.LOCK );
			}
			if ( cascadeContainer.getCascadeRefresh() != null ) {
				cascadeTypes.add( CascadeType.REFRESH );
			}
			if ( cascadeContainer.getCascadeReplicate() != null ) {
				//noinspection deprecation
				cascadeTypes.add( CascadeType.REPLICATE );
			}
			if ( cascadeContainer.getCascadeDetach() != null ) {
				cascadeTypes.add( CascadeType.DETACH );
			}
		}

		manyToOneAnn.setAttributeValue( "cascade", asList( cascadeTypes ) );
	}

	private static <E extends Enum<E>> List<E> asList(EnumSet<E> enums) {
		final List<E> list = CollectionHelper.arrayList( enums.size() );
		list.addAll( enums );
		return list;
	}
}
