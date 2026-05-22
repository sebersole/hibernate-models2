/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;

/// Resolves inverse plural associations from their owning-side mapping objects.
///
/// @author Steve Ebersole
class InversePluralAssociationBinder {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;

	InversePluralAssociationBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
	}

	void bindInverseAssociations() {
		bindingState.forEachInversePluralAssociationBinding( (inverseBinding) -> {
			if ( inverseBinding.ownerBinding() == entityBinder.getTypeBinding() ) {
				switch ( inverseBinding.nature() ) {
					case MANY_TO_MANY -> bindInverseManyToMany( inverseBinding );
					case ONE_TO_MANY -> bindInverseOneToMany( inverseBinding );
				}
			}
		} );
	}

	private void bindInverseManyToMany(InversePluralAssociationBinding inverseBinding) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				inverseBinding.targetClassDetails()
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for inverse plural association target entity - "
							+ inverseBinding.targetClassDetails().getClassName()
			);
		}

		final Property owningProperty = targetTypeBinder.getTypeBinding().getProperty( inverseBinding.mappedBy() );
		if ( !( owningProperty.getValue() instanceof Collection owningCollection ) ) {
			throw new MappingException(
					"Inverse plural association mappedBy did not name a collection-valued owning attribute - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( owningCollection.isInverse() ) {
			throw new MappingException(
					"Inverse plural association mappedBy named another inverse collection - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( !( owningCollection.getElement() instanceof ManyToOne owningElement ) ) {
			throw new MappingException(
					"Inverse @ManyToMany mappedBy did not name an owning many-to-many collection - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( owningCollection.getKey() == null ) {
			throw new MappingException(
					"Owning many-to-many collection key was not available while resolving mappedBy - "
							+ owningCollection.getRole()
			);
		}

		final Collection inverseCollection = inverseBinding.collection();
		final Table collectionTable = owningCollection.getCollectionTable();
		inverseCollection.setCollectionTable( collectionTable );
		inverseCollection.setKey( createInverseKey( inverseBinding, collectionTable, owningElement ) );
		inverseCollection.setElement( createInverseElement( inverseBinding, collectionTable, targetTypeBinder, owningCollection ) );
		inverseCollection.createAllKeys();
	}

	private void bindInverseOneToMany(InversePluralAssociationBinding inverseBinding) {
		final EntityTypeBinder targetTypeBinder = resolveTargetTypeBinder( inverseBinding );
		final Property owningProperty = targetTypeBinder.getTypeBinding().getProperty( inverseBinding.mappedBy() );
		final Value owningValue = owningProperty.getValue();
		if ( !( owningValue instanceof ManyToOne owningToOne ) ) {
			throw new MappingException(
					"Inverse @OneToMany mappedBy did not name an owning to-one attribute - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}
		if ( !inverseBinding.ownerBinding().getEntityName().equals( owningToOne.getReferencedEntityName() ) ) {
			throw new MappingException(
					"Inverse @OneToMany mappedBy named a to-one attribute that targets `"
							+ owningToOne.getReferencedEntityName() + "` rather than `"
							+ inverseBinding.ownerBinding().getEntityName() + "` - "
							+ inverseBinding.ownerType().getClassDetails().getClassName()
							+ "." + inverseBinding.attributeMetadata().getName()
			);
		}

		final Collection inverseCollection = inverseBinding.collection();
		final Table collectionTable = owningToOne.getTable();
		inverseCollection.setCollectionTable( collectionTable );
		inverseCollection.setKey( createInverseKey( inverseBinding, collectionTable, owningToOne ) );
		inverseCollection.setElement( createOneToManyElement( inverseBinding, targetTypeBinder ) );
		inverseCollection.createAllKeys();
	}

	private KeyValue createInverseKey(
			InversePluralAssociationBinding inverseBinding,
			Table collectionTable,
			ManyToOne owningElement) {
		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding(
				inverseBinding.ownerType().getHierarchy().getRoot()
		);
		if ( identifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for inverse plural association owner - "
							+ inverseBinding.ownerBinding().getEntityName()
			);
		}

		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				collectionTable,
				identifierBinding.value()
		);
		key.setNullable( false );
		key.setUpdateable( false );
		for ( Column owningElementColumn : owningElement.getColumns() ) {
			key.addColumn( copyColumn( collectionTable, owningElementColumn, false ), true, false );
		}
		return key;
	}

	private ManyToOne createInverseElement(
			InversePluralAssociationBinding inverseBinding,
			Table collectionTable,
			EntityTypeBinder targetTypeBinder,
			Collection owningCollection) {
		final ManyToOne element = new ManyToOne( bindingState.getMetadataBuildingContext(), collectionTable );
		element.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setReferenceToPrimaryKey( true );
		element.setTypeName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setTypeUsingReflection(
				inverseBinding.ownerType().getClassDetails().getClassName(),
				inverseBinding.attributeMetadata().getName()
		);
		for ( Column owningKeyColumn : owningCollection.getKey().getColumns() ) {
			element.addColumn( copyColumn( collectionTable, owningKeyColumn, owningKeyColumn.isUnique() ) );
		}
		return element;
	}

	private OneToMany createOneToManyElement(
			InversePluralAssociationBinding inverseBinding,
			EntityTypeBinder targetTypeBinder) {
		final OneToMany element = new OneToMany(
				bindingState.getMetadataBuildingContext(),
				inverseBinding.ownerBinding()
		);
		element.setAssociatedClass( targetTypeBinder.getTypeBinding() );
		element.setReferencedEntityName( targetTypeBinder.getTypeBinding().getEntityName() );
		element.setTypeUsingReflection(
				inverseBinding.ownerType().getClassDetails().getClassName(),
				inverseBinding.attributeMetadata().getName()
		);
		return element;
	}

	private EntityTypeBinder resolveTargetTypeBinder(InversePluralAssociationBinding inverseBinding) {
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				inverseBinding.targetClassDetails()
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for inverse plural association target entity - "
							+ inverseBinding.targetClassDetails().getClassName()
			);
		}
		return targetTypeBinder;
	}

	private Column copyColumn(Table table, Column source, boolean unique) {
		final Column result = new Column( source.getName() );
		result.setLength( source.getLength() );
		result.setPrecision( source.getPrecision() );
		result.setScale( source.getScale() );
		result.setSqlType( source.getSqlType() );
		result.setNullable( false );
		result.setUnique( unique );
		table.addColumn( result );
		final Column canonicalColumn = table.getColumn( result );
		return canonicalColumn == null ? result : canonicalColumn;
	}
}
