/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.models.orm.JpaAnnotations;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ModelCategorizationContext;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;


/**
 * @author Steve Ebersole
 */
public abstract class AbstractIdentifiableTypeMetadata
		extends AbstractManagedTypeMetadata
		implements IdentifiableTypeMetadata {
	private final EntityHierarchy hierarchy;
	private final AbstractIdentifiableTypeMetadata superType;
	private final Set<IdentifiableTypeMetadata> subTypes = new HashSet<>();

	private final AccessType accessType;

	/**
	 * Used when creating the hierarchy root-root
	 *
	 * @param accessType This is the hierarchy default
	 */
	public AbstractIdentifiableTypeMetadata(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AccessType accessType,
			ModelCategorizationContext processingContext) {
		super( classDetails, processingContext );

		this.hierarchy = hierarchy;
		this.superType = null;

		this.accessType = determineAccessType( accessType );
	}


	public AbstractIdentifiableTypeMetadata(
			ClassDetails classDetails,
			EntityHierarchy hierarchy,
			AbstractIdentifiableTypeMetadata superType,
			ModelCategorizationContext processingContext) {
		super( classDetails, processingContext );

		assert superType != null;

		this.hierarchy = hierarchy;
		this.superType = superType;

		this.accessType = determineAccessType( superType.getAccessType() );
	}

	protected void postInstantiate(HierarchyTypeConsumer typeConsumer) {
		typeConsumer.acceptType( this );

		// now we can effectively walk subs
		walkSubclasses( typeConsumer );

		// the idea here is to collect up class-level annotations and to apply
		// the maps from supers
		collectConversionInfo();
		collectAttributeOverrides();
		collectAssociationOverrides();
	}

	private void walkSubclasses(HierarchyTypeConsumer typeConsumer) {
		walkSubclasses( getClassDetails(), typeConsumer );
	}

	private void walkSubclasses(ClassDetails base, HierarchyTypeConsumer typeConsumer) {
		final ClassDetailsRegistry classDetailsRegistry = getModelContext().getClassDetailsRegistry();
		classDetailsRegistry.forEachDirectSubType( base.getName(), (subClassDetails) -> {
			final AbstractIdentifiableTypeMetadata subTypeMetadata;
			if ( CategorizationHelper.isEntity( subClassDetails ) ) {
				subTypeMetadata = new EntityTypeMetadataImpl(
						subClassDetails,
						getHierarchy(),
						this,
						typeConsumer,
						getModelContext()
				);
				addSubclass( subTypeMetadata );
			}
			else if ( CategorizationHelper.isMappedSuperclass( subClassDetails ) ) {
				subTypeMetadata = new MappedSuperclassTypeMetadataImpl(
						subClassDetails,
						getHierarchy(),
						this,
						typeConsumer,
						getModelContext()
				);
				addSubclass( subTypeMetadata );
			}
			else {
				// skip over "intermediate" sub-types
				walkSubclasses( subClassDetails, typeConsumer );
			}
		} );

	}

	private AccessType determineAccessType(AccessType defaultAccessType) {
		final AnnotationUsage<Access> annotation = getClassDetails().getAnnotationUsage( JpaAnnotations.ACCESS );
		if ( annotation != null ) {
			return annotation.getAttributeValue( "value" );
		}

		return defaultAccessType;
	}

	private void addSubclass(IdentifiableTypeMetadata subclass) {
		subTypes.add( subclass );
	}

	@Override
	public EntityHierarchy getHierarchy() {
		return hierarchy;
	}

	@Override
	public IdentifiableTypeMetadata getSuperType() {
		return superType;
	}

	@Override
	public boolean isAbstract() {
		return getClassDetails().isAbstract();
	}

	@Override
	public boolean hasSubTypes() {
		// assume this is called only after its constructor is complete
		return !subTypes.isEmpty();
	}

	@Override
	public int getNumberOfSubTypes() {
		return subTypes.size();
	}

	@Override
	public void forEachSubType(Consumer<IdentifiableTypeMetadata> consumer) {
		// assume this is called only after its constructor is complete
		subTypes.forEach( consumer );
	}

	@Override
	public Iterable<IdentifiableTypeMetadata> getSubTypes() {
		// assume this is called only after its constructor is complete
		return subTypes;
	}

	@Override
	public AccessType getAccessType() {
		return accessType;
	}

	protected void collectConversionInfo() {
		// we only need to do this on root
	}

	protected void collectAttributeOverrides() {
		// we only need to do this on root
	}

	protected void collectAssociationOverrides() {
		// we only need to do this on root
	}
}
