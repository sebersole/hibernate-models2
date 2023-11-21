/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/**
 * @author Steve Ebersole
 */
public abstract class IdentifiableTypeBinder extends ManagedTypeBinder {
	private final IdentifiableTypeMetadata superType;
	private final EntityHierarchy.HierarchyRelation hierarchyRelation;

	private final List<AttributeBinder> attributeBinders;
	private final IdentifiableTypeBinder superTypeBinder;

	public IdentifiableTypeBinder(
			IdentifiableTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		super( type, state, options, bindingContext );
		this.superType = superType;
		this.hierarchyRelation = hierarchyRelation;
		this.superTypeBinder = superType == null ? null : (IdentifiableTypeBinder) state.getTypeBinder( superType.getClassDetails() );
		this.attributeBinders = new ArrayList<>( type.getNumberOfAttributes() );
	}

	public abstract EntityTypeMetadata findSuperEntity();

	public EntityTypeBinder getSuperEntityBinder() {
		IdentifiableTypeBinder check = superTypeBinder;
		if ( check == null ) {
			return null;
		}

		do {
			if ( check.getTypeBinding() instanceof PersistentClass ) {
				return (EntityTypeBinder) check;
			}
			check = check.getSuperTypeBinder();
		} while ( check != null );

		return null;
	}

	public IdentifiableTypeBinder getSuperTypeBinder() {
		return superTypeBinder;
	}

	public abstract IdentifiableTypeClass getTypeBinding();

	public IdentifiableTypeMetadata getSuperType() {
		return superType;
	}

	public EntityHierarchy.HierarchyRelation getHierarchyRelation() {
		return hierarchyRelation;
	}

	public abstract Table getTable();

	@Override
	public IdentifiableTypeMetadata getManagedType() {
		return (IdentifiableTypeMetadata) super.getManagedType();
	}

	@Override
	protected void prepareBinding(ModelBinders modelBinders) {
		final var primaryTable = getTable();
		final var managedType = getManagedType();

		managedType.forEachAttribute( (index, attributeMetadata) -> {
			if ( managedType.getHierarchy().getIdMapping().contains( attributeMetadata )
					|| managedType.getHierarchy().getVersionAttribute() == attributeMetadata
					|| managedType.getHierarchy().getTenantIdAttribute() == attributeMetadata ) {
				return;
			}

			final var attributeBinder = new AttributeBinder(
					attributeMetadata,
					primaryTable,
					getBindingState(),
					getOptions(),
					getBindingContext()
			);

			final var property = attributeBinder.getBinding();
			final var value = property.getValue();

			attributeBinders.add( attributeBinder );
			final Table attributeTable = value.getTable();
			if ( attributeTable == primaryTable ) {
				getTypeBinding().applyProperty( property );
			}
			else {
				final Join join = findJoin( attributeTable );
				join.addProperty( property );
			}
		} );

		super.prepareBinding( modelBinders );
	}

	private Join findJoin(Table attributeTable) {
		final List<Join> joins = ( (PersistentClass) getTypeBinding() ).getJoinClosure();
		for ( int i = 0; i < joins.size(); i++ ) {
			if ( joins.get( i ).getTable() == attributeTable ) {
				return joins.get( i );
			}
		}
		throw new IllegalArgumentException( "Could not locate Table for name - " + attributeTable.getName() );
	}

	@Override
	public void processSecondPasses() {
		attributeBinders.forEach( AttributeBinder::processSecondPasses );
	}

}
