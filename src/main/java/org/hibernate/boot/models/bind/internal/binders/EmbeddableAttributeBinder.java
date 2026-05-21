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

import jakarta.persistence.Column;
import jakarta.persistence.Convert;

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
	private OverrideAndConverterCollector overrideAndConverterCollector;

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
		overrideAndConverterCollector = new OverrideAndConverterCollector( member, bindingContext );
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
				this::resolveConversion,
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
		visitColumnSources( attributeMember.getType().determineRawClass(), "", (path, member) -> {
			final ColumnSource columnSource = resolveColumnSource( path, member );
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

	private void visitColumnSources(
			org.hibernate.models.spi.ClassDetails componentType,
			String pathPrefix,
			java.util.function.BiConsumer<String, MemberDetails> consumer) {
		componentType.forEachPersistableMember( (member) -> {
			final String attributeName = member.resolveAttributeName();
			final String path = pathPrefix + attributeName;
			if ( member.hasDirectAnnotationUsage( jakarta.persistence.Embedded.class )
					|| member.getType().determineRawClass().hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class ) ) {
				visitColumnSources( member.getType().determineRawClass(), path + ".", consumer );
			}
			else {
				consumer.accept( path, member );
			}
		} );
	}

	private ColumnSource resolveColumnSource(String memberPath, MemberDetails member) {
		final var override = overrideAndConverterCollector.locateAttributeOverride( memberPath );
		if ( override != null ) {
			return ColumnSource.from( override.column() );
		}

		final Column columnAnn = member.getDirectAnnotationUsage( Column.class );
		return ColumnSource.from( columnAnn );
	}

	private Convert resolveConversion(String memberPath, MemberDetails member) {
		final Convert override = overrideAndConverterCollector.locateConversion( memberPath );
		if ( override != null ) {
			return override;
		}

		final Convert directConversion = member.getDirectAnnotationUsage( Convert.class );
		return directConversion != null && StringHelper.isEmpty( directConversion.attributeName() )
				? directConversion
				: null;
	}
}
