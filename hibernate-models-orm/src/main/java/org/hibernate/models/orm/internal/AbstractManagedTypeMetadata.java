/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.models.internal.IndexedConsumer;
import org.hibernate.models.orm.MultipleAttributeNaturesException;
import org.hibernate.models.orm.spi.AttributeMetadata;
import org.hibernate.models.orm.spi.HibernateAnnotations;
import org.hibernate.models.orm.spi.JpaAnnotations;
import org.hibernate.models.orm.spi.ManagedTypeMetadata;
import org.hibernate.models.orm.spi.OrmModelBuildingContext;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.MemberDetails;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.hibernate.models.internal.CollectionHelper.arrayList;
import static org.hibernate.models.orm.internal.OrmModelLogging.ORM_MODEL_LOGGER;

/**
 * Models metadata about a JPA {@linkplain jakarta.persistence.metamodel.ManagedType managed-type}.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class AbstractManagedTypeMetadata implements ManagedTypeMetadata {
	private final ClassDetails classDetails;
	private final OrmModelBuildingContext modelContext;

	private final AttributePath attributePathBase;
	private final AttributeRole attributeRoleBase;

	/**
	 * This form is intended for construction of the root of an entity hierarchy
	 * and its mapped-superclasses
	 */
	public AbstractManagedTypeMetadata(ClassDetails classDetails, OrmModelBuildingContext modelContext) {
		this.classDetails = classDetails;
		this.modelContext = modelContext;
		this.attributeRoleBase = new AttributeRole( classDetails.getName() );
		this.attributePathBase = new AttributePath();
	}

	/**
	 * This form is used to create Embedded references
	 *
	 * @param classDetails The Embeddable descriptor
	 * @param attributeRoleBase The base for the roles of attributes created *from* here
	 * @param attributePathBase The base for the paths of attributes created *from* here
	 */
	public AbstractManagedTypeMetadata(
			ClassDetails classDetails,
			AttributeRole attributeRoleBase,
			AttributePath attributePathBase,
			OrmModelBuildingContext modelContext) {
		this.classDetails = classDetails;
		this.modelContext = modelContext;
		this.attributeRoleBase = attributeRoleBase;
		this.attributePathBase = attributePathBase;
	}

	public ClassDetails getClassDetails() {
		return classDetails;
	}

	public OrmModelBuildingContext getModelContext() {
		return modelContext;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		AbstractManagedTypeMetadata that = (AbstractManagedTypeMetadata) o;
		return Objects.equals( classDetails.getName(), that.classDetails.getName() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( classDetails );
	}

	@Override
	public String toString() {
		return "ManagedTypeMetadata(" + classDetails.getName() + ")";
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// attribute handling

	protected abstract List<AttributeMetadata> attributeList();

	@Override
	public int getNumberOfAttributes() {
		return attributeList().size();
	}

	@Override
	public Collection<AttributeMetadata> getAttributes() {
		return attributeList();
	}

	@Override
	public void forEachAttribute(IndexedConsumer<AttributeMetadata> consumer) {
		for ( int i = 0; i < attributeList().size(); i++ ) {
			consumer.accept( i, attributeList().get( i ) );
		}
	}

	protected List<AttributeMetadata> resolveAttributes() {
		final List<MemberDetails> backingMembers = StandardPersistentAttributeMemberResolver.INSTANCE.resolveAttributesMembers(
				classDetails,
				getAccessType(),
				modelContext
		);

		final List<AttributeMetadata> attributeList = arrayList( backingMembers.size() );

		for ( MemberDetails backingMember : backingMembers ) {
			final AttributeMetadata attribute = new AttributeMetadataImpl(
					backingMember.resolveAttributeName(),
					determineAttributeNature( backingMember ),
					backingMember
			);
			attributeList.add( attribute );
		}

		return attributeList;
	}

	/**
	 * Determine the attribute's nature - is it a basic mapping, an embeddable, ...?
	 *
	 * Also performs some simple validation around multiple natures being indicated
	 */
	private AttributeMetadata.AttributeNature determineAttributeNature(MemberDetails backingMember) {
		final EnumSet<AttributeMetadata.AttributeNature> natures = EnumSet.noneOf( AttributeMetadata.AttributeNature.class );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// first, look for explicit nature annotations

		final AnnotationUsage<Any> any = backingMember.getUsage( HibernateAnnotations.ANY );
		final AnnotationUsage<Basic> basic = backingMember.getUsage( JpaAnnotations.BASIC );
		final AnnotationUsage<ElementCollection> elementCollection = backingMember.getUsage( JpaAnnotations.ELEMENT_COLLECTION );
		final AnnotationUsage<Embedded> embedded = backingMember.getUsage( JpaAnnotations.EMBEDDED );
		final AnnotationUsage<EmbeddedId> embeddedId = backingMember.getUsage( JpaAnnotations.EMBEDDED_ID );
		final AnnotationUsage<ManyToAny> manyToAny = backingMember.getUsage( HibernateAnnotations.MANY_TO_ANY );
		final AnnotationUsage<ManyToMany> manyToMany = backingMember.getUsage( JpaAnnotations.MANY_TO_MANY );
		final AnnotationUsage<ManyToOne> manyToOne = backingMember.getUsage( JpaAnnotations.MANY_TO_ONE );
		final AnnotationUsage<OneToMany> oneToMany = backingMember.getUsage( JpaAnnotations.ONE_TO_MANY );
		final AnnotationUsage<OneToOne> oneToOne = backingMember.getUsage( JpaAnnotations.ONE_TO_ONE );

		if ( basic != null ) {
			natures.add( AttributeMetadata.AttributeNature.BASIC );
		}

		if ( embedded != null
				|| embeddedId != null
				|| ( backingMember.getType() != null && backingMember.getType().getUsage( JpaAnnotations.EMBEDDABLE ) != null ) ) {
			natures.add( AttributeMetadata.AttributeNature.EMBEDDED );
		}

		if ( any != null ) {
			natures.add( AttributeMetadata.AttributeNature.ANY );
		}

		if ( oneToOne != null
				|| manyToOne != null ) {
			natures.add( AttributeMetadata.AttributeNature.TO_ONE );
		}

		final boolean plural = oneToMany != null
				|| manyToMany != null
				|| elementCollection != null
				|| manyToAny != null;
		if ( plural ) {
			natures.add( AttributeMetadata.AttributeNature.PLURAL );
		}

		// look at annotations that imply a nature
		//		NOTE : these could apply to the element or index of collection, so
		//		only do these if it is not a collection

		if ( !plural ) {
			// first implicit basic nature
			if ( backingMember.getUsage( JpaAnnotations.TEMPORAL ) != null
					|| backingMember.getUsage( JpaAnnotations.LOB ) != null
					|| backingMember.getUsage( JpaAnnotations.ENUMERATED ) != null
					|| backingMember.getUsage( JpaAnnotations.CONVERT ) != null
					|| backingMember.getUsage( JpaAnnotations.VERSION ) != null
					|| backingMember.getUsage( HibernateAnnotations.GENERATED ) != null
					|| backingMember.getUsage( HibernateAnnotations.NATIONALIZED ) != null
					|| backingMember.getUsage( HibernateAnnotations.TZ_COLUMN ) != null
					|| backingMember.getUsage( HibernateAnnotations.TZ_STORAGE ) != null
					|| backingMember.getUsage( HibernateAnnotations.TYPE ) != null
					|| backingMember.getUsage( HibernateAnnotations.TENANT_ID ) != null
					|| backingMember.getUsage( HibernateAnnotations.JAVA_TYPE ) != null
					|| backingMember.getUsage( HibernateAnnotations.JDBC_TYPE_CODE ) != null
					|| backingMember.getUsage( HibernateAnnotations.JDBC_TYPE ) != null ) {
				natures.add( AttributeMetadata.AttributeNature.BASIC );
			}

			// then embedded
			if ( backingMember.getUsage( HibernateAnnotations.EMBEDDABLE_INSTANTIATOR ) != null
					|| backingMember.getUsage( HibernateAnnotations.COMPOSITE_TYPE ) != null ) {
				natures.add( AttributeMetadata.AttributeNature.EMBEDDED );
			}

			// and any
			if ( backingMember.getUsage( HibernateAnnotations.ANY_DISCRIMINATOR ) != null
					|| backingMember.getUsage( HibernateAnnotations.ANY_DISCRIMINATOR_VALUE ) != null
					|| backingMember.getUsage( HibernateAnnotations.ANY_DISCRIMINATOR_VALUES ) != null
					|| backingMember.getUsage( HibernateAnnotations.ANY_KEY_JAVA_TYPE ) != null
					|| backingMember.getUsage( HibernateAnnotations.ANY_KEY_JAVA_CLASS ) != null
					|| backingMember.getUsage( HibernateAnnotations.ANY_KEY_JDBC_TYPE ) != null
					|| backingMember.getUsage( HibernateAnnotations.ANY_KEY_JDBC_TYPE_CODE ) != null ) {
				natures.add( AttributeMetadata.AttributeNature.ANY );
			}
		}

		int size = natures.size();
		switch ( size ) {
			case 0: {
				ORM_MODEL_LOGGER.debugf(
						"Implicitly interpreting attribute `%s` as BASIC",
						backingMember.resolveAttributeName()
				);
				return AttributeMetadata.AttributeNature.BASIC;
			}
			case 1: {
				return natures.iterator().next();
			}
			default: {
				throw new MultipleAttributeNaturesException( backingMember.resolveAttributeName(), natures );
			}
		}
	}

//	@Override
//	public <A extends Annotation> List<AnnotationUsage<A>> findAnnotations(AnnotationDescriptor<A> type) {
//		return classDetails.getAnnotations( type );
//	}
//
//	@Override
//	public <A extends Annotation> void forEachAnnotation(AnnotationDescriptor<A> type, Consumer<AnnotationUsage<A>> consumer) {
//		classDetails.forEachAnnotation( type, consumer );
//	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Stuff affecting attributes built from this managed type.

	public boolean canAttributesBeInsertable() {
		return true;
	}

	public boolean canAttributesBeUpdatable() {
		return true;
	}

	public NaturalIdMutability getContainerNaturalIdMutability() {
		return NaturalIdMutability.NOT_NATURAL_ID;
	}
}
