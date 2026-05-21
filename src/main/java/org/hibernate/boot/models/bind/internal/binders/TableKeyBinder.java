/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.models.ModelsException;

/// Binds table keys that depend on an already-bound entity hierarchy identifier.
///
/// @author Steve Ebersole
public class TableKeyBinder {
	private final EntityTypeBinder entityBinder;
	private final BindingState bindingState;

	public TableKeyBinder(EntityTypeBinder entityBinder) {
		this.entityBinder = entityBinder;
		this.bindingState = entityBinder.getBindingState();
	}

	public void bindTableKeys() {
		if ( entityBinder.getTypeBinding() instanceof JoinedSubclass joinedSubclass ) {
			bindJoinedSubclassKey( joinedSubclass );
		}

		entityBinder.getTypeBinding().getJoins().forEach( this::bindSecondaryTableKey );
	}

	private void bindJoinedSubclassKey(JoinedSubclass joinedSubclass) {
		final IdentifierBinding rootIdentifierBinding = resolveIdentifierBinding();
		final DependantValue key = createDependentKeyValue( joinedSubclass.getTable(), rootIdentifierBinding );
		joinedSubclass.setKey( key );
		createPrimaryKey( joinedSubclass.getTable(), key );
		key.createForeignKeyOfEntity( entityBinder.getSuperEntityBinder().getTypeBinding().getEntityName() );
	}

	private void bindSecondaryTableKey(Join join) {
		final IdentifierBinding rootIdentifierBinding = resolveIdentifierBinding();
		final DependantValue key = createDependentKeyValue( join.getTable(), rootIdentifierBinding );
		join.setKey( key );
		join.createPrimaryKey();
		if ( !join.isInverse() ) {
			join.createForeignKey();
		}
	}

	private IdentifierBinding resolveIdentifierBinding() {
		final EntityTypeMetadata rootType = entityBinder.getManagedType().getHierarchy().getRoot();
		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding( rootType );
		if ( identifierBinding == null ) {
			throw new ModelsException( "Identifier binding not available for " + rootType.getEntityName() );
		}
		return identifierBinding;
	}

	private DependantValue createDependentKeyValue(Table table, IdentifierBinding identifierBinding) {
		final DependantValue key = new DependantValue(
				bindingState.getMetadataBuildingContext(),
				table,
				identifierBinding.value()
		);
		key.setNullable( false );
		key.setUpdateable( false );
		for ( Column identifierColumn : identifierBinding.columns() ) {
			key.addColumn( copyKeyColumn( identifierColumn ), true, false );
		}
		return key;
	}

	private Column copyKeyColumn(Column source) {
		// todo : is this enough detail?
		final Column result = new Column( source.getName() );
		result.setLength( source.getLength() );
		result.setPrecision( source.getPrecision() );
		result.setScale( source.getScale() );
		result.setSqlType( source.getSqlType() );
		result.setNullable( false );
		result.setUnique( source.isUnique() );
		return result;
	}

	private void createPrimaryKey(Table table, KeyValue key) {
		final PrimaryKey primaryKey = new PrimaryKey( table );
		table.setPrimaryKey( primaryKey );
		key.getColumns().forEach( primaryKey::addColumn );
	}
}
