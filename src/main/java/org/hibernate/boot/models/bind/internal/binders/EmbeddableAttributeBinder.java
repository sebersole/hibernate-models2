/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

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
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;

import java.util.Locale;

/**
 * Binds simple component-valued attributes.
 */
class EmbeddableAttributeBinder {
	private final IdentifiableTypeMetadata ownerType;
	private final PersistentClass ownerBinding;
	private final AttributeMetadata attributeMetadata;
	private final Table primaryTable;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	EmbeddableAttributeBinder(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			AttributeMetadata attributeMetadata,
			Table primaryTable,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this.ownerType = ownerType;
		this.ownerBinding = ownerBinding;
		this.attributeMetadata = attributeMetadata;
		this.primaryTable = primaryTable;
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;
	}

	Component bind(Property property) {
		final MemberDetails member = attributeMetadata.getMember();
		final Table componentTable = resolveComponentTable( member );
		final Component component = new Component(
				bindingState.getMetadataBuildingContext(),
				componentTable,
				ownerBinding
		);
		component.setEmbedded( true );
		component.setComponentClassName( member.getType().determineRawClass().getClassName() );
		component.setTable( componentTable );
		component.setTypeUsingReflection( ownerType.getClassDetails().getClassName(), attributeMetadata.getName() );

		new ComponentBinder( bindingState, bindingOptions, bindingContext ).bindBasicProperties(
				member.getType().determineRawClass(),
				component,
				componentTable,
				this::resolveColumnSource,
				(ignored, column) -> {
				},
				false,
				true,
				true
		);

		property.setOptional( true );
		return component;
	}

	private Table resolveComponentTable(MemberDetails attributeMember) {
		final Table[] result = { primaryTable };
		attributeMember.getType().determineRawClass().forEachPersistableMember( (member) -> {
			final ColumnSource columnSource = resolveColumnSource( member );
			if ( columnSource == null || StringHelper.isEmpty( columnSource.table() ) ) {
				return;
			}

			final Identifier identifier = Identifier.toIdentifier( columnSource.table() );
			final TableReference tableReference = bindingState.getTableByName( identifier.getCanonicalName() );
			final Table table = tableReference.binding();
			if ( result[0] != primaryTable && result[0] != table ) {
				throw new MappingException( String.format( Locale.ROOT,
						"Embeddable attributes cannot span multiple tables - %s.%s",
						attributeMember.getDeclaringType().getName(),
						attributeMetadata.getName()
				) );
			}
			result[0] = table;
		} );
		return result[0];
	}

	private ColumnSource resolveColumnSource(MemberDetails member) {
		final AttributeOverride override = resolveAttributeOverride( member.resolveAttributeName() );
		if ( override != null ) {
			return ColumnSource.from( override.column() );
		}

		final Column columnAnn = member.getDirectAnnotationUsage( Column.class );
		return ColumnSource.from( columnAnn );
	}

	private AttributeOverride resolveAttributeOverride(String memberName) {
		final AttributeOverride[] overrides = attributeMetadata.getMember().getRepeatedAnnotationUsages(
				AttributeOverride.class,
				bindingContext.getBootstrapContext().getModelsContext()
		);
		for ( AttributeOverride override : overrides ) {
			if ( memberName.equals( override.name() ) ) {
				return override;
			}
		}
		return null;
	}
}
