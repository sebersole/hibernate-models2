/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.internal.sources.CollectionSource;
import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

/// Binds association-valued plural attributes.
///
/// @author Steve Ebersole
class PluralAssociationAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final PersistentClass ownerBinding;
	private final AttributeMetadata attributeMetadata;
	private final ModelBinders modelBinders;
	private final BindingOptions bindingOptions;
	private final BindingState bindingState;
	private final BindingContext bindingContext;

	PluralAssociationAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		this.ownerType = ownerType;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.modelBinders = modelBinders;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	Collection bindManyToMany(Property property) {
		final CollectionSource source = CollectionSource.manyToMany( attributeMetadata.getMember() );
		final ManyToMany manyToMany = source.manyToMany();
		if ( manyToMany != null && StringHelper.isNotEmpty( manyToMany.mappedBy() ) ) {
			return bindInverseManyToMany( source, manyToMany.mappedBy() );
		}
		return bindAssociation( source, false );
	}

	Collection bindOneToMany(Property property) {
		final CollectionSource source = CollectionSource.oneToMany( attributeMetadata.getMember() );
		final OneToMany oneToMany = source.oneToMany();
		if ( oneToMany != null && StringHelper.isNotEmpty( oneToMany.mappedBy() ) ) {
			return bindInverseOneToMany( source, oneToMany.mappedBy() );
		}
		return bindAssociation( source, true );
	}

	private Collection bindInverseManyToMany(CollectionSource source, String mappedBy) {
		if ( source.classification().toJpaClassification() == jakarta.persistence.metamodel.PluralAttribute.CollectionType.MAP ) {
			throw new UnsupportedOperationException( "Map-valued plural associations are not yet implemented" );
		}

		final ClassDetails targetClassDetails = resolveTargetClassDetails( source );
		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + attributeMetadata.getName() );
		collection.setInverse( true );
		collection.setMappedByProperty( mappedBy );
		collection.setMutable( true );
		collection.setOptimisticLocked( true );
		collection.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );

		bindingState.addInversePluralAssociationBinding( new InversePluralAssociationBinding(
				InversePluralAssociationBinding.Nature.MANY_TO_MANY,
				ownerType,
				ownerBinding,
				attributeMetadata,
				collection,
				targetClassDetails,
				mappedBy
		) );
		bindingState.getMetadataBuildingContext().getMetadataCollector().addCollectionBinding( collection );
		return collection;
	}

	private Collection bindInverseOneToMany(CollectionSource source, String mappedBy) {
		if ( source.classification().toJpaClassification() == jakarta.persistence.metamodel.PluralAttribute.CollectionType.MAP ) {
			throw new UnsupportedOperationException( "Map-valued plural associations are not yet implemented" );
		}

		final ClassDetails targetClassDetails = resolveTargetClassDetails( source );
		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + attributeMetadata.getName() );
		collection.setInverse( true );
		collection.setMappedByProperty( mappedBy );
		collection.setMutable( true );
		collection.setOptimisticLocked( true );
		collection.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );

		bindingState.addInversePluralAssociationBinding( new InversePluralAssociationBinding(
				InversePluralAssociationBinding.Nature.ONE_TO_MANY,
				ownerType,
				ownerBinding,
				attributeMetadata,
				collection,
				targetClassDetails,
				mappedBy
		) );
		bindingState.getMetadataBuildingContext().getMetadataCollector().addCollectionBinding( collection );
		return collection;
	}

	private Collection bindAssociation(CollectionSource source, boolean uniqueTargetColumns) {
		final TargetEntityBinding target = resolveTargetEntityBinding( source );
		final Table table = modelBinders.getTableBinder()
				.bindAssociationTable(
						resolveOwnerEntityType(),
						ownerBinding.getTable(),
						attributeMetadata.getName(),
						target.entityType(),
						target.primaryTable(),
						source.joinTable()
				)
				.binding();

		final Collection collection = createCollection( source );
		collection.setRole( ownerBinding.getEntityName() + "." + attributeMetadata.getName() );
		collection.setCollectionTable( table );
		collection.setInverse( false );
		collection.setMutable( true );
		collection.setOptimisticLocked( true );
		collection.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );

		final ManyToOne element = bindElementValue( source, target, table, uniqueTargetColumns );
		collection.setElement( element );
		if ( collection instanceof org.hibernate.mapping.Map map ) {
			CollectionIndexBinder.bindBasicMapKey(
					source,
					map,
					table,
					bindingOptions,
					bindingState,
					bindingContext
			);
		}
		bindingState.addCollectionTableBinding( new CollectionTableBinding(
				collection,
				source.associationJoinColumns(),
				ForeignKeySource.from( source.joinTable() ),
				source.joinTable() == null ? new jakarta.persistence.UniqueConstraint[0] : source.joinTable().uniqueConstraints(),
				source.joinTable() == null ? new jakarta.persistence.Index[0] : source.joinTable().indexes()
		) );
		bindingState.getMetadataBuildingContext().getMetadataCollector().addCollectionBinding( collection );
		return collection;
	}

	private Collection createCollection(CollectionSource source) {
		return switch ( source.classification() ) {
			case SET, ORDERED_SET, SORTED_SET -> new org.hibernate.mapping.Set( bindingState.getMetadataBuildingContext(), ownerBinding );
			case LIST -> new org.hibernate.mapping.List( bindingState.getMetadataBuildingContext(), ownerBinding );
			case BAG -> new org.hibernate.mapping.Bag( bindingState.getMetadataBuildingContext(), ownerBinding );
			case MAP, ORDERED_MAP, SORTED_MAP -> new org.hibernate.mapping.Map( bindingState.getMetadataBuildingContext(), ownerBinding );
			case ARRAY, ID_BAG -> throw new UnsupportedOperationException(
					source.classification() + " plural associations are not yet implemented"
			);
		};
	}

	private ManyToOne bindElementValue(
			CollectionSource source,
			TargetEntityBinding target,
			Table table,
			boolean uniqueByDefault) {
		final ManyToOne element = new ManyToOne( bindingState.getMetadataBuildingContext(), table );
		element.setReferencedEntityName( target.entityName() );
		element.setReferenceToPrimaryKey( true );
		element.setTypeName( target.entityName() );
		element.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );

		bindJoinColumns(
				source.associationInverseJoinColumns(),
				element,
				target,
				table,
				uniqueByDefault,
				attributeMetadata.getName()
		);
		element.createForeignKey();
		applyInverseForeignKey( source, table, target );
		return element;
	}

	private void bindJoinColumns(
			List<JoinColumn> joinColumnAnns,
			ManyToOne value,
			TargetEntityBinding target,
			Table table,
			boolean uniqueByDefault,
			String propertyName) {
		final List<org.hibernate.mapping.Column> targetColumns = target.identifierColumns();

		if ( !joinColumnAnns.isEmpty() && joinColumnAnns.size() != targetColumns.size() ) {
			throw new MappingException(
					"Plural association inverse join column count did not match target identifier column count - "
							+ ownerType.getClassDetails().getClassName() + "." + propertyName
			);
		}

		final List<JoinColumn> orderedJoinColumns = ToOneAttributeBinder.orderJoinColumns(
				joinColumnAnns,
				targetColumns,
				ownerType.getClassDetails().getClassName(),
				propertyName
		);
		for ( int i = 0; i < targetColumns.size(); i++ ) {
			final org.hibernate.mapping.Column targetColumn = targetColumns.get( i );
			final JoinColumn joinColumnAnn = orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i );
			final org.hibernate.mapping.Column column = ColumnBinder.bindColumn(
					org.hibernate.boot.models.bind.internal.sources.ColumnSource.from( joinColumnAnn ),
					() -> propertyName + "_" + targetColumn.getName(),
					uniqueByDefault,
					false
			);
			if ( uniqueByDefault ) {
				column.setUnique( true );
			}
			table.addColumn( column );
			value.addColumn( column );
		}
	}

	private void applyInverseForeignKey(
			CollectionSource source,
			Table table,
			TargetEntityBinding target) {
		final ForeignKeySource foreignKeySource = ForeignKeySource.inverseFrom( source.joinTable() );
		if ( foreignKeySource == null ) {
			return;
		}

		for ( ForeignKey foreignKey : table.getForeignKeyCollection() ) {
			if ( target.entityName().equals( foreignKey.getReferencedEntityName() ) ) {
				if ( foreignKeySource.isNoConstraint() ) {
					foreignKey.disableCreation();
				}
				if ( StringHelper.isNotEmpty( foreignKeySource.name() ) ) {
					foreignKey.setName( foreignKeySource.name() );
				}
				if ( StringHelper.isNotEmpty( foreignKeySource.definition() ) ) {
					foreignKey.setKeyDefinition( foreignKeySource.definition() );
				}
				return;
			}
		}
	}

	private TargetEntityBinding resolveTargetEntityBinding(CollectionSource source) {
		final ClassDetails targetClassDetails = resolveTargetClassDetails( source );
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				targetClassDetails
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for plural association target entity - "
							+ targetClassDetails.getClassName()
			);
		}

		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding(
				targetTypeBinder.getManagedType().getHierarchy().getRoot()
		);
		if ( identifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for plural association target entity - "
							+ targetTypeBinder.getTypeBinding().getEntityName()
			);
		}

		return new TargetEntityBinding(
				targetTypeBinder.getTypeBinding().getEntityName(),
				targetTypeBinder.getManagedType(),
				targetTypeBinder.getTable(),
				identifierBinding.columns()
		);
	}

	private ClassDetails resolveTargetClassDetails(CollectionSource source) {
		final ManyToMany manyToMany = source.manyToMany();
		if ( manyToMany != null && manyToMany.targetEntity() != void.class ) {
			return bindingContext.getClassDetailsRegistry().resolveClassDetails( manyToMany.targetEntity().getName() );
		}
		final OneToMany oneToMany = source.oneToMany();
		if ( oneToMany != null && oneToMany.targetEntity() != void.class ) {
			return bindingContext.getClassDetailsRegistry().resolveClassDetails( oneToMany.targetEntity().getName() );
		}
		return source.elementType().determineRawClass();
	}

	private EntityTypeMetadata resolveOwnerEntityType() {
		if ( ownerType instanceof EntityTypeMetadata entityType ) {
			return entityType;
		}
		return ownerType.getHierarchy().getRoot();
	}

	private record TargetEntityBinding(
			String entityName,
			EntityTypeMetadata entityType,
			Table primaryTable,
			List<org.hibernate.mapping.Column> identifierColumns) {
	}
}
