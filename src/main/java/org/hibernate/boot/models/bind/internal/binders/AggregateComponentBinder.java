/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;

/// Detects aggregate components and records them for later finalization.
///
/// This is the local counterpart to upstream `AggregateComponentBinder`, but it
/// records typed pending work in [BindingState] instead of registering
/// `AggregateComponentSecondPass`.  The follow-up finalization code is still
/// intentionally close to upstream and should be folded into
/// `AggregateComponentFinalizer` as this prototype grows.
class AggregateComponentBinder {
	static void processAggregate(
			PersistentClass ownerBinding,
			Component component,
			ClassDetails componentClassDetails,
			MemberDetails aggregateMember,
			String propertyPath,
			Table table,
			BindingState bindingState,
			BindingOptions bindingOptions) {
		if ( !isAggregate( aggregateMember, componentClassDetails ) ) {
			return;
		}

		final QualifiedName structName = determineStructName( aggregateMember, componentClassDetails, bindingState );
		final String renderedStructName = structName == null ? null : structName.render();
		bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.resolveDescriptor(
						componentClassDetails.toJavaClass(),
						() -> new EmbeddableAggregateJavaType<>( componentClassDetails.toJavaClass(), renderedStructName )
				);

		component.setStructName( structName );
		component.setStructColumnNames( determineStructAttributeNames( aggregateMember, componentClassDetails ) );

		final Column column = ColumnBinder.bindColumn(
				ColumnSource.from( aggregateMember.getDirectAnnotationUsage( jakarta.persistence.Column.class ) ),
				() -> aggregateMember.resolveAttributeName()
		);
		applyAggregateSqlType( column, aggregateMember, structName, bindingState );

		final BasicValue aggregateValue = new BasicValue( bindingState.getMetadataBuildingContext(), table );
		aggregateValue.setTable( table );
		aggregateValue.addColumn( column );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.attribute( aggregateMember ),
				null,
				aggregateValue,
				bindingOptions,
				bindingState,
				null
		);

		final AggregateColumn aggregateColumn = new AggregateColumn( column, component );
		table.addColumn( aggregateColumn );
		component.setAggregateColumn( aggregateColumn );

		bindingState.addAggregateComponentBinding( new AggregateComponentBinding(
				ownerBinding,
				component,
				componentClassDetails,
				propertyPath,
				table
		) );
	}

	private static void applyAggregateSqlType(
			Column column,
			MemberDetails aggregateMember,
			QualifiedName structName,
			BindingState bindingState) {
		final JdbcTypeCode jdbcTypeCode = aggregateMember.getDirectAnnotationUsage( JdbcTypeCode.class );
		if ( jdbcTypeCode != null ) {
			column.setSqlTypeCode( jdbcTypeCode.value() );
			return;
		}

		if ( structName != null ) {
			column.setSqlTypeCode( SqlTypes.STRUCT );
			column.setSqlType( structName.render() );
		}
	}

	private static QualifiedName determineStructName(
			MemberDetails aggregateMember,
			ClassDetails componentClassDetails,
			BindingState bindingState) {
		final Struct memberStruct = aggregateMember.getDirectAnnotationUsage( Struct.class );
		if ( memberStruct != null ) {
			return toQualifiedName( memberStruct, bindingState );
		}

		final Struct typeStruct = componentClassDetails.getDirectAnnotationUsage( Struct.class );
		return typeStruct == null ? null : toQualifiedName( typeStruct, bindingState );
	}

	private static QualifiedName toQualifiedName(Struct struct, BindingState bindingState) {
		final var database = bindingState.getDatabase();
		return new QualifiedNameImpl(
				database.toIdentifier( struct.catalog() ),
				database.toIdentifier( struct.schema() ),
				database.toIdentifier( struct.name() )
		);
	}

	private static String[] determineStructAttributeNames(MemberDetails aggregateMember, ClassDetails componentClassDetails) {
		final Struct memberStruct = aggregateMember.getDirectAnnotationUsage( Struct.class );
		if ( memberStruct != null ) {
			return memberStruct.attributes();
		}

		final Struct typeStruct = componentClassDetails.getDirectAnnotationUsage( Struct.class );
		return typeStruct == null ? null : typeStruct.attributes();
	}

	private static boolean isAggregate(MemberDetails aggregateMember, ClassDetails componentClassDetails) {
		if ( aggregateMember.hasDirectAnnotationUsage( Struct.class ) ) {
			return true;
		}

		final JdbcTypeCode jdbcTypeCode = aggregateMember.getDirectAnnotationUsage( JdbcTypeCode.class );
		if ( jdbcTypeCode != null ) {
			return switch ( jdbcTypeCode.value() ) {
				case SqlTypes.STRUCT,
						SqlTypes.JSON,
						SqlTypes.SQLXML,
						SqlTypes.STRUCT_ARRAY,
						SqlTypes.STRUCT_TABLE,
						SqlTypes.JSON_ARRAY,
						SqlTypes.XML_ARRAY -> true;
				default -> false;
			};
		}

		return componentClassDetails.hasDirectAnnotationUsage( Struct.class );
	}

	private AggregateComponentBinder() {
	}
}
