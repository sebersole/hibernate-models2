/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.Comment;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.UserDefinedObjectType;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.internal.EmbeddableHelper;
import org.hibernate.sql.Template;
import org.hibernate.type.SqlTypes;

import static org.hibernate.internal.util.StringHelper.qualify;

/// Finalizes aggregate components after their nested properties are bound.
///
/// This class is intentionally close to upstream
/// `org.hibernate.boot.model.internal.AggregateComponentSecondPass`.  It is a
/// transitional adapter while the PoC proves the phase-oriented bootstrap flow;
/// as the design settles, the copied second-pass logic should be folded into
/// this finalizer and expressed directly in terms of [AggregateComponentBinding]
/// and [BindingState].
class AggregateComponentFinalizer {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;

	AggregateComponentFinalizer(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
	}

	void finalizeAggregateComponents() {
		bindingState.forEachAggregateComponentBinding( (aggregateComponentBinding) -> {
			if ( aggregateComponentBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				finalizeAggregateComponent( aggregateComponentBinding );
			}
		} );
	}

	private void finalizeAggregateComponent(AggregateComponentBinding binding) {
		final Component component = binding.component();
		validateComponent( component, binding.propertyPath(), isAggregateArray( component ) );

		final var metadataCollector = bindingState.getMetadataBuildingContext().getMetadataCollector();
		final var typeConfiguration = metadataCollector.getTypeConfiguration();
		final var database = metadataCollector.getDatabase();
		final var dialect = database.getDialect();
		final var aggregateSupport = dialect.getAggregateSupport();

		final int[] originalOrder = component.sortProperties();
		final List<Column> aggregatedColumns = component.getAggregatedColumns();
		final AggregateColumn aggregateColumn = component.getAggregateColumn();

		ensureInitialized( aggregateColumn );
		validateSupportedColumnTypes( binding.propertyPath(), component );

		final QualifiedName structName = component.getStructName();
		final boolean addAuxiliaryObjects;
		if ( structName != null ) {
			final Namespace namespace = database.locateNamespace(
					structName.getCatalogName(),
					structName.getSchemaName()
			);
			if ( !database.getDialect().supportsUserDefinedTypes() ) {
				throw new MappingException( "Database does not support user-defined types (remove '@Struct' annotation)" );
			}
			final var udt = new UserDefinedObjectType( "orm", namespace, structName.getObjectName() );
			final var comment = binding.componentClassDetails().getDirectAnnotationUsage( Comment.class );
			if ( comment != null ) {
				udt.setComment( comment.value() );
			}
			for ( Column aggregatedColumn : aggregatedColumns ) {
				udt.addColumn( aggregatedColumn );
			}
			final var registeredUdt = namespace.createUserDefinedType( structName.getObjectName(), name -> udt );
			if ( registeredUdt == udt ) {
				addAuxiliaryObjects = true;
				orderColumns( registeredUdt, component, originalOrder );
			}
			else {
				addAuxiliaryObjects =
						isAggregateArray( component )
								&& namespace.locateUserDefinedArrayType( Identifier.toIdentifier( aggregateColumn.getSqlType() ) ) == null;
				validateEqual( registeredUdt, udt, component, binding.componentClassDetails().getName() );
			}
		}
		else {
			addAuxiliaryObjects = true;
		}

		final String aggregateReadTemplate = aggregateColumn.getAggregateReadExpressionTemplate( dialect );
		final String aggregateReadExpression = aggregateReadTemplate.replace( Template.TEMPLATE + ".", "" );
		final String aggregateAssignmentExpression = aggregateColumn.getAggregateAssignmentExpressionTemplate( dialect )
				.replace( Template.TEMPLATE + ".", "" );
		if ( addAuxiliaryObjects ) {
			aggregateSupport.aggregateAuxiliaryDatabaseObjects(
					database.getDefaultNamespace(),
					aggregateReadExpression,
					aggregateColumn,
					aggregatedColumns
			).forEach( database::addAuxiliaryDatabaseObject );
		}
		aggregateColumn.setCustomWrite(
				aggregateSupport.aggregateCustomWriteExpression( aggregateColumn, aggregatedColumns )
		);

		for ( Column subColumn : aggregatedColumns ) {
			final String selectableExpression = subColumn.getText( dialect );
			String assignmentExpression;
			try {
				assignmentExpression = aggregateSupport.aggregateComponentAssignmentExpression(
						aggregateAssignmentExpression,
						selectableExpression,
						aggregateColumn,
						subColumn
				);
			}
			catch (RuntimeException ex) {
				// Transitional guard while this class still mirrors AggregateComponentSecondPass.
				assignmentExpression = aggregateAssignmentExpression;
			}
			final String customReadExpression;
			if ( subColumn.getCustomReadExpression() == null ) {
				if ( subColumn.isFormula() ) {
					customReadExpression = aggregateComponentCustomReadExpression(
							() -> aggregateSupport.aggregateComponentCustomReadExpression(
									subColumn.getTemplate( dialect, typeConfiguration ),
									Template.TEMPLATE + ".",
									aggregateReadTemplate,
									"",
									aggregateColumn,
									subColumn
							),
							selectableExpression
					);
				}
				else {
					customReadExpression = aggregateComponentCustomReadExpression(
							() -> aggregateSupport.aggregateComponentCustomReadExpression(
									"",
									"",
									aggregateReadTemplate,
									selectableExpression,
									aggregateColumn,
									subColumn
							),
							selectableExpression
					);
				}
			}
			else {
				customReadExpression = aggregateComponentCustomReadExpression(
						() -> aggregateSupport.aggregateComponentCustomReadExpression(
								subColumn.getCustomReadExpression(),
								Template.TEMPLATE + ".",
								aggregateReadTemplate,
								"",
								aggregateColumn,
								subColumn
						),
						selectableExpression
				);
			}
			subColumn.setAssignmentExpression( assignmentExpression );
			subColumn.setCustomRead( customReadExpression );
		}

		binding.table().getColumns().removeAll( aggregatedColumns );
	}

