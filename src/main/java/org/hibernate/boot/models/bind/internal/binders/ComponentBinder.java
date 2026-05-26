/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.boot.models.AccessTypePlacementException;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.models.bind.internal.sources.BasicValueSource;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.internal.sources.ComponentSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.internal.AttributeMetadataImpl;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Convert;
import jakarta.persistence.Transient;

/// Shared support for binding component-valued mappings.
///
/// Components appear in several source roles: embedded attributes, embedded ids,
/// nested embeddables, and embeddable collection elements.  This binder walks the
/// component type's persistent members and applies path-aware column overrides,
/// association overrides, and converter overrides supplied by [ComponentSource].
///
/// Nested to-one associations are delegated back to [ToOneAttributeBinder] so
/// they participate in the same target-resolution, derived-identifier, and
/// foreign-key phases as top-level associations.
///
/// @author Steve Ebersole
class ComponentBinder {
	private final ModelBinders modelBinders;
	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext context;

	ComponentBinder(
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		this.modelBinders = modelBinders;
		this.state = state;
		this.options = options;
		this.context = context;
	}

	List<Column> bindBasicProperties(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ComponentSource source,
			Component component,
			Table table,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		return bindProperties(
				ownerType,
				ownerBinding,
				source.componentType(),
				component,
				table,
				"",
				resolveComponentMembers(
						source.componentType(),
						determineComponentAccessType( source.componentType(), ownerType.getAccessType() )
				),
				source::columnSource,
				source::conversion,
				(path, member) -> source.associationOverride( path ),
				columnConsumer,
				uniqueByDefault,
				nullableByDefault,
				updatable
		);
	}

	List<Column> bindBasicProperties(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ClassDetails componentType,
			Component component,
			Table table,
			BiFunction<String, MemberDetails, ColumnSource> columnSourceResolver,
			BiFunction<String, MemberDetails, Convert> conversionResolver,
			BiFunction<String, MemberDetails, AssociationOverride> associationOverrideResolver,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		return bindProperties(
				ownerType,
				ownerBinding,
				componentType,
				component,
				table,
				"",
				resolveComponentMembers( componentType, determineComponentAccessType( componentType, ownerType.getAccessType() ) ),
				columnSourceResolver,
				conversionResolver,
				associationOverrideResolver,
				columnConsumer,
				uniqueByDefault,
				nullableByDefault,
				updatable
		);
	}

	private List<Column> bindProperties(
			IdentifiableTypeMetadata ownerType,
			PersistentClass ownerBinding,
			ClassDetails componentType,
			Component component,
			Table table,
			String pathPrefix,
			List<MemberDetails> members,
			BiFunction<String, MemberDetails, ColumnSource> columnSourceResolver,
			BiFunction<String, MemberDetails, Convert> conversionResolver,
			BiFunction<String, MemberDetails, AssociationOverride> associationOverrideResolver,
			BiConsumer<MemberDetails, Column> columnConsumer,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		final List<Column> columns = new ArrayList<>();
		for ( MemberDetails member : members ) {
			validateMember( member );
			final String attributeName = member.resolveAttributeName();
			final String memberPath = pathPrefix + attributeName;

			if ( isToOneMember( member ) ) {
				final Property property = new Property();
				property.setName( attributeName );
				final var manyToOne = ToOneAttributeBinder.bindToOne(
						ownerType,
						ownerBinding,
						componentType.getClassName(),
						attributeName,
						member,
						property,
						table,
						associationOverrideResolver.apply( memberPath, member ),
						modelBinders,
						options,
						state,
						context
				);
				property.setValue( manyToOne );
				component.addProperty( property );
				columns.addAll( manyToOne.getColumns() );
				continue;
			}

			if ( isEmbeddedMember( member ) ) {
				final Component nestedComponent = new Component( state.getMetadataBuildingContext(), component );
				nestedComponent.setEmbedded( true );
				nestedComponent.setComponentClassName( member.getType().determineRawClass().getClassName() );
				nestedComponent.setTable( table );
				nestedComponent.setTypeUsingReflection( componentType.getClassName(), attributeName );

				final Property property = createProperty( attributeName, nestedComponent );
				component.addProperty( property );
				final ClassDetails nestedComponentType = member.getType().determineRawClass();
				columns.addAll( bindProperties(
							ownerType,
							ownerBinding,
							nestedComponentType,
							nestedComponent,
							table,
							memberPath + ".",
							resolveComponentMembers(
									nestedComponentType,
									determineComponentAccessType(
											nestedComponentType,
											resolveComponentAccessType( members )
									)
							),
						columnSourceResolver,
						conversionResolver,
						associationOverrideResolver,
						columnConsumer,
						uniqueByDefault,
						nullableByDefault,
						updatable
				) );
				AggregateComponentBinder.processAggregate(
						ownerBinding,
						nestedComponent,
						nestedComponentType,
						member,
						memberPath,
						table,
						state,
						options
				);
				continue;
			}

			final BasicValue basicValue = createBasicValue(
					table,
					member,
					conversionResolver.apply( memberPath, member )
			);
			final Property property = createProperty( attributeName, basicValue );
			component.addProperty( property );

			final Column column = bindColumn(
					() -> attributeName,
					basicValue,
					columnSourceResolver.apply( memberPath, member ),
					uniqueByDefault,
					nullableByDefault,
					updatable
			);
			columnConsumer.accept( member, column );
			columns.add( column );
		}
		return columns;
	}

