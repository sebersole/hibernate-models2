/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal.binders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.models.ModelsException;
import org.hibernate.models.orm.bind.internal.SecondaryTable;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.bind.spi.TableReference;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;

import jakarta.persistence.InheritanceType;

/**
 * Binder for binding a {@linkplain PersistentClass} from a {@linkplain EntityTypeMetadata}
 *
 * @author Steve Ebersole
 */
public class EntityTypeBinder extends IdentifiableTypeBinder {
	private final PersistentClass binding;

	private final TableReference primaryTable;
	private Map<String,SecondaryTable> secondaryTableMap;

	public EntityTypeBinder(
			EntityTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			DelegateBinders delegateBinders,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		super( type, superType, hierarchyRelation, state, options, bindingContext );

		this.binding = createBinding();

		this.primaryTable = delegateBinders.getTableBinder().processPrimaryTable( getManagedType() );
		( (TableOwner) binding ).setTable( primaryTable.getBinding() );

		final List<SecondaryTable> secondaryTables = delegateBinders.getTableBinder().processSecondaryTables( getManagedType() );
		secondaryTables.forEach( (secondaryTable) -> {
			final Join join = new Join();
			join.setTable( secondaryTable.binding() );
			join.setPersistentClass( binding );
			join.setOptional( secondaryTable.optional() );
			join.setInverse( !secondaryTable.owned() );
		} );

		prepareBinding( delegateBinders );
	}

	@Override
	public PersistentClass getTypeBinding() {
		return binding;
	}

	@Override
	public EntityTypeMetadata getManagedType() {
		return (EntityTypeMetadata) super.getManagedType();
	}

	private PersistentClass createBinding() {
		if ( getHierarchyRelation() == EntityHierarchy.HierarchyRelation.SUB ) {
			return createSubclass();
		}
		else {
			return new RootClass( getBindingState().getMetadataBuildingContext() );
		}
	}

	private PersistentClass createSubclass() {
		final IdentifiableTypeMetadata superType = getSuperType();

		final IdentifiableTypeBinder superTypeBinder = (IdentifiableTypeBinder) getBindingState().getTypeBinder( superType.getClassDetails() );
		final IdentifiableTypeClass superTypeBinding = superTypeBinder.getTypeBinding();

		// we have a few cases to handle here, complicated by how Hibernate has historically modeled
		// mapped-superclass in its PersistentClass model (aka, not well) which manifests in some
		// craziness over how these PersistentClass instantiations happen

		final InheritanceType inheritanceType = superType.getHierarchy().getInheritanceType();
		if ( inheritanceType == InheritanceType.JOINED ) {
			return createJoinedSubclass( superTypeBinding );
		}

		if ( inheritanceType == InheritanceType.TABLE_PER_CLASS ) {
			return createUnionSubclass( superTypeBinding );
		}

		assert inheritanceType == null || inheritanceType == InheritanceType.SINGLE_TABLE;
		return createSingleTableSubclass( superTypeBinding );
	}

	private UnionSubclass createUnionSubclass(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass superEntity ) {
			return new UnionSubclass( superEntity, getBindingState().getMetadataBuildingContext() );
		}
		else {
			assert superTypeBinding instanceof MappedSuperclass;

			final PersistentClass superEntity = resolveSuperEntity( superTypeBinding );
			final UnionSubclass binding = new UnionSubclass(
					superEntity,
					getBindingState().getMetadataBuildingContext()
			);
			binding.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinding );
			return binding;
		}
	}

	private JoinedSubclass createJoinedSubclass(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass superEntity ) {
			return new JoinedSubclass( superEntity, getBindingState().getMetadataBuildingContext() );
		}
		else {
			assert superTypeBinding instanceof MappedSuperclass;

			final PersistentClass superEntity = resolveSuperEntity( superTypeBinding );
			final JoinedSubclass binding = new JoinedSubclass(
					superEntity,
					getBindingState().getMetadataBuildingContext()
			);
			binding.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinding );
			return binding;
		}
	}

	private SingleTableSubclass createSingleTableSubclass(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass superEntity ) {
			return new SingleTableSubclass( superEntity, getBindingState().getMetadataBuildingContext() );
		}
		else {
			assert superTypeBinding instanceof MappedSuperclass;

			final PersistentClass superEntity = resolveSuperEntity( superTypeBinding );
			final SingleTableSubclass binding = new SingleTableSubclass(
					superEntity,
					getBindingState().getMetadataBuildingContext()
			);
			binding.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinding );
			return binding;
		}
	}

	private PersistentClass resolveSuperEntity(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass ) {
			return (PersistentClass) superTypeBinding;
		}

		if ( superTypeBinding.getSuperType() != null ) {
			return resolveSuperEntity( superTypeBinding.getSuperType() );
		}

		throw new ModelsException( "Unable to resolve super PersistentClass for " + superTypeBinding );
	}

	@Override
	protected void prepareBinding(DelegateBinders delegateBinders) {
		// todo : possibly Hierarchy details - version, tenant-id, ...

		super.prepareBinding( delegateBinders );
	}
}