	private static String aggregateComponentCustomReadExpression(
			java.util.function.Supplier<String> expressionSupplier,
			String fallbackExpression) {
		try {
			return expressionSupplier.get();
		}
		catch (RuntimeException ex) {
			// Transitional guard while this class still mirrors AggregateComponentSecondPass.
			return fallbackExpression;
		}
	}

	private static void validateComponent(Component component, String basePath, boolean inArray) {
		for ( Property property : component.getProperties() ) {
			final Value value = property.getValue();
			if ( value instanceof Component comp ) {
				validateComponent( comp, qualify( basePath, property.getName() ), inArray );
			}
			else if ( value instanceof ToOne toOne ) {
				if ( inArray && toOne.getReferencedPropertyName() != null ) {
					throw new AnnotationException(
							"Property '" + qualify( basePath, property.getName() )
									+ "' uses one-to-one mapping with mappedBy '"
									+ toOne.getReferencedPropertyName()
									+ "' in the aggregate component class '"
									+ component.getComponentClassName()
									+ "' within an array property, which is not allowed."
					);
				}
			}
			else if ( value instanceof Collection collection ) {
				if ( inArray && collection.getMappedByProperty() != null ) {
					throw new AnnotationException(
							"Property '" + qualify( basePath, property.getName() )
									+ "' uses *-to-many mapping with mappedBy '"
									+ collection.getMappedByProperty()
									+ "' in the aggregate component class '"
									+ component.getComponentClassName()
									+ "' within an array property, which is not allowed."
					);
				}
				if ( inArray && collection.getCollectionTable() != null ) {
					throw new AnnotationException(
							"Property '" + qualify( basePath, property.getName() )
									+ "' defines a collection table '"
									+ collection.getCollectionTable()
									+ "' in the aggregate component class '"
									+ component.getComponentClassName()
									+ "' within an array property, which is not allowed."
					);
				}
			}
		}
	}

