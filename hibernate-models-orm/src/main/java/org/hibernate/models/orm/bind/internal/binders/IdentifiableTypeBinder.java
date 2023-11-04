/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;

/**
 * @author Steve Ebersole
 */
public abstract class IdentifiableTypeBinder extends ManagedTypeBinder {
	private final IdentifiableTypeMetadata superType;
	private final EntityHierarchy.HierarchyRelation hierarchyRelation;

	private final List<AttributeBinder> attributeBinders;

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

		this.attributeBinders = new ArrayList<>( type.getNumberOfAttributes() );
	}

	public abstract IdentifiableTypeClass getTypeBinding();

	public IdentifiableTypeMetadata getSuperType() {
		return superType;
	}

	public EntityHierarchy.HierarchyRelation getHierarchyRelation() {
		return hierarchyRelation;
	}

	@Override
	public IdentifiableTypeMetadata getManagedType() {
		return (IdentifiableTypeMetadata) super.getManagedType();
	}

	@Override
	protected void prepareBinding(DelegateBinders delegateBinders) {
//		getManagedType().forEachAttribute( (index, attributeMetadata) -> {
//			final AttributeBinder attributeBinder = new AttributeBinder(
//					attributeMetadata,
//					getBindingState(),
//					getOptions(),
//					getBindingContext()
//			);
//			attributeBinders.add( attributeBinder );
//			getTypeBinding().applyProperty( attributeBinder.getBinding() );
//		} );

		super.prepareBinding( delegateBinders );
	}
}