	private AccessType resolveComponentAccessType(List<MemberDetails> members) {
		if ( members.isEmpty() ) {
			return AccessType.FIELD;
		}
		return members.get( 0 ).isField() ? AccessType.FIELD : AccessType.PROPERTY;
	}

	private AccessType determineComponentAccessType(ClassDetails componentType, AccessType containingAccessType) {
		final Access access = componentType.getDirectAnnotationUsage( Access.class );
		return access == null ? containingAccessType : access.value();
	}

	private List<MemberDetails> resolveComponentMembers(ClassDetails componentType, AccessType accessType) {
		final LinkedHashMap<String, MemberDetails> results = new LinkedHashMap<>();

		for ( FieldDetails field : componentType.getFields() ) {
			final Access access = field.getDirectAnnotationUsage( Access.class );
			if ( access == null ) {
				continue;
			}
			validateAttributeLevelAccess( componentType, field, access.value() );
			if ( !isTransient( field ) ) {
				results.put( field.resolveAttributeName(), field );
			}
		}

		for ( MethodDetails method : componentType.getMethods() ) {
			final Access access = method.getDirectAnnotationUsage( Access.class );
			if ( access == null ) {
				continue;
			}
			validateAttributeLevelAccess( componentType, method, access.value() );
			if ( !isTransient( method ) ) {
				results.put( method.resolveAttributeName(), method );
			}
		}

		if ( accessType == AccessType.FIELD ) {
			for ( FieldDetails field : componentType.getFields() ) {
				if ( field.isPersistable()
						&& !isTransient( field )
						&& !results.containsKey( field.resolveAttributeName() ) ) {
					results.put( field.resolveAttributeName(), field );
				}
			}
		}
		else {
			for ( MethodDetails method : componentType.getMethods() ) {
				if ( method.isPersistable()
						&& !isTransient( method )
						&& !results.containsKey( method.resolveAttributeName() ) ) {
					results.put( method.resolveAttributeName(), method );
				}
			}
		}

		final List<MemberDetails> members = new ArrayList<>( results.values() );
		validateAccessTypeIndependence( componentType, accessType, members );
		return members;
	}

	private void validateAccessTypeIndependence(
			ClassDetails componentType,
			AccessType accessType,
			List<MemberDetails> members) {
		final List<AttributeMetadata> attributes = new ArrayList<>( members.size() );
		for ( MemberDetails member : members ) {
			attributes.add( new AttributeMetadataImpl(
					member.resolveAttributeName(),
					AttributeNature.BASIC,
					member
			) );
		}
		modelBinders.getEmbeddableAccessTypeIndependenceValidator().validate( componentType, accessType, attributes );
	}

	private boolean isTransient(MemberDetails member) {
		return member.hasDirectAnnotationUsage( Transient.class );
	}

	private void validateAttributeLevelAccess(
			ClassDetails componentType,
			MemberDetails member,
			AccessType attributeAccessType) {
		if ( ( attributeAccessType == AccessType.FIELD && !member.isField() )
				|| ( attributeAccessType == AccessType.PROPERTY && member.isField() )
				|| ( attributeAccessType == AccessType.PROPERTY
						&& member.asMethodDetails().getMethodKind() != MethodDetails.MethodKind.GETTER ) ) {
			throw new AccessTypePlacementException( componentType, member );
		}
	}

	private void validateMember(MemberDetails member) {
		if ( member.isPlural()
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ManyToMany.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.ElementCollection.class ) ) {
			throw new UnsupportedOperationException(
					"Only basic embeddable members are supported for now - " + member.getName()
			);
		}
	}

	private boolean isEmbeddedMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.Embedded.class )
				|| member.getType().determineRawClass().hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class );
	}

	private boolean isToOneMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class );
	}

	private BasicValue createBasicValue(Table table, MemberDetails member, Convert conversion) {
		final BasicValue basicValue = new BasicValue( state.getMetadataBuildingContext(), table );
		basicValue.setTable( table );
		BasicValueBinder.bindBasicValue(
				BasicValueSource.embeddableMember( member, conversion ),
				null,
				basicValue,
				options,
				state,
				context
		);
		return basicValue;
	}

	private Property createProperty(String name, org.hibernate.mapping.Value value) {
		final Property property = new Property();
		property.setName( name );
		property.setValue( value );
		return property;
	}

	private Column bindColumn(
			java.util.function.Supplier<String> implicitName,
			BasicValue basicValue,
			ColumnSource columnSource,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			boolean updatable) {
		final Column column = ColumnBinder.bindColumn(
				columnSource,
				implicitName,
				uniqueByDefault,
				nullableByDefault
		);
		basicValue.addColumn( column, true, updatable );
		return column;
	}
}