	private static boolean isAggregateArray(Component component) {
		return switch ( component.getAggregateColumn().getTypeCode() ) {
			case SqlTypes.STRUCT_ARRAY,
					SqlTypes.STRUCT_TABLE,
					SqlTypes.JSON_ARRAY,
					SqlTypes.XML_ARRAY,
					SqlTypes.ARRAY,
					SqlTypes.TABLE -> true;
			default -> false;
		};
	}

	private void orderColumns(UserDefinedObjectType userDefinedType, Component component, int[] originalOrder) {
		final Class<?> componentClass = component.getComponentClass();
		final String[] structColumnNames = component.getStructColumnNames();
		if ( structColumnNames == null || structColumnNames.length == 0 ) {
			final int[] propertyMappingIndex;
			if ( componentClass.isRecord() ) {
				if ( originalOrder == null ) {
					propertyMappingIndex = null;
				}
				else {
					final String[] componentNames = ReflectHelper.getRecordComponentNames( componentClass );
					propertyMappingIndex = EmbeddableHelper.determineMappingIndex(
							component.getPropertyNames(),
							componentNames
					);
				}
			}
			else if ( component.getInstantiatorPropertyNames() != null ) {
				propertyMappingIndex = EmbeddableHelper.determineMappingIndex(
						component.getPropertyNames(),
						component.getInstantiatorPropertyNames()
				);
			}
			else {
				propertyMappingIndex = null;
			}
			final ArrayList<Column> orderedColumns = new ArrayList<>( userDefinedType.getColumnSpan() );
			if ( propertyMappingIndex == null ) {
				for ( Property property : component.getProperties() ) {
					addColumns( orderedColumns, property.getValue() );
				}
				if ( component.isPolymorphic() ) {
					addColumns( orderedColumns, component.getDiscriminator() );
				}
			}
			else {
				final List<Property> properties = component.getProperties();
				for ( int propertyIndex : propertyMappingIndex ) {
					addColumns( orderedColumns, properties.get( propertyIndex ).getValue() );
				}
			}
			final List<Column> reorderedColumn = bindingState.getMetadataBuildingContext()
					.getBuildingOptions()
					.getColumnOrderingStrategy()
					.orderUserDefinedTypeColumns(
							userDefinedType,
							bindingState.getMetadataBuildingContext().getMetadataCollector()
					);
			userDefinedType.reorderColumns( reorderedColumn != null ? reorderedColumn : orderedColumns );
		}
		else {
			final ArrayList<Column> orderedColumns = new ArrayList<>( userDefinedType.getColumnSpan() );
			for ( String structColumnName : structColumnNames ) {
				if ( !addColumns( orderedColumns, component, structColumnName ) ) {
					throw new MappingException(
							"Couldn't find column [" + structColumnName + "] that was defined in @Struct(attributes) in the component ["
									+ component.getComponentClassName() + "]"
					);
				}
			}
			userDefinedType.reorderColumns( orderedColumns );
		}
	}

	private static void addColumns(ArrayList<Column> orderedColumns, Value value) {
		if ( value instanceof Component subComponent ) {
			if ( subComponent.getAggregateColumn() == null ) {
				for ( Property property : subComponent.getProperties() ) {
					addColumns( orderedColumns, property.getValue() );
				}
			}
			else {
				orderedColumns.add( subComponent.getAggregateColumn() );
			}
		}
		else {
			orderedColumns.addAll( value.getColumns() );
		}
	}

