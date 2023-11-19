/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.KeyMapping;

/**
 * @author Steve Ebersole
 */
public abstract class IdentifiableTypeBinder extends ManagedTypeBinder {
	private final IdentifiableTypeMetadata superType;
	private final EntityHierarchy.HierarchyRelation hierarchyRelation;

	private final List<AttributeBinder> attributeBinders;
	private final IdentifiableTypeBinder superTypeBinder;

	public IdentifiableTypeBinder(
			IdentifiableTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		super( type, state, options, bindingContext );
		this.superType = superType;
		this.hierarchyRelation = hierarchyRelation;
		this.superTypeBinder = superType == null ? null : (IdentifiableTypeBinder) state.getTypeBinder( superType.getClassDetails() );
		this.attributeBinders = new ArrayList<>( type.getNumberOfAttributes() );
	}

	public abstract EntityTypeMetadata findSuperEntity();

	public EntityTypeBinder getSuperEntityBinder() {
		IdentifiableTypeBinder check = superTypeBinder;
		if ( check == null ) {
			return null;
		}

		do {
			if ( check.getTypeBinding() instanceof PersistentClass ) {
				return (EntityTypeBinder) check.getSuperTypeBinder();
			}
			check = check.getSuperTypeBinder();
		} while ( check != null );

		return null;
	}

	public IdentifiableTypeBinder getSuperTypeBinder() {
		return superTypeBinder;
	}

	public abstract IdentifiableTypeClass getTypeBinding();

	public IdentifiableTypeMetadata getSuperType() {
		return superType;
	}

	public EntityHierarchy.HierarchyRelation getHierarchyRelation() {
		return hierarchyRelation;
	}

	public abstract Table getTable();

	@Override
	public IdentifiableTypeMetadata getManagedType() {
		return (IdentifiableTypeMetadata) super.getManagedType();
	}

	@Override
	protected void prepareBinding(DelegateBinders delegateBinders) {
		final var table = getTable();
		final var managedType = getManagedType();

		managedType.forEachAttribute( (index, attributeMetadata) -> {
			final var attributeBinder = new AttributeBinder(
					attributeMetadata,
					getBindingState(),
					getOptions(),
					getBindingContext()
			);

			final var property = attributeBinder.getBinding();

			final var value = property.getValue();
			applyTable( value, table );

			processIdMapping( attributeMetadata, property );
			processNaturalId( attributeMetadata, property );

			attributeBinders.add( attributeBinder );
			getTypeBinding().applyProperty( property );
		} );

		super.prepareBinding( delegateBinders );
	}

	private void processIdMapping(AttributeMetadata attributeMetadata, Property property) {
		final KeyMapping idMapping = getManagedType().getHierarchy().getIdMapping();
		if ( !idMapping.contains( attributeMetadata ) ) {
			return;
		}

		// todo : do it
	}

	private void processNaturalId(AttributeMetadata attributeMetadata, Property property) {
		final var naturalIdAnn = attributeMetadata.getMember().getAnnotationUsage( NaturalId.class );
		if ( naturalIdAnn == null ) {
			return;
		}
		property.setNaturalIdentifier( true );
		property.setUpdateable( naturalIdAnn.getBoolean( "mutable" ) );
	}

	private void applyTable(Value value, Table table) {
		if ( value instanceof BasicValue ) {
			( (BasicValue) value ).setTable( table );
		}
	}

	@Override
	public void processSecondPasses() {
		attributeBinders.forEach( AttributeBinder::processSecondPasses );
	}

}
