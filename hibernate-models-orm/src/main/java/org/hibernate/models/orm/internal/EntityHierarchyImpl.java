/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.internal;

import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.models.orm.spi.EntityHierarchy;
import org.hibernate.models.orm.spi.EntityTypeMetadata;
import org.hibernate.models.orm.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.spi.JpaAnnotations;
import org.hibernate.models.orm.spi.OrmModelBuildingContext;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;

import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.hibernate.models.orm.internal.OrmModelLogging.ORM_MODEL_LOGGER;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchyImpl implements EntityHierarchy {
	private final EntityTypeMetadata rootEntityTypeMetadata;

	private final InheritanceType inheritanceType;

	// todo (models) : version?  row-id?  tenant-id?  others?

	public EntityHierarchyImpl(
			ClassDetails rootEntityClassDetails,
			jakarta.persistence.AccessType defaultAccessType,
			Consumer<IdentifiableTypeMetadata> typeConsumer,
			OrmModelBuildingContext processingContext) {
		this.rootEntityTypeMetadata = new EntityTypeMetadataImpl(
				rootEntityClassDetails,
				this,
				defaultAccessType,
				typeConsumer,
				processingContext
		);

		this.inheritanceType = determineInheritanceType( rootEntityTypeMetadata );
	}

	private InheritanceType determineInheritanceType(EntityTypeMetadata root) {
		if ( ORM_MODEL_LOGGER.isDebugEnabled() ) {
			// Validate that there is no @Inheritance annotation further down the hierarchy
			ensureNoInheritanceAnnotationsOnSubclasses( root );
		}

		IdentifiableTypeMetadata current = root;
		while ( current != null ) {
			final InheritanceType inheritanceType = getLocallyDefinedInheritanceType( current.getClassDetails() );
			if ( inheritanceType != null ) {
				return inheritanceType;
			}

			current = current.getSuperType();
		}

		return InheritanceType.SINGLE_TABLE;
	}

	/**
	 * Find the InheritanceType from the locally defined {@link Inheritance} annotation,
	 * if one.  Returns {@code null} if {@link Inheritance} is not locally defined.
	 *
	 * @apiNote Used when building the {@link EntityHierarchy}
	 */
	private static InheritanceType getLocallyDefinedInheritanceType(ClassDetails managedClass) {
		final AnnotationUsage<Inheritance> localAnnotation = managedClass.getUsage( JpaAnnotations.INHERITANCE );
		if ( localAnnotation == null ) {
			return null;
		}

		return localAnnotation.getAttributeValue( "strategy" );
	}

	private void ensureNoInheritanceAnnotationsOnSubclasses(IdentifiableTypeMetadata type) {
		type.forEachSubType( (subType) -> {
			if ( getLocallyDefinedInheritanceType( subType.getClassDetails() ) != null ) {
				ORM_MODEL_LOGGER.debugf(
						"@javax.persistence.Inheritance was specified on non-root entity [%s]; ignoring...",
						type.getClassDetails().getName()
				);
			}
			ensureNoInheritanceAnnotationsOnSubclasses( subType );
		} );
	}

	@Override
	public EntityTypeMetadata getRoot() {
		return rootEntityTypeMetadata;
	}

	@Override
	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"EntityHierarchyImpl(`%s` (%s))",
				rootEntityTypeMetadata.getEntityName(),
				inheritanceType.name()
		);
	}
}