/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.annotations.Bag;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Convert;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;

/**
 * Binds the first supported element-collection shape: a basic element collection
 * with an explicit collection table.
 */
class ElementCollectionAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final PersistentClass ownerBinding;
	private final AttributeMetadata attributeMetadata;
	private final BindingOptions bindingOptions;
	private final BindingState bindingState;
	private final BindingContext bindingContext;

	ElementCollectionAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		this.ownerType = ownerType;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	Collection bind(Property property) {
		final MemberDetails member = attributeMetadata.getMember();
		final CollectionTable collectionTable = member.getDirectAnnotationUsage( CollectionTable.class );
		if ( collectionTable == null || StringHelper.isEmpty( collectionTable.name() ) ) {
			// todo: implement implicit collection-table naming using the configured
			//  ImplicitNamingStrategy from BindingContext#getBootstrapContext(), with the
			//  MetadataBuildingContext from BindingState for the naming source.
			throw new UnsupportedOperationException( "Implicit @CollectionTable names are not yet implemented" );
		}

		final Table table = bindCollectionTable( collectionTable );
		final Collection collection = createCollection( member );
		collection.setRole( ownerBinding.getEntityName() + "." + attributeMetadata.getName() );
		collection.setCollectionTable( table );
		collection.setInverse( false );
		collection.setMutable( true );
		collection.setOptimisticLocked( true );
		collection.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );

		final Value element = bindElementValue( member, collection, table );
		collection.setElement( element );
		if ( collection instanceof org.hibernate.mapping.Map map ) {
			bindMapKey( member, map, table );
		}
		else if ( collection instanceof IndexedCollection indexedCollection ) {
			bindListIndex( member, indexedCollection, table );
		}

		final List<JoinColumn> joinColumns = listJoinColumns( collectionTable.joinColumns() );
		final IdentifierBinding ownerIdentifierBinding = bindingState.getIdentifierBinding( ownerType.getHierarchy().getRoot() );
		if ( ownerIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for element collection owner - "
							+ ownerType.getClassDetails().getClassName()
			);
		}
		if ( !joinColumns.isEmpty() && joinColumns.size() != ownerIdentifierBinding.columns().size() ) {
			throw new MappingException(
					"Collection table join column count did not match owner identifier column count - "
							+ ownerType.getClassDetails().getClassName()
			);
		}
		bindingState.addCollectionTableBinding( new CollectionTableBinding(
				collection,
				joinColumns,
				ForeignKeySource.from( collectionTable ),
				collectionTable.uniqueConstraints(),
				collectionTable.indexes()
		) );
		bindingState.getMetadataBuildingContext().getMetadataCollector().addCollectionBinding( collection );
		return collection;
	}

	private Collection createCollection(MemberDetails member) {
		final Class<?> collectionType = member.getType().determineRawClass().toJavaClass();
		if ( java.util.Set.class.isAssignableFrom( collectionType ) ) {
			return new org.hibernate.mapping.Set( bindingState.getMetadataBuildingContext(), ownerBinding );
		}
		if ( java.util.List.class.isAssignableFrom( collectionType )
				&& !member.hasDirectAnnotationUsage( Bag.class ) ) {
			return new org.hibernate.mapping.List( bindingState.getMetadataBuildingContext(), ownerBinding );
		}
		if ( java.util.Map.class.isAssignableFrom( collectionType ) ) {
			return new org.hibernate.mapping.Map( bindingState.getMetadataBuildingContext(), ownerBinding );
		}
		return new org.hibernate.mapping.Bag( bindingState.getMetadataBuildingContext(), ownerBinding );
	}

	private void bindListIndex(MemberDetails member, IndexedCollection collection, Table table) {
		final OrderColumn orderColumn = member.getDirectAnnotationUsage( OrderColumn.class );

		final BasicValue index = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		index.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.listIndex( member ),
				null,
				index,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final org.hibernate.mapping.Column indexColumn = ColumnBinder.bindColumn(
				ColumnSource.from( orderColumn ),
				() -> IndexedCollection.DEFAULT_INDEX_COLUMN_NAME
		);
		table.addColumn( indexColumn );
		index.addColumn(
				indexColumn,
				orderColumn == null || orderColumn.insertable(),
				orderColumn == null || orderColumn.updatable()
		);
		collection.setIndex( index );
	}

	private void bindMapKey(MemberDetails member, org.hibernate.mapping.Map collection, Table table) {
		final MapKeyColumn mapKeyColumn = member.getDirectAnnotationUsage( MapKeyColumn.class );

		final BasicValue index = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		index.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.mapKey( member ),
				null,
				index,
				bindingOptions,
				bindingState,
				bindingContext
		);
		bindMapKeyConversion( member, index );

		final org.hibernate.mapping.Column indexColumn = ColumnBinder.bindColumn(
				ColumnSource.from( mapKeyColumn ),
				() -> Collection.DEFAULT_KEY_COLUMN_NAME,
				false,
				false
		);
		table.addColumn( indexColumn );
		index.addColumn(
				indexColumn,
				mapKeyColumn == null || mapKeyColumn.insertable(),
				mapKeyColumn == null || mapKeyColumn.updatable()
		);
		collection.setIndex( index );
	}

	private void bindMapKeyConversion(MemberDetails member, BasicValue index) {
		final Convert conversion = locateMapKeyConversion( member );
		if ( conversion != null && !conversion.disableConversion() ) {
			final Class<AttributeConverter<?, ?>> javaClass = (Class<AttributeConverter<?, ?>>) conversion.converter();
			index.setJpaAttributeConverterDescriptor(
					new RegisteredConversion( null, javaClass, false ).getConverterDescriptor()
			);
		}
	}

	private Convert locateMapKeyConversion(MemberDetails member) {
		final var modelsContext = bindingContext.getBootstrapContext().getModelsContext();
		for ( Convert conversion : member.getRepeatedAnnotationUsages( Convert.class, modelsContext ) ) {
			if ( "key".equals( conversion.attributeName() ) ) {
				return conversion;
			}
		}
		return null;
	}

	private Table bindCollectionTable(CollectionTable collectionTable) {
		// todo: route explicit catalog/schema/table names through BindingHelper so
		//  global quoting and the configured JdbcEnvironment are applied consistently.
		implicitNamingStrategy( bindingContext );
		final Identifier logicalName = Identifier.toIdentifier( collectionTable.name() );
		final Identifier schemaName = StringHelper.isEmpty( collectionTable.schema() )
				? bindingOptions.getDefaultSchemaName()
				: Identifier.toIdentifier( collectionTable.schema() );
		final Identifier catalogName = StringHelper.isEmpty( collectionTable.catalog() )
				? bindingOptions.getDefaultCatalogName()
				: Identifier.toIdentifier( collectionTable.catalog() );

		final Table table = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				schemaName == null ? null : schemaName.getCanonicalName(),
				catalogName == null ? null : catalogName.getCanonicalName(),
				logicalName.getCanonicalName(),
				null,
				false,
				bindingState.getMetadataBuildingContext(),
				false
		);
		if ( StringHelper.isNotEmpty( collectionTable.options() ) ) {
			table.setOptions( collectionTable.options() );
		}
		return table;
	}

	private Value bindElementValue(MemberDetails member, Collection collection, Table table) {
		if ( isEmbeddableElement( member ) ) {
			return bindEmbeddableElementValue( member, collection, table );
		}
		return bindBasicElementValue( member, table );
	}

	private boolean isEmbeddableElement(MemberDetails member) {
		final ClassDetails elementType = member.getElementType().determineRawClass();
		return elementType.hasDirectAnnotationUsage( Embeddable.class )
				|| member.hasDirectAnnotationUsage( Embedded.class );
	}

	private Component bindEmbeddableElementValue(MemberDetails member, Collection collection, Table table) {
		final ClassDetails elementType = member.getElementType().determineRawClass();
		final Component component = new Component( bindingState.getMetadataBuildingContext(), collection );
		component.setEmbedded( true );
		component.setComponentClassName( elementType.getClassName() );
		component.setTable( table );
		component.setRoleName( collection.getRole() );

		final OverrideAndConverterCollector overrideAndConverterCollector = new OverrideAndConverterCollector(
				member,
				bindingContext
		);
		new ComponentBinder( bindingState, bindingOptions, bindingContext ).bindBasicProperties(
				ownerType,
				ownerBinding,
				elementType,
				component,
				table,
				(path, elementMember) -> {
					final var override = overrideAndConverterCollector.locateAttributeOverride( path );
					if ( override != null ) {
						return ColumnSource.from( (jakarta.persistence.Column) override.column() );
					}
					final jakarta.persistence.Column column = elementMember.getDirectAnnotationUsage( jakarta.persistence.Column.class );
					return ColumnSource.from( column );
				},
				(path, elementMember) -> resolveConversion( overrideAndConverterCollector, path, elementMember ),
				(path, elementMember) -> overrideAndConverterCollector.locateAssociationOverride( path ),
				(ignored, column) -> table.addColumn( column ),
				false,
				true,
				true
		);
		return component;
	}

	private Convert resolveConversion(
			OverrideAndConverterCollector overrideAndConverterCollector,
			String path,
			MemberDetails elementMember) {
		final Convert override = overrideAndConverterCollector.locateConversion( path );
		if ( override != null ) {
			return override;
		}

		final Convert direct = elementMember.getDirectAnnotationUsage( Convert.class );
		return direct != null && StringHelper.isEmpty( direct.attributeName() ) ? direct : null;
	}

	private BasicValue bindBasicElementValue(MemberDetails member, Table table) {
		final BasicValue element = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		element.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.collectionElement( member ),
				null,
				element,
				bindingOptions,
				bindingState,
				bindingContext
		);

		final jakarta.persistence.Column column = member.getDirectAnnotationUsage( jakarta.persistence.Column.class );
		final org.hibernate.mapping.Column elementColumn = ColumnBinder.bindColumn(
				ColumnSource.from( column ),
				() -> Collection.DEFAULT_ELEMENT_COLUMN_NAME
		);
		table.addColumn( elementColumn );
		element.addColumn( elementColumn );
		return element;
	}

	private static org.hibernate.boot.model.naming.ImplicitNamingStrategy implicitNamingStrategy(BindingContext bindingContext) {
		return bindingContext.getBootstrapContext()
				.getMetadataBuildingOptions()
				.getImplicitNamingStrategy();
	}

	private static List<JoinColumn> listJoinColumns(JoinColumn[] joinColumns) {
		if ( joinColumns.length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( joinColumns.length );
		for ( JoinColumn joinColumn : joinColumns ) {
			result.add( joinColumn );
		}
		return result;
	}
}
