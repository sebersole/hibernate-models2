/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AggregatedKeyMapping;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.KeyMapping;
import org.hibernate.boot.models.categorize.spi.NonAggregatedKeyMapping;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Column;

/**
 * @author Steve Ebersole
 */
public class IdentifierBinder {
	private final ModelBinders modelBinders;

	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext context;

	public IdentifierBinder(
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		this.modelBinders = modelBinders;
		this.state = state;
		this.options = options;
		this.context = context;
	}

	public static IdentifierBinding bindIdentifier(
			EntityTypeMetadata type,
			RootClass typeBinding,
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		final IdentifierBinder identifierBinder = new IdentifierBinder( modelBinders, state, options, context );
		return identifierBinder.bindIdentifier( type, typeBinding );
	}

	private IdentifierBinding bindIdentifier(EntityTypeMetadata type, RootClass typeBinding) {
		final EntityHierarchy hierarchy = type.getHierarchy();
		final KeyMapping idMapping = hierarchy.getIdMapping();
		final Table table = typeBinding.getTable();

		final PrimaryKey primaryKey = new PrimaryKey( table );
		table.setPrimaryKey( primaryKey );

		if ( idMapping instanceof BasicKeyMapping basicKeyMapping ) {
			return bindBasicIdentifier( basicKeyMapping, table, type, typeBinding );
		}
		else if ( idMapping instanceof AggregatedKeyMapping aggregatedKeyMapping ) {
			return bindAggregatedIdentifier( aggregatedKeyMapping, table, type, typeBinding );
		}
		else {
			return bindNonAggregatedIdentifier( (NonAggregatedKeyMapping) idMapping, table, type, typeBinding );
		}
	}

	private IdentifierBinding bindBasicIdentifier(
			BasicKeyMapping basicKeyMapping,
			Table table,
			EntityTypeMetadata typeMetadata,
			RootClass typeBinding) {
		final AttributeMetadata idAttribute = basicKeyMapping.getAttribute();
		final MemberDetails idAttributeMember = idAttribute.getMember();

		final BasicValue idValue = createBasicIdValue( table, idAttributeMember );
		typeBinding.setIdentifier( idValue );

		final Property idProperty = createProperty( idAttribute.getName(), idValue );
		typeBinding.addProperty( idProperty );
		typeBinding.setIdentifierProperty( idProperty );

		final org.hibernate.mapping.Column column = bindIdColumn( idAttributeMember, () -> "id", idValue, table );

		return new IdentifierBinding(
				typeMetadata,
				typeBinding,
				basicKeyMapping,
				idValue,
				idProperty,
				table,
				List.of( column )
		);
	}

	private IdentifierBinding bindAggregatedIdentifier(
			AggregatedKeyMapping aggregatedKeyMapping,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final Component idValue = new Component( state.getMetadataBuildingContext(), typeBinding );
		idValue.setKey( true );
		idValue.setEmbedded( true );
		idValue.setComponentClassName( aggregatedKeyMapping.getKeyType().getClassName() );
		idValue.setTable( table );
		typeBinding.setIdentifier( idValue );
		typeBinding.setEmbeddedIdentifier( true );

		final Property idProperty = createProperty( aggregatedKeyMapping.getAttributeName(), idValue );
		typeBinding.setIdentifierProperty( idProperty );

		final List<org.hibernate.mapping.Column> columns = bindComponentIdentifierProperties(
				aggregatedKeyMapping.getKeyType(),
				idValue,
				table
		);

		return new IdentifierBinding(
				type,
				typeBinding,
				aggregatedKeyMapping,
				idValue,
				idProperty,
				table,
				columns
		);
	}

	private IdentifierBinding bindNonAggregatedIdentifier(
			NonAggregatedKeyMapping idMapping,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final Component idValue = new Component( state.getMetadataBuildingContext(), typeBinding );
		idValue.setKey( true );
		idValue.setEmbedded( false );
		idValue.setComponentClassName( idMapping.getIdClassType().getClassName() );
		idValue.setTable( table );
		typeBinding.setIdentifier( idValue );
		typeBinding.setEmbeddedIdentifier( false );

		final List<org.hibernate.mapping.Column> columns = new ArrayList<>( idMapping.getIdAttributes().size() );
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final MemberDetails member = idAttribute.getMember();
			final BasicValue basicValue = createBasicIdValue( table, member );
			final Property rootProperty = createProperty( idAttribute.getName(), basicValue );
			typeBinding.addProperty( rootProperty );

			final Property componentProperty = createProperty( idAttribute.getName(), basicValue );
			idValue.addProperty( componentProperty );

			final org.hibernate.mapping.Column column = bindIdColumn( member, idAttribute::getName, basicValue, table );
			columns.add( column );
		}

		return new IdentifierBinding(
				type,
				typeBinding,
				idMapping,
				idValue,
				null,
				table,
				columns
		);
	}

	private List<org.hibernate.mapping.Column> bindComponentIdentifierProperties(
			ClassDetails embeddableType,
			Component idValue,
			Table table) {
		final List<org.hibernate.mapping.Column> columns = new ArrayList<>();
		embeddableType.forEachPersistableMember( (member) -> {
			final BasicValue basicValue = createBasicIdValue( table, member );
			final Property property = createProperty( member.resolveAttributeName(), basicValue );
			idValue.addProperty( property );

			final org.hibernate.mapping.Column column = bindIdColumn(
					member,
					member::resolveAttributeName,
					basicValue,
					table
			);
			columns.add( column );
		} );
		return columns;
	}

	private BasicValue createBasicIdValue(Table table, MemberDetails member) {
		final BasicValue basicValue = new BasicValue( state.getMetadataBuildingContext(), table );
		basicValue.setTable( table );
		AttributeBinder.bindImplicitJavaType( member, null, basicValue, options, state, context );
		BasicValueBinder.bindJavaType( member, null, basicValue, options, state, context );
		BasicValueBinder.bindJdbcType( member, null, basicValue, options, state, context );
		BasicValueBinder.bindNationalized( member, null, basicValue, options, state, context );
		return basicValue;
	}

	private Property createProperty(String name, org.hibernate.mapping.Value value) {
		final Property property = new Property();
		property.setName( name );
		property.setValue( value );
		return property;
	}

	private org.hibernate.mapping.Column bindIdColumn(
			MemberDetails member,
			java.util.function.Supplier<String> implicitName,
			BasicValue basicValue,
			Table table) {
		final Column columnAnn = member.getDirectAnnotationUsage( Column.class );
		final org.hibernate.mapping.Column column = ColumnBinder.bindColumn(
				ColumnSource.from( columnAnn ),
				implicitName,
				true,
				false
		);
		basicValue.addColumn( column, true, false );
		table.getPrimaryKey().addColumn( column );
		return column;
	}
}
