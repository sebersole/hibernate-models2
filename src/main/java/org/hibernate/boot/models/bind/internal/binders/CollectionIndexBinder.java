/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.internal.sources.CollectionSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Table;
import org.hibernate.models.ModelsException;

import jakarta.persistence.MapKeyJoinColumn;

/// Binds synthetic collection index values such as list indexes and basic map keys.
///
/// @author Steve Ebersole
class CollectionIndexBinder {
	static void bindListIndex(
			CollectionSource source,
			IndexedCollection collection,
			Table table,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValue index = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		index.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.listIndex( source.member() ),
				null,
				index,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final org.hibernate.mapping.Column indexColumn = ColumnBinder.bindColumn(
				ColumnSource.from( source.orderColumn() ),
				() -> IndexedCollection.DEFAULT_INDEX_COLUMN_NAME
		);
		table.addColumn( indexColumn );
		index.addColumn(
				indexColumn,
				source.orderColumn() == null || source.orderColumn().insertable(),
				source.orderColumn() == null || source.orderColumn().updatable()
		);
		collection.setIndex( index );
	}

	static void bindMapKey(
			CollectionSource source,
			org.hibernate.mapping.Map collection,
			Table table,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( !source.mapKeyJoinColumns().isEmpty() ) {
			bindEntityMapKey( source, collection, table, bindingState );
			return;
		}
		bindBasicMapKey( source, collection, table, bindingOptions, bindingState, bindingContext );
	}

	private static void bindBasicMapKey(
			CollectionSource source,
			org.hibernate.mapping.Map collection,
			Table table,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final BasicValue index = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		index.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.mapKey( source.member(), bindingContext ),
				null,
				index,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final org.hibernate.mapping.Column indexColumn = ColumnBinder.bindColumn(
				ColumnSource.from( source.mapKeyColumn() ),
				() -> Collection.DEFAULT_KEY_COLUMN_NAME,
				false,
				false
		);
		table.addColumn( indexColumn );
		index.addColumn(
				indexColumn,
				source.mapKeyColumn() == null || source.mapKeyColumn().insertable(),
				source.mapKeyColumn() == null || source.mapKeyColumn().updatable()
		);
		collection.setIndex( index );
	}

	private static void bindEntityMapKey(
			CollectionSource source,
			org.hibernate.mapping.Map collection,
			Table table,
			BindingState bindingState) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				source.mapKeyType().determineRawClass()
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for entity-valued map key - "
							+ source.mapKeyType().determineRawClass().getClassName()
			);
		}

		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding(
				targetTypeBinder.getManagedType().getHierarchy().getRoot()
		);
		if ( identifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for entity-valued map key - "
							+ targetTypeBinder.getTypeBinding().getEntityName()
			);
		}

		final ManyToOne index = new ManyToOne( bindingState.getMetadataBuildingContext(), table );
		index.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		index.setReferenceToPrimaryKey( true );
		index.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		index.setTypeUsingReflection( collection.getOwner().getClassName(), source.member().resolveAttributeName() );

		final List<MapKeyJoinColumn> orderedJoinColumns = orderMapKeyJoinColumns(
				source.mapKeyJoinColumns(),
				identifierBinding.columns(),
				collection.getRole()
		);
		for ( int i = 0; i < identifierBinding.columns().size(); i++ ) {
			final Column targetColumn = identifierBinding.columns().get( i );
			final MapKeyJoinColumn mapKeyJoinColumn = orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i );
			final Column column = ColumnBinder.bindColumn(
					ColumnSource.from( mapKeyJoinColumn ),
					() -> Collection.DEFAULT_KEY_COLUMN_NAME + "_" + targetColumn.getName(),
					false,
					false
			);
			table.addColumn( column );
			index.addColumn(
					column,
					mapKeyJoinColumn == null || mapKeyJoinColumn.insertable(),
					mapKeyJoinColumn == null || mapKeyJoinColumn.updatable()
			);
		}
		index.createForeignKey();
		collection.setIndex( index );
	}

	private static List<MapKeyJoinColumn> orderMapKeyJoinColumns(
			List<MapKeyJoinColumn> joinColumns,
			List<Column> targetColumns,
			String role) {
		if ( joinColumns.isEmpty() || joinColumns.stream().noneMatch( (joinColumn) -> !joinColumn.referencedColumnName().isEmpty() ) ) {
			return joinColumns;
		}

		final ArrayList<MapKeyJoinColumn> orderedJoinColumns = new ArrayList<>( targetColumns.size() );
		final ArrayList<MapKeyJoinColumn> unmatchedJoinColumns = new ArrayList<>( joinColumns );
		for ( Column targetColumn : targetColumns ) {
			final MapKeyJoinColumn joinColumn = findMapKeyJoinColumn( targetColumn, unmatchedJoinColumns, role );
			orderedJoinColumns.add( joinColumn );
			unmatchedJoinColumns.remove( joinColumn );
		}
		return orderedJoinColumns;
	}

	private static MapKeyJoinColumn findMapKeyJoinColumn(
			Column targetColumn,
			List<MapKeyJoinColumn> joinColumns,
			String role) {
		for ( MapKeyJoinColumn joinColumn : joinColumns ) {
			if ( targetColumn.getName().equals( joinColumn.referencedColumnName() ) ) {
				return joinColumn;
			}
		}

		throw new ModelsException(
				"Unable to match map-key join column referencedColumnName to target identifier column `"
						+ targetColumn.getName() + "` - " + role
		);
	}
}
