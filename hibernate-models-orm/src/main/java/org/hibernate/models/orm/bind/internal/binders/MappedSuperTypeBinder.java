/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal.binders;

import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
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
	public IdentifiableTypeClass getTypeBinding() {
		return null;
	}
}
