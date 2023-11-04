/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal.binders;

import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.Table;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.MappedSuperclassTypeMetadata;

/**
 * @author Steve Ebersole
 */
public class MappedSuperTypeBinder extends IdentifiableTypeBinder {
	private final MappedSuperclass binding;

	public MappedSuperTypeBinder(
			MappedSuperclassTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		super( type, superType, hierarchyRelation, state, options, bindingContext );

		throw new UnsupportedOperationException( "Not yet implemented" );
//		this.binding = new MappedSuperclass(  );
	}

	@Override
	public MappedSuperclass getTypeBinding() {
		return binding;
	}

	@Override
	public Table getTable() {
		final var superEntityBinder = getSuperEntityBinder();
		if ( superEntityBinder == null ) {
			return null;
		}

		return superEntityBinder.getTypeBinding().getTable();
	}

	@Override
	public EntityTypeMetadata findSuperEntity() {
		if ( getSuperType() != null ) {
			final var superTypeBinder = getBindingState().getSuperTypeBinder( getManagedType().getClassDetails() );
			return superTypeBinder.findSuperEntity();
		}
		return null;
	}
}