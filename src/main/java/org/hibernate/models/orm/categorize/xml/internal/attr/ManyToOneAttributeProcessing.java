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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCascadeTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularFetchModeImpl;
import org.hibernate.internal.util.StringHelper;
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

		applyJoinColumns( memberDetails, jaxbManyToOne, manyToOneAnn, sourceModelBuildingContext );
		applyNotFound( memberDetails, jaxbManyToOne, manyToOneAnn, sourceModelBuildingContext );
		applyOnDelete( memberDetails, jaxbManyToOne, manyToOneAnn, sourceModelBuildingContext );
		applyFetching( memberDetails, jaxbManyToOne, manyToOneAnn, sourceModelBuildingContext );
		applyOptimisticLock( memberDetails, jaxbManyToOne, manyToOneAnn, sourceModelBuildingContext );
		applyTarget( memberDetails, jaxbManyToOne, manyToOneAnn, sourceModelBuildingContext );
		applyCascading( memberDetails, jaxbManyToOne, manyToOneAnn, sourceModelBuildingContext );

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

	private static void applyJoinColumns(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			SourceModelBuildingContext sourceModelBuildingContext) {
		if ( CollectionHelper.isNotEmpty( jaxbManyToOne.getJoinColumns() ) ) {
			final List<MutableAnnotationUsage<JoinColumn>> joinColumns = new ArrayList<>( jaxbManyToOne.getJoinColumns().size() );
			manyToOneAnn.setAttributeValue( "joinColumns", joinColumns );
			for ( int i = 0; i < jaxbManyToOne.getJoinColumns().size(); i++ ) {
				final JaxbJoinColumnImpl jaxbJoinColumn = jaxbManyToOne.getJoinColumns().get( i );
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

	@SuppressWarnings("unused")
	private static void applyNotFound(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			SourceModelBuildingContext sourceModelBuildingContext) {
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
			SourceModelBuildingContext sourceModelBuildingContext) {
		final OnDeleteAction action = jaxbManyToOne.getOnDelete();
		if ( action == null ) {
			return;
		}

		final MutableAnnotationUsage<OnDelete> notFoundAnn = makeAnnotation( OnDelete.class, memberDetails );
		notFoundAnn.setAttributeValue( "action", action );
	}

	@SuppressWarnings("unused")
	private static void applyFetching(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final JaxbSingularFetchModeImpl jaxbFetchMode = jaxbManyToOne.getFetchMode();
		if ( jaxbFetchMode == null ) {
			return;
		}

		final FetchMode fetchMode = FetchMode.valueOf( jaxbFetchMode.value() );
		final MutableAnnotationUsage<Fetch> fetchAnn = makeAnnotation( Fetch.class, memberDetails );
		fetchAnn.setAttributeValue( "value", fetchMode );
	}

	@SuppressWarnings("unused")
	private static void applyOptimisticLock(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final boolean includeInOptimisticLock = jaxbManyToOne.isOptimisticLock();
		final MutableAnnotationUsage<OptimisticLock> optLockAnn = makeAnnotation( OptimisticLock.class, memberDetails );
		optLockAnn.setAttributeValue( "excluded", !includeInOptimisticLock );
	}

	@SuppressWarnings("unused")
	private static void applyTarget(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			MutableAnnotationUsage<ManyToOne> manyToOneAnn,
			SourceModelBuildingContext sourceModelBuildingContext) {
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
			SourceModelBuildingContext sourceModelBuildingContext) {
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
