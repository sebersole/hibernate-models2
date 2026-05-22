/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;

/// Resolves JPA `@OrderBy` declarations for collection mappings.
///
/// JPA `@OrderBy` is source-model metadata, not a SQL fragment.  It names
/// element properties, and an empty value means the element entity identifier.
/// This phase translates that source expression into the physical column
/// fragment expected by [Collection#setOrderBy(String)] after entity identifiers
/// and member properties have been bound.
///
/// @author Steve Ebersole
class CollectionOrderingBinder {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;

	CollectionOrderingBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
	}

	void bindCollectionOrderings() {
		bindingState.forEachCollectionOrderingBinding( (orderingBinding) -> {
			if ( orderingBinding.collection().getOwner() == entityBinder.getTypeBinding() ) {
				orderingBinding.collection().setOrderBy( resolveOrderBy( orderingBinding ) );
			}
		} );
	}

	private String resolveOrderBy(CollectionOrderingBinding orderingBinding) {
		if ( StringHelper.isEmpty( orderingBinding.orderBy() ) ) {
			return resolveIdentifierOrderBy( orderingBinding );
		}

		final ArrayList<String> fragments = new ArrayList<>();
		for ( String part : orderingBinding.orderBy().split( "," ) ) {
			final OrderExpression expression = OrderExpression.parse( part, orderingBinding.collection() );
			fragments.addAll( resolvePropertyOrderBy( orderingBinding, expression ) );
		}
		return String.join( ", ", fragments );
	}

	private String resolveIdentifierOrderBy(CollectionOrderingBinding orderingBinding) {
		final EntityTypeBinder elementTypeBinder = resolveEntityElementTypeBinder( orderingBinding );
		if ( elementTypeBinder == null ) {
			throw new UnsupportedOperationException(
					"Empty @OrderBy is only implemented for entity-valued collections - "
							+ orderingBinding.collection().getRole()
			);
		}

		final PersistentClass elementBinding = elementTypeBinder.getTypeBinding();
		final List<String> fragments = columnFragments(
				elementBinding.getIdentifier().getColumns(),
				OrderExpression.DEFAULT_DIRECTION
		);
		return String.join( ", ", fragments );
	}

	private List<String> resolvePropertyOrderBy(
			CollectionOrderingBinding orderingBinding,
			OrderExpression expression) {
		if ( expression.propertyPath().contains( "." ) ) {
			throw new UnsupportedOperationException(
					"Nested @OrderBy property paths are not yet implemented - "
							+ orderingBinding.collection().getRole()
			);
		}

		final EntityTypeBinder elementTypeBinder = resolveEntityElementTypeBinder( orderingBinding );
		if ( elementTypeBinder != null ) {
			final Property property = resolveEntityProperty(
					elementTypeBinder.getTypeBinding(),
					expression.propertyPath(),
					orderingBinding.collection()
			);
			return columnFragments( property.getValue(), expression.direction() );
		}

		if ( orderingBinding.collection().getElement() instanceof Component component ) {
			final Property property = resolveComponentProperty(
					component,
					expression.propertyPath(),
					orderingBinding.collection()
			);
			return columnFragments( property.getValue(), expression.direction() );
		}

		throw new UnsupportedOperationException(
				"@OrderBy property resolution is only implemented for entity and embeddable collection elements - "
						+ orderingBinding.collection().getRole()
		);
	}

	private EntityTypeBinder resolveEntityElementTypeBinder(CollectionOrderingBinding orderingBinding) {
		final var typeBinder = bindingState.getTypeBinder(
				orderingBinding.source().elementType().determineRawClass()
		);
		return typeBinder instanceof EntityTypeBinder entityTypeBinder ? entityTypeBinder : null;
	}

	private Property resolveEntityProperty(PersistentClass elementBinding, String propertyName, Collection collection) {
		final Property identifierProperty = elementBinding.getIdentifierProperty();
		if ( identifierProperty != null && identifierProperty.getName().equals( propertyName ) ) {
			return identifierProperty;
		}
		return resolveProperty( elementBinding.getProperties(), propertyName, collection );
	}

	private Property resolveComponentProperty(Component component, String propertyName, Collection collection) {
		return resolveProperty( component.getProperties(), propertyName, collection );
	}

	private Property resolveProperty(List<Property> properties, String propertyName, Collection collection) {
		for ( Property property : properties ) {
			if ( property.getName().equals( propertyName ) ) {
				return property;
			}
		}
		throw new MappingException(
				"Could not resolve @OrderBy property `" + propertyName + "` - " + collection.getRole()
		);
	}

	private List<String> columnFragments(Value value, String direction) {
		return columnFragments( value.getColumns(), direction );
	}

	private List<String> columnFragments(List<Column> columns, String direction) {
		final ArrayList<String> result = new ArrayList<>( columns.size() );
		for ( Column column : columns ) {
			result.add( column.getName() + direction );
		}
		return result;
	}

	private record OrderExpression(String propertyPath, String direction) {
		private static final String DEFAULT_DIRECTION = "";

		private static OrderExpression parse(String text, Collection collection) {
			final String trimmed = text.trim();
			if ( trimmed.isEmpty() ) {
				throw new MappingException( "Empty @OrderBy segment - " + collection.getRole() );
			}

			final String[] tokens = trimmed.split( "\\s+" );
			if ( tokens.length == 1 ) {
				return new OrderExpression( tokens[0], DEFAULT_DIRECTION );
			}
			if ( tokens.length == 2 ) {
				if ( "asc".equalsIgnoreCase( tokens[1] ) ) {
					return new OrderExpression( tokens[0], " asc" );
				}
				if ( "desc".equalsIgnoreCase( tokens[1] ) ) {
					return new OrderExpression( tokens[0], " desc" );
				}
			}
			throw new MappingException( "Invalid @OrderBy segment `" + text + "` - " + collection.getRole() );
		}
	}
}
