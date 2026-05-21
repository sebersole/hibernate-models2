/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToOne;

/**
 * Binds simple owning to-one associations.
 */
class ToOneAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final AttributeMetadata attributeMetadata;
	private final Table primaryTable;
	private final BindingState bindingState;
	private final BindingContext bindingContext;

	ToOneAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			AttributeMetadata attributeMetadata,
			Table primaryTable,
			@SuppressWarnings("unused") BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		this.ownerType = ownerType;
		this.attributeMetadata = attributeMetadata;
		this.primaryTable = primaryTable;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	ManyToOne bind(Property property) {
		final MemberDetails member = attributeMetadata.getMember();
		final jakarta.persistence.ManyToOne manyToOneAnn = member.getDirectAnnotationUsage( jakarta.persistence.ManyToOne.class );
		final OneToOne oneToOneAnn = member.getDirectAnnotationUsage( OneToOne.class );
		if ( oneToOneAnn != null && StringHelper.isNotEmpty( oneToOneAnn.mappedBy() ) ) {
			throw new UnsupportedOperationException( "Inverse @OneToOne is not yet implemented" );
		}

		final TargetEntityBinding target = resolveTargetEntityBinding( member, manyToOneAnn, oneToOneAnn );

		final ManyToOne value = new ManyToOne(
				bindingState.getMetadataBuildingContext(),
				resolveAssociationTable( member )
		);
		value.setReferencedEntityName( target.entityName() );
		value.setReferenceToPrimaryKey( true );
		value.setTypeName( target.entityName() );
		value.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );
		value.setLazy( resolveFetchType( manyToOneAnn, oneToOneAnn ) == FetchType.LAZY );

		final boolean logicalOneToOne = oneToOneAnn != null;
		if ( logicalOneToOne ) {
			value.markAsLogicalOneToOne();
		}

		final boolean optional = resolveOptionality( manyToOneAnn, oneToOneAnn );
		property.setOptional( optional );

		bindJoinColumns( member, value, target, logicalOneToOne, optional );
		value.createForeignKey();
		return value;
	}

	private void bindJoinColumns(
			MemberDetails member,
			ManyToOne value,
			TargetEntityBinding target,
			boolean uniqueByDefault,
			boolean optional) {
		final List<JoinColumn> joinColumnAnns = resolveJoinColumns( member );
		final List<Column> targetColumns = target.identifierColumns();

		if ( joinColumnAnns.size() > 1 && joinColumnAnns.size() != targetColumns.size() ) {
			throw new MappingException(
					"Composite to-one join column count did not match target identifier column count - "
							+ ownerType.getClassDetails().getClassName() + "." + attributeMetadata.getName()
			);
		}

		for ( int i = 0; i < targetColumns.size(); i++ ) {
			final Column targetColumn = targetColumns.get( i );
			final JoinColumn joinColumnAnn = joinColumnAnns.isEmpty() ? null : joinColumnAnns.get( i );
			final Column column = ColumnBinder.bindColumn(
					ColumnSource.from( joinColumnAnn ),
					() -> attributeMetadata.getName() + "_" + targetColumn.getName(),
					uniqueByDefault,
					optional
			);
			if ( uniqueByDefault ) {
				column.setUnique( true );
			}
			if ( !optional ) {
				column.setNullable( false );
			}
			value.addColumn( column );
		}
	}

	private Table resolveAssociationTable(MemberDetails member) {
		final JoinColumn joinColumn = member.getDirectAnnotationUsage( JoinColumn.class );
		if ( joinColumn == null || StringHelper.isEmpty( joinColumn.table() ) ) {
			return primaryTable;
		}

		final Identifier identifier = Identifier.toIdentifier( joinColumn.table() );
		final TableReference tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
		return tableByName.binding();
	}

	private List<JoinColumn> resolveJoinColumns(MemberDetails member) {
		final JoinColumns joinColumnsAnn = member.getDirectAnnotationUsage( JoinColumns.class );
		if ( joinColumnsAnn != null ) {
			final ArrayList<JoinColumn> result = new ArrayList<>( joinColumnsAnn.value().length );
			for ( JoinColumn joinColumn : joinColumnsAnn.value() ) {
				result.add( joinColumn );
			}
			return result;
		}

		final JoinColumn joinColumnAnn = member.getDirectAnnotationUsage( JoinColumn.class );
		return joinColumnAnn == null ? List.of() : List.of( joinColumnAnn );
	}

	private TargetEntityBinding resolveTargetEntityBinding(
			MemberDetails member,
			jakarta.persistence.ManyToOne manyToOneAnn,
			OneToOne oneToOneAnn) {
		final ClassDetails targetClassDetails = resolveTargetClassDetails( member, manyToOneAnn, oneToOneAnn );
		final EntityTypeBinder targetTypeBinder = (EntityTypeBinder) bindingState.getTypeBinder(
				targetClassDetails
		);
		if ( targetTypeBinder == null ) {
			throw new MappingException(
					"Could not resolve local type binding for to-one target entity - "
							+ targetClassDetails.getClassName()
			);
		}

		final IdentifierBinding identifierBinding = bindingState.getIdentifierBinding(
				targetTypeBinder.getManagedType().getHierarchy().getRoot()
		);
		if ( identifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for to-one target entity - "
							+ targetTypeBinder.getTypeBinding().getEntityName()
			);
		}

		return new TargetEntityBinding(
				targetTypeBinder.getTypeBinding().getEntityName(),
				identifierBinding.columns()
		);
	}

	private ClassDetails resolveTargetClassDetails(
			MemberDetails member,
			jakarta.persistence.ManyToOne manyToOneAnn,
			OneToOne oneToOneAnn) {
		if ( manyToOneAnn != null && manyToOneAnn.targetEntity() != void.class ) {
			return bindingContext.getClassDetailsRegistry().resolveClassDetails( manyToOneAnn.targetEntity().getName() );
		}
		else if ( oneToOneAnn != null && oneToOneAnn.targetEntity() != void.class ) {
			return bindingContext.getClassDetailsRegistry().resolveClassDetails( oneToOneAnn.targetEntity().getName() );
		}
		else {
			return member.getType().determineRawClass();
		}
	}

	private FetchType resolveFetchType(jakarta.persistence.ManyToOne manyToOneAnn, OneToOne oneToOneAnn) {
		if ( manyToOneAnn != null ) {
			return manyToOneAnn.fetch();
		}
		return oneToOneAnn.fetch();
	}

	private boolean resolveOptionality(jakarta.persistence.ManyToOne manyToOneAnn, OneToOne oneToOneAnn) {
		if ( manyToOneAnn != null ) {
			return manyToOneAnn.optional();
		}
		return oneToOneAnn.optional();
	}

	private record TargetEntityBinding(
			String entityName,
			List<Column> identifierColumns) {
	}
}
