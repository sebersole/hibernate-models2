/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.boot.models.AccessTypeDeterminationException;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.categorize.spi.CategorizationContext;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds {@link EntityHierarchy} references from
 * {@linkplain ClassDetailsRegistry#forEachClassDetails managed classes}.
 *
 * @author Steve Ebersole
 */
public class EntityHierarchyBuilder {

	public static Set<EntityHierarchy> createEntityHierarchies(
			ManagedTypeInheritanceState inheritanceState,
			CategorizationContext buildingContext) {
		return new EntityHierarchyBuilder( buildingContext ).process( inheritanceState );
	}

	private final CategorizationContext modelContext;

	public EntityHierarchyBuilder(CategorizationContext modelContext) {
		this.modelContext = modelContext;
	}

	private Set<EntityHierarchy> process(
			ManagedTypeInheritanceState inheritanceState,
			MappedSuperclassTracker mappedSuperclassTracker) {
		final Set<ClassDetails> rootEntities = inheritanceState.getRootEntities();
		final Set<EntityHierarchy> hierarchies = CollectionHelper.setOfSize( rootEntities.size() );

		rootEntities.forEach( (rootEntity) -> {
			final AccessType defaultAccessType = determineDefaultAccessTypeForHierarchy( rootEntity, inheritanceState );
			hierarchies.add( new EntityHierarchyImpl(
					rootEntity,
					defaultAccessType,
					org.hibernate.cache.spi.access.AccessType.TRANSACTIONAL,
					inheritanceState,
					mappedSuperclassTracker,
					modelContext
			) );
		} );

		return hierarchies;
	}

	private Set<EntityHierarchy> process(ManagedTypeInheritanceState inheritanceState) {
		final MappedSuperclassTracker mappedSuperclassTracker = new MappedSuperclassTracker( inheritanceState );
		final Set<EntityHierarchy> entityHierarchies = process(
				inheritanceState,
				mappedSuperclassTracker
		);
		mappedSuperclassTracker.warnAboutUnusedMappedSuperclasses();
		return entityHierarchies;
	}

	@NonNull
	private AccessType determineDefaultAccessTypeForHierarchy(
			ClassDetails rootEntityType,
			ManagedTypeInheritanceState inheritanceState) {
		assert rootEntityType != null;

		final AccessType[] result = new AccessType[1];
		final Set<ClassDetails> visited = new HashSet<>();

		ClassDetails current = rootEntityType;
		while ( current != null ) {
			applyDefaultedAccessType( rootEntityType, current, result, visited );
			current = current.getSuperClass();
		}

		applyDefaultedAccessTypesFromSubTypes( rootEntityType, rootEntityType, inheritanceState, result, visited );

		return result[0] == null
				? modelContext.getEffectiveMappingDefaults().getDefaultPropertyAccessType()
				: result[0];
	}

	private void applyDefaultedAccessTypesFromSubTypes(
			ClassDetails rootEntityType,
			ClassDetails current,
			ManagedTypeInheritanceState inheritanceState,
			AccessType[] result,
			Set<ClassDetails> visited) {
		inheritanceState.forEachSubType( current, (subType) -> {
			applyDefaultedAccessType( rootEntityType, subType, result, visited );
			applyDefaultedAccessTypesFromSubTypes( rootEntityType, subType, inheritanceState, result, visited );
		} );
	}

	private void applyDefaultedAccessType(
			ClassDetails rootEntityType,
			ClassDetails current,
			AccessType[] result,
			Set<ClassDetails> visited) {
		if ( !visited.add( current ) ) {
			return;
		}

		final Access accessAnnotation = current.getDirectAnnotationUsage( JpaAnnotations.ACCESS );
		if ( accessAnnotation != null ) {
			return;
		}

		final MemberDetails defaultedMember = findDefaultedMember( current );
		if ( defaultedMember == null ) {
			return;
		}

		final AccessType memberAccessType = determineAccessType( rootEntityType, defaultedMember );
		if ( result[0] == null ) {
			result[0] = memberAccessType;
		}
		else if ( result[0] != memberAccessType ) {
			throw new AccessTypeDeterminationException( rootEntityType );
		}
	}

	private AccessType determineAccessType(ClassDetails rootEntityType, MemberDetails memberDetails) {
		if ( memberDetails.getKind() == AnnotationTarget.Kind.FIELD ) {
			return AccessType.FIELD;
		}
		else if ( memberDetails.getKind() == AnnotationTarget.Kind.METHOD
				&& memberDetails.asMethodDetails().getMethodKind() == MethodDetails.MethodKind.GETTER ) {
			return AccessType.PROPERTY;
		}

		throw new AccessTypeDeterminationException( rootEntityType );
	}

	protected MemberDetails findDefaultedMember(ClassDetails current) {
		final List<MethodDetails> methods = current.getMethods();
		for ( int i = 0; i < methods.size(); i++ ) {
			final MethodDetails methodDetails = methods.get( i );
			if ( CategorizationHelper.isDefaultAccessTypeIndicator( methodDetails ) ) {
				return methodDetails;
			}
		}

		final List<FieldDetails> fields = current.getFields();
		for ( int i = 0; i < fields.size(); i++ ) {
			final FieldDetails fieldDetails = fields.get( i );
			if ( CategorizationHelper.isDefaultAccessTypeIndicator( fieldDetails ) ) {
				return fieldDetails;
			}
		}

		return null;
	}

	public static boolean isRoot(ClassDetails classInfo) {
		// perform a series of opt-out checks against the super-type hierarchy

		// an entity is considered a root of the hierarchy if:
		// 		1) it has no super-types
		//		2) its super types contain no entities (MappedSuperclasses are allowed)

		if ( classInfo.getSuperClass() == null ) {
			return true;
		}

		ClassDetails current = classInfo.getSuperClass();
		while ( current != null ) {
			if ( current.hasDirectAnnotationUsage( Entity.class ) ) {
				// a super type has `@Entity`, cannot be root
				return false;
			}
			current = current.getSuperClass();
		}

		// if we hit no opt-outs we have a root
		return true;
	}
}