	private static boolean addColumns(ArrayList<Column> orderedColumns, Component component, String structColumnName) {
		for ( Property property : component.getProperties() ) {
			final Value value = property.getValue();
			if ( value instanceof Component subComponent ) {
				if ( subComponent.getAggregateColumn() == null ) {
					if ( addColumns( orderedColumns, subComponent, structColumnName ) ) {
						return true;
					}
				}
				else if ( structColumnName.equals( subComponent.getAggregateColumn().getName() ) ) {
					orderedColumns.add( subComponent.getAggregateColumn() );
					return true;
				}
			}
			else {
				for ( Selectable selectable : value.getSelectables() ) {
					if ( selectable instanceof Column column
							&& structColumnName.equals( column.getName() ) ) {
						orderedColumns.add( column );
						return true;
					}
				}
			}
		}
		if ( component.isPolymorphic() ) {
			final Column column = component.getDiscriminator().getColumns().get( 0 );
			if ( structColumnName.equals( column.getName() ) ) {
				orderedColumns.add( column );
				return true;
			}
		}
		return false;
	}

	private void validateSupportedColumnTypes(String basePath, Component component) {
		for ( Property property : component.getProperties() ) {
			final Value value = property.getValue();
			if ( value instanceof Component subComponent ) {
				if ( subComponent.getAggregateColumn() == null ) {
					validateSupportedColumnTypes( qualify( basePath, property.getName() ), subComponent );
				}
			}
		}
	}

	private void ensureInitialized(AggregateColumn aggregateColumn) {
		ensureParentInitialized( aggregateColumn );
		ensureChildrenInitialized( aggregateColumn );
	}

	private void ensureChildrenInitialized(AggregateColumn aggregateColumn) {
		for ( Column aggregatedColumn : aggregateColumn.getComponent().getAggregatedColumns() ) {
			aggregatedColumn.getSqlTypeCode( bindingState.getMetadataBuildingContext().getMetadataCollector() );
			if ( aggregatedColumn instanceof AggregateColumn aggregate ) {
				ensureChildrenInitialized( aggregate );
			}
		}
	}

	private void ensureParentInitialized(AggregateColumn aggregateColumn) {
		do {
			aggregateColumn.getValue().getType();
			aggregateColumn.getSqlTypeCode( bindingState.getMetadataBuildingContext().getMetadataCollector() );
			aggregateColumn = aggregateColumn.getComponent().getParentAggregateColumn();
		} while ( aggregateColumn != null );
	}

	private void validateEqual(
			UserDefinedObjectType udt1,
			UserDefinedObjectType udt2,
			Component component,
			String componentClassName) {
		if ( udt1.getColumnSpan() != udt2.getColumnSpan() ) {
			throw new MappingException(
					String.format(
							"Struct [%s] is defined by multiple components %s with different number of mappings %d and %d",
							udt1.getName(),
							findComponentClasses( component ),
							udt1.getColumnSpan(),
							udt2.getColumnSpan()
					)
			);
		}
		final List<Column> missingColumns = new ArrayList<>();
		for ( Column column1 : udt1.getColumns() ) {
			final Column column2 = udt2.getColumn( column1 );
			if ( column2 == null ) {
				missingColumns.add( column1 );
			}
			else if ( !column1.getSqlType().equals( column2.getSqlType() ) ) {
				throw new MappingException(
						String.format(
								"Struct [%s] of class [%s] is defined by multiple components with different mappings [%s] and [%s] for column [%s]",
								udt1.getName(),
								componentClassName,
								column1.getSqlType(),
								column2.getSqlType(),
								column1.getCanonicalName()
						)
				);
			}
		}

		if ( !missingColumns.isEmpty() ) {
			throw new MappingException(
					String.format(
							"Struct [%s] is defined by multiple components %s but some columns are missing in [%s]: %s",
							udt1.getName(),
							findComponentClasses( component ),
							componentClassName,
							missingColumns
					)
			);
		}
	}

	private TreeSet<String> findComponentClasses(Component component) {
		final TreeSet<String> componentClasses = new TreeSet<>();
		bindingState.getMetadataBuildingContext().getMetadataCollector().visitRegisteredComponents(
				c -> {
					if ( component.getStructName().equals( c.getStructName() ) ) {
						componentClasses.add( c.getComponentClassName() );
					}
				}
		);
		return componentClasses;
	}
}
