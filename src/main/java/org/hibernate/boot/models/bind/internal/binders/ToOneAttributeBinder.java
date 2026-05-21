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
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.FetchType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;

/**
 * Binds simple owning to-one associations.
 */
class ToOneAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final PersistentClass ownerBinding;
	private final AttributeMetadata attributeMetadata;
	private final Table primaryTable;
	private final BindingOptions bindingOptions;
	private final BindingState bindingState;
	private final BindingContext bindingContext;

	ToOneAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			Table primaryTable,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		this.ownerType = ownerType;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.primaryTable = primaryTable;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;
	}

	ManyToOne bind(Property property) {
		final MemberDetails member = attributeMetadata.getMember();
		return bindToOne(
				ownerType,
				ownerBinding,
				ownerType.getClassDetails().getClassName(),
				attributeMetadata.getName(),
				member,
				property,
				primaryTable,
				null,
				bindingOptions,
				bindingState,
				bindingContext
		);
	}

	static ManyToOne bindToOne(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			String ownerClassName,
			String propertyName,
			MemberDetails member,
			Property property,
			Table primaryTable,
			AssociationOverride associationOverride,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final jakarta.persistence.ManyToOne manyToOneAnn = member.getDirectAnnotationUsage( jakarta.persistence.ManyToOne.class );
		final OneToOne oneToOneAnn = member.getDirectAnnotationUsage( OneToOne.class );
		if ( oneToOneAnn != null && StringHelper.isNotEmpty( oneToOneAnn.mappedBy() ) ) {
			throw new UnsupportedOperationException( "Inverse @OneToOne is not yet implemented" );
		}

		final TargetEntityBinding target = resolveTargetEntityBinding( member, manyToOneAnn, oneToOneAnn, bindingState, bindingContext );
		final JoinTable joinTable = resolveJoinTable( member, associationOverride );
		final Table associationTable = joinTable == null
				? resolveAssociationTable( member, associationOverride, primaryTable, bindingState )
				: bindAssociationTable(
						ownerType,
						ownerBinding,
						joinTable,
						bindingOptions,
						bindingState
				);

		final ManyToOne value = new ManyToOne(
				bindingState.getMetadataBuildingContext(),
				associationTable
		);
		value.setReferencedEntityName( target.entityName() );
		value.setReferenceToPrimaryKey( true );
		value.setTypeName( target.entityName() );
		value.setTypeUsingReflection( ownerClassName, propertyName );
		value.setLazy( resolveFetchType( manyToOneAnn, oneToOneAnn ) == FetchType.LAZY );

		final boolean logicalOneToOne = oneToOneAnn != null;
		if ( logicalOneToOne ) {
			value.markAsLogicalOneToOne();
		}

		final boolean optional = resolveOptionality( manyToOneAnn, oneToOneAnn );
		property.setOptional( optional );

		bindJoinColumns(
				resolveValueJoinColumns( member, associationOverride, joinTable ),
				value,
				target,
				logicalOneToOne,
				optional,
				ownerClassName,
				propertyName
		);
		value.createForeignKey();
		return value;
	}

	private static void bindJoinColumns(
			List<JoinColumn> joinColumnAnns,
			ManyToOne value,
			TargetEntityBinding target,
			boolean uniqueByDefault,
			boolean optional,
			String ownerClassName,
			String propertyName) {
		final List<Column> targetColumns = target.identifierColumns();

		if ( !joinColumnAnns.isEmpty() && joinColumnAnns.size() != targetColumns.size() ) {
			throw new MappingException(
					"Composite to-one join column count did not match target identifier column count - "
							+ ownerClassName + "." + propertyName
			);
		}

		final List<JoinColumn> orderedJoinColumns = orderJoinColumns(
				joinColumnAnns,
				targetColumns,
				ownerClassName,
				propertyName
		);
		for ( int i = 0; i < targetColumns.size(); i++ ) {
			final Column targetColumn = targetColumns.get( i );
			final JoinColumn joinColumnAnn = orderedJoinColumns.isEmpty() ? null : orderedJoinColumns.get( i );
			final Column column = ColumnBinder.bindColumn(
					ColumnSource.from( joinColumnAnn ),
					() -> propertyName + "_" + targetColumn.getName(),
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

	static List<JoinColumn> orderJoinColumns(
			List<JoinColumn> joinColumns,
			List<Column> targetColumns,
			String ownerClassName,
			String propertyName) {
		if ( joinColumns.isEmpty() || joinColumns.stream().noneMatch( (joinColumn) -> StringHelper.isNotEmpty( joinColumn.referencedColumnName() ) ) ) {
			return joinColumns;
		}

		final ArrayList<JoinColumn> orderedJoinColumns = new ArrayList<>( targetColumns.size() );
		final ArrayList<JoinColumn> unmatchedJoinColumns = new ArrayList<>( joinColumns );
		for ( Column targetColumn : targetColumns ) {
			final JoinColumn joinColumn = findJoinColumn(
					targetColumn,
					unmatchedJoinColumns,
					ownerClassName,
					propertyName
			);
			orderedJoinColumns.add( joinColumn );
			unmatchedJoinColumns.remove( joinColumn );
		}
		return orderedJoinColumns;
	}

	private static JoinColumn findJoinColumn(
			Column targetColumn,
			List<JoinColumn> joinColumns,
			String ownerClassName,
			String propertyName) {
		for ( JoinColumn joinColumn : joinColumns ) {
			if ( targetColumn.getName().equals( joinColumn.referencedColumnName() ) ) {
				return joinColumn;
			}
		}

		throw new MappingException(
				"Unable to match join column referencedColumnName to target identifier column `"
						+ targetColumn.getName() + "` - " + ownerClassName + "." + propertyName
		);
	}

	private static Table bindAssociationTable(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			JoinTable joinTable,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		if ( ownerBinding == null ) {
			throw new UnsupportedOperationException( "To-one @JoinTable inside embeddables is not yet implemented" );
		}
		if ( StringHelper.isEmpty( joinTable.name() ) ) {
			throw new UnsupportedOperationException( "Implicit to-one @JoinTable names are not yet implemented" );
		}

		final Identifier logicalName = Identifier.toIdentifier( joinTable.name() );
		final Identifier schemaName = StringHelper.isEmpty( joinTable.schema() )
				? bindingOptions.getDefaultSchemaName()
				: Identifier.toIdentifier( joinTable.schema() );
		final Identifier catalogName = StringHelper.isEmpty( joinTable.catalog() )
				? bindingOptions.getDefaultCatalogName()
				: Identifier.toIdentifier( joinTable.catalog() );

		final Table associationTable = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				schemaName == null ? null : schemaName.getCanonicalName(),
				catalogName == null ? null : catalogName.getCanonicalName(),
				logicalName.getCanonicalName(),
				null,
				false,
				bindingState.getMetadataBuildingContext(),
				false
		);

		final Join join = new Join();
		join.setTable( associationTable );
		join.setPersistentClass( ownerBinding );
		join.setOptional( true );
		join.setInverse( false );
		ownerBinding.addJoin( join );

		final IdentifierBinding ownerIdentifierBinding = bindingState.getIdentifierBinding( ownerType.getHierarchy().getRoot() );
		if ( ownerIdentifierBinding == null ) {
			throw new MappingException(
					"Could not resolve identifier binding for to-one association table owner - "
							+ ownerType.getClassDetails().getClassName()
			);
		}
		final List<JoinColumn> joinColumns = listJoinColumns( joinTable.joinColumns() );
		if ( !joinColumns.isEmpty() && joinColumns.size() != ownerIdentifierBinding.columns().size() ) {
			throw new MappingException(
					"Association table join column count did not match owner identifier column count - "
							+ ownerType.getClassDetails().getClassName()
			);
		}
		bindingState.addAssociationTableBinding( new AssociationTableBinding( join, joinColumns ) );
		return associationTable;
	}

	private static Table resolveAssociationTable(
			MemberDetails member,
			AssociationOverride associationOverride,
			Table primaryTable,
			BindingState bindingState) {
		final List<JoinColumn> joinColumns = resolveJoinColumns( member, associationOverride );
		final String tableName = resolveJoinTableName( joinColumns, primaryTable );
		if ( StringHelper.isEmpty( tableName ) ) {
			return primaryTable;
		}

		final Identifier identifier = Identifier.toIdentifier( tableName );
		final TableReference tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
		return tableByName.binding();
	}

	private static String resolveJoinTableName(List<JoinColumn> joinColumns, Table primaryTable) {
		String tableName = null;
		for ( JoinColumn joinColumn : joinColumns ) {
			final String joinColumnTableName = StringHelper.isEmpty( joinColumn.table() )
					? primaryTable.getName()
					: joinColumn.table();
			if ( tableName != null && !tableName.equals( joinColumnTableName ) ) {
				throw new MappingException( "To-one join columns cannot span multiple tables" );
			}
			tableName = joinColumnTableName;
		}
		return primaryTable.getName().equals( tableName ) ? null : tableName;
	}

	private static List<JoinColumn> resolveValueJoinColumns(
			MemberDetails member,
			AssociationOverride associationOverride,
			JoinTable joinTable) {
		if ( joinTable != null ) {
			return listJoinColumns( joinTable.inverseJoinColumns() );
		}
		return resolveJoinColumns( member, associationOverride );
	}

	static List<JoinColumn> resolveJoinColumns(
			MemberDetails member,
			AssociationOverride associationOverride) {
		if ( associationOverride != null && associationOverride.joinColumns().length > 0 ) {
			final ArrayList<JoinColumn> result = new ArrayList<>( associationOverride.joinColumns().length );
			for ( JoinColumn joinColumn : associationOverride.joinColumns() ) {
				result.add( joinColumn );
			}
			return result;
		}

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

	private static JoinTable resolveJoinTable(MemberDetails member, AssociationOverride associationOverride) {
		if ( associationOverride != null && isSpecified( associationOverride.joinTable() ) ) {
			return associationOverride.joinTable();
		}
		return member.getDirectAnnotationUsage( JoinTable.class );
	}

	private static boolean isSpecified(JoinTable joinTable) {
		return joinTable != null
				&& ( StringHelper.isNotEmpty( joinTable.name() )
						|| joinTable.joinColumns().length > 0
						|| joinTable.inverseJoinColumns().length > 0 );
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

	private static TargetEntityBinding resolveTargetEntityBinding(
			MemberDetails member,
			jakarta.persistence.ManyToOne manyToOneAnn,
			OneToOne oneToOneAnn,
			BindingState bindingState,
			BindingContext bindingContext) {
		final ClassDetails targetClassDetails = resolveTargetClassDetails( member, manyToOneAnn, oneToOneAnn, bindingContext );
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

	private static ClassDetails resolveTargetClassDetails(
			MemberDetails member,
			jakarta.persistence.ManyToOne manyToOneAnn,
			OneToOne oneToOneAnn,
			BindingContext bindingContext) {
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

	private static FetchType resolveFetchType(jakarta.persistence.ManyToOne manyToOneAnn, OneToOne oneToOneAnn) {
		if ( manyToOneAnn != null ) {
			return manyToOneAnn.fetch();
		}
		return oneToOneAnn.fetch();
	}

	private static boolean resolveOptionality(jakarta.persistence.ManyToOne manyToOneAnn, OneToOne oneToOneAnn) {
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
