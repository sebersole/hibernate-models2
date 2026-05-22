/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.internal.sources.CollectionSource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;

/// Applies collection-shape metadata after the concrete collection mapping exists.
///
/// `CollectionSource` decides the semantic classification of a plural member.
/// This helper applies the metadata that is orthogonal to table/element/key
/// binding, such as ordered and sorted collection settings.
///
/// @author Steve Ebersole
class CollectionShapeBinder {
	static void apply(CollectionSource source, Collection collection) {
		switch ( source.classification() ) {
			case ORDERED_SET, ORDERED_MAP -> applyOrdering( source, collection );
			case SORTED_SET, SORTED_MAP -> applySorting( source, collection );
			default -> {
			}
		}
	}

	private static void applyOrdering(CollectionSource source, Collection collection) {
		final var sqlOrder = source.sqlOrder();
		if ( sqlOrder != null ) {
			collection.setOrderBy( sqlOrder.value() );
			return;
		}

		final var orderBy = source.orderBy();
		if ( orderBy != null ) {
			// todo (models2): empty @OrderBy means order by target primary key.  We need
			// to resolve that from the collection element/key metadata instead of assuming
			// a physical column name.
			collection.setOrderBy( StringHelper.nullIfEmpty( orderBy.value() ) );
		}
	}

	private static void applySorting(CollectionSource source, Collection collection) {
		collection.setSorted( true );

		final var sortComparator = source.sortComparator();
		if ( sortComparator != null ) {
			collection.setComparatorClassName( sortComparator.value().getName() );
		}
	}
}
