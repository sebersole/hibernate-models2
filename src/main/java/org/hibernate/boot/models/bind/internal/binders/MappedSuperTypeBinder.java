/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;

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

		final IdentifiableTypeBinder superTypeBinder = getSuperTypeBinder();
		final EntityTypeBinder superEntityBinder = getSuperEntityBinder();
		final MappedSuperclass superMappedSuper;
		final PersistentClass superEntity;
		if ( superTypeBinder == superEntityBinder && superTypeBinder != null ) {
			superMappedSuper = null;
			superEntity = superEntityBinder.getTypeBinding();
		}
		else if ( superTypeBinder != null ) {
			superMappedSuper = (MappedSuperclass) superTypeBinder.getTypeBinding();
			superEntity = null;
		}
		else if ( superEntityBinder != null ) {
			superMappedSuper = null;
			superEntity = superEntityBinder.getTypeBinding();
		}
		else {
			superMappedSuper = null;
			superEntity = null;
		}

		this.binding = new MappedSuperclass( superMappedSuper, superEntity, getTable() );
		state.registerTypeBinder( type, this );

		state.getMetadataBuildingContext().getMetadataCollector().addImport(
				StringHelper.unqualify( type.getClassDetails().getClassName() ),
				type.getClassDetails().getClassName()
		);
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
