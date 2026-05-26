/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize;

import org.hibernate.boot.models.AccessTypeDeterminationException;
import org.hibernate.boot.models.AccessTypeIndependenceException;
import org.hibernate.boot.models.AccessTypePlacementException;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.orm.bind.BindingTestingHelper;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for Jakarta Persistence 3.2 section 2.3, Access Type.
///
/// @author Steve Ebersole
public class AccessTypeTests {
	/// 2.3.1, default access type: mapping annotations on instance variables infer FIELD access
	/// for the entity hierarchy.
	@Test
	void fieldIdDeterminesFieldAccessForHierarchy() {
		final EntityHierarchy hierarchy = singleHierarchy( FieldDefaultEntity.class );
		final EntityTypeMetadata root = hierarchy.getRoot();

		assertThat( hierarchy.getDefaultAccessType() ).isEqualTo( AccessType.FIELD );
		assertThat( root.getAccessType() ).isEqualTo( AccessType.FIELD );
		assertThat( root.findAttribute( "id" ).getMember().isField() ).isTrue();
		assertThat( root.findAttribute( "name" ).getMember().isField() ).isTrue();
	}

	/// 2.3.1, default access type: mapping annotations on getter methods infer PROPERTY access
	/// for the entity hierarchy.
	@Test
	void getterIdDeterminesPropertyAccessForHierarchy() {
		final EntityHierarchy hierarchy = singleHierarchy( PropertyDefaultEntity.class );
		final EntityTypeMetadata root = hierarchy.getRoot();

		assertThat( hierarchy.getDefaultAccessType() ).isEqualTo( AccessType.PROPERTY );
		assertThat( root.getAccessType() ).isEqualTo( AccessType.PROPERTY );
		assertThat( root.findAttribute( "id" ).getMember().isField() ).isFalse();
		assertThat( root.findAttribute( "name" ).getMember().isField() ).isFalse();
	}

	/// 2.3.2, explicit access type: class-level @Access applies only to the annotated
	/// entity, mapped superclass, or embeddable; it does not affect other hierarchy types.
	@Test
	void explicitClassAccessOnlyAppliesToTheAnnotatedClass() {
		final EntityHierarchy hierarchy = singleHierarchy(
				ExplicitPropertyRoot.class,
				DefaultFieldSubclass.class
		);
		final IdentifiableTypeMetadata subType = singleSubType( hierarchy.getRoot() );

		assertThat( hierarchy.getDefaultAccessType() ).isEqualTo( AccessType.FIELD );
		assertThat( hierarchy.getRoot().getAccessType() ).isEqualTo( AccessType.PROPERTY );
		assertThat( subType.getAccessType() ).isEqualTo( AccessType.FIELD );
		assertThat( subType.findAttribute( "detail" ).getMember().isField() ).isTrue();
	}

	/// 2.3.2, explicit access type: a class using FIELD access may designate an
	/// individual getter for PROPERTY access.
	@Test
	void attributeLevelAccessCanOverrideExplicitFieldAccess() {
		final EntityTypeMetadata root = singleHierarchy( FieldAccessWithPropertyAttribute.class ).getRoot();

		assertThat( root.getAccessType() ).isEqualTo( AccessType.FIELD );
		assertThat( root.findAttribute( "id" ).getMember().isField() ).isTrue();
		assertThat( root.findAttribute( "name" ).getMember().isField() ).isFalse();
	}

	/// 2.3.2, explicit access type: a class using PROPERTY access may designate an
	/// individual field for FIELD access.
	@Test
	void attributeLevelAccessCanOverrideExplicitPropertyAccess() {
		final EntityTypeMetadata root = singleHierarchy( PropertyAccessWithFieldAttribute.class ).getRoot();

		assertThat( root.getAccessType() ).isEqualTo( AccessType.PROPERTY );
		assertThat( root.findAttribute( "id" ).getMember().isField() ).isFalse();
		assertThat( root.findAttribute( "name" ).getMember().isField() ).isTrue();
	}

	/// 2.3.1, default access type: defaulted classes in an entity hierarchy must use
	/// consistent annotation placement; mixed field/property placement is not supported.
	@Test
	void mixedDefaultAccessIndicatorsAreRejected() {
		assertThatThrownBy( () -> singleHierarchy( MixedFieldRoot.class, MixedPropertySubclass.class ) )
				.isInstanceOf( AccessTypeDeterminationException.class );
	}

	/// 2.3.2, explicit access type: @Access(PROPERTY) is misplaced on a field.
	@Test
	void accessPropertyMayNotBePlacedOnAField() {
		assertThatThrownBy( () -> singleHierarchy( PropertyAccessOnField.class ) )
				.isInstanceOf( AccessTypePlacementException.class );
	}

	/// 2.3.2, explicit access type: @Access(FIELD) is misplaced on a property getter.
	@Test
	void accessFieldMayNotBePlacedOnAGetter() {
		assertThatThrownBy( () -> singleHierarchy( FieldAccessOnGetter.class ) )
				.isInstanceOf( AccessTypePlacementException.class );
	}

	/// 2.3.2, explicit access type: @Access must not occur on a property setter.
	@Test
	void accessMayNotBePlacedOnASetter() {
		assertThatThrownBy( () -> singleHierarchy( AccessOnSetter.class ) )
				.isInstanceOf( AccessTypePlacementException.class );
	}

	/// 2.3.3, access type of an embeddable class: an embeddable without explicit
	/// `@Access` inherits FIELD access from the class/attribute in which it is embedded.
	@Test
	@ServiceRegistry
	void embeddableUsesFieldAccessInheritedFromContainingAttribute(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( FieldAccessEmbeddableOwner.class.getName() );
					final Component component = (Component) entityBinding.getProperty( "component" ).getValue();

					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "fieldValue" );
				},
				scope.getRegistry(),
				FieldAccessEmbeddableOwner.class,
				AccessSensitiveEmbeddable.class
		);
	}

	/// 2.3.3, access type of an embeddable class: an embeddable without explicit
	/// `@Access` inherits PROPERTY access from the class/attribute in which it is embedded.
	@Test
	@ServiceRegistry
	void embeddableUsesPropertyAccessInheritedFromContainingAttribute(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( PropertyAccessEmbeddableOwner.class.getName() );
					final Component component = (Component) entityBinding.getProperty( "component" ).getValue();

					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "property_column" );
				},
				scope.getRegistry(),
				PropertyAccessEmbeddableOwner.class,
				AccessSensitiveEmbeddable.class
		);
	}

	/// 2.3.3, access type of an embeddable class: explicit `@Access` on the embeddable
	/// overrides the access type inherited from the containing class/attribute.
	@Test
	@ServiceRegistry
	void explicitEmbeddableAccessOverridesContainingAttributeAccess(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( FieldAccessExplicitEmbeddableOwner.class.getName() );
					final Component component = (Component) entityBinding.getProperty( "component" ).getValue();

					assertThat( component.getColumns() )
							.extracting( org.hibernate.mapping.Column::getName )
							.containsExactly( "explicit_property_column" );
				},
				scope.getRegistry(),
				FieldAccessExplicitEmbeddableOwner.class,
				ExplicitPropertyEmbeddable.class
		);
	}

	// 2.3.4, defaulted access types of embeddable classes and mapped superclasses:
	// a defaulted embeddable used in both field and property contexts is portable
	// when the persistent attribute number, names, and types are independent of access type.
	@Test
	@ServiceRegistry
	void defaultedEmbeddableCanBeUsedInDifferingAccessContextsWhenShapeIsIndependent(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final PersistentClass fieldAccessEntity = context.getMetadataCollector()
							.getEntityBinding( FieldAccessIndependentEmbeddableOwner.class.getName() );
					final Component fieldAccessComponent = (Component) fieldAccessEntity.getProperty( "component" ).getValue();
					assertThat( fieldAccessComponent.getProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.containsExactly( "value" );

					final PersistentClass propertyAccessEntity = context.getMetadataCollector()
							.getEntityBinding( PropertyAccessIndependentEmbeddableOwner.class.getName() );
					final Component propertyAccessComponent = (Component) propertyAccessEntity.getProperty( "component" ).getValue();
					assertThat( propertyAccessComponent.getProperties() )
							.extracting( org.hibernate.mapping.Property::getName )
							.containsExactly( "value" );
				},
				scope.getRegistry(),
				FieldAccessIndependentEmbeddableOwner.class,
				PropertyAccessIndependentEmbeddableOwner.class,
				AccessIndependentEmbeddable.class
		);
	}

	// 2.3.4, defaulted access types of embeddable classes and mapped superclasses:
	// a defaulted mapped superclass used in both field and property hierarchy contexts is
	// portable when the persistent attribute number, names, and types are access-independent.
	@Test
	void defaultedMappedSuperclassCanBeUsedInDifferingAccessContextsWhenShapeIsIndependent() {
		final CategorizedDomainModel domainModel = BindingTestingHelper.buildCategorizedDomainModel(
				FieldAccessMappedSuperclassEntity.class,
				PropertyAccessMappedSuperclassEntity.class,
				AccessIndependentMappedSuperclass.class
		);

		assertThat( domainModel.getEntityHierarchies() ).hasSize( 2 );
		for ( EntityHierarchy hierarchy : domainModel.getEntityHierarchies() ) {
			final IdentifiableTypeMetadata mappedSuperclass = hierarchy.getRoot().getSuperType();

			assertThat( mappedSuperclass ).isNotNull();
			assertThat( mappedSuperclass.getClassDetails().getClassName() )
					.isEqualTo( AccessIndependentMappedSuperclass.class.getName() );
			assertThat( mappedSuperclass.findAttribute( "shared" ) ).isNotNull();

			if ( hierarchy.getRoot().getClassDetails().getClassName()
					.equals( FieldAccessMappedSuperclassEntity.class.getName() ) ) {
				assertThat( hierarchy.getDefaultAccessType() ).isEqualTo( AccessType.FIELD );
				assertThat( mappedSuperclass.getAccessType() ).isEqualTo( AccessType.FIELD );
				assertThat( mappedSuperclass.findAttribute( "shared" ).getMember().isField() ).isTrue();
			}
			else {
				assertThat( hierarchy.getDefaultAccessType() ).isEqualTo( AccessType.PROPERTY );
				assertThat( mappedSuperclass.getAccessType() ).isEqualTo( AccessType.PROPERTY );
				assertThat( mappedSuperclass.findAttribute( "shared" ).getMember().isField() ).isFalse();
			}
		}
	}

	// 2.3.4, defaulted access types of embeddable classes and mapped superclasses:
	// if the same defaulted embeddable has different persistent attribute names under
	// FIELD and PROPERTY access, the undefined spec case is treated as a mapping error.
	@Test
	@ServiceRegistry
	void defaultedEmbeddableWithAccessDependentShapeIsRejected(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> BindingTestingHelper.checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				FieldAccessDependentEmbeddableOwner.class,
				PropertyAccessDependentEmbeddableOwner.class,
				AccessDependentEmbeddable.class
		) )
				.isInstanceOf( AccessTypeIndependenceException.class )
				.hasMessageContaining( AccessDependentEmbeddable.class.getName() );
	}

	// 2.3.4, defaulted access types of embeddable classes and mapped superclasses:
	// if the same defaulted mapped superclass has different persistent attribute names under
	// FIELD and PROPERTY access, the undefined spec case is treated as a mapping error.
	@Test
	void defaultedMappedSuperclassWithAccessDependentShapeIsRejected() {
		assertThatThrownBy( () -> BindingTestingHelper.buildCategorizedDomainModel(
				FieldAccessDependentMappedSuperclassEntity.class,
				PropertyAccessDependentMappedSuperclassEntity.class,
				AccessDependentMappedSuperclass.class
		) )
				.isInstanceOf( AccessTypeIndependenceException.class )
				.hasMessageContaining( AccessDependentMappedSuperclass.class.getName() );
	}

	private static EntityHierarchy singleHierarchy(Class<?>... classes) {
		final CategorizedDomainModel domainModel = BindingTestingHelper.buildCategorizedDomainModel( classes );
		assertThat( domainModel.getEntityHierarchies() ).hasSize( 1 );
		return domainModel.getEntityHierarchies().iterator().next();
	}

	private static IdentifiableTypeMetadata singleSubType(EntityTypeMetadata root) {
		assertThat( root.getSubTypes() ).hasSize( 1 );
		return root.getSubTypes().iterator().next();
	}

	@Entity
	public static class FieldDefaultEntity {
		@Id
		private Long id;
		private String name;
	}

	@Entity
	public static class PropertyDefaultEntity {
		private Long id;
		private String name;

		@Id
		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity
	@Access(AccessType.PROPERTY)
	public static class ExplicitPropertyRoot {
		private Long id;

		@Id
		public Long getId() {
			return id;
		}
	}

	@Entity
	public static class DefaultFieldSubclass extends ExplicitPropertyRoot {
		@Basic
		private String detail;
	}

	@Entity
	@Access(AccessType.FIELD)
	public static class FieldAccessWithPropertyAttribute {
		@Id
		private Long id;
		private String name;

		@Access(AccessType.PROPERTY)
		@Basic
		public String getName() {
			return name;
		}
	}

	@Entity
	@Access(AccessType.PROPERTY)
	public static class PropertyAccessWithFieldAttribute {
		private Long id;

		@Access(AccessType.FIELD)
		@Basic
		private String name;

		@Id
		public Long getId() {
			return id;
		}

		@Transient
		public String getName() {
			return name;
		}
	}

	@Entity
	public static class MixedFieldRoot {
		@Id
		private Long id;
	}

	@Entity
	public static class MixedPropertySubclass extends MixedFieldRoot {
		private String detail;

		@Basic
		public String getDetail() {
			return detail;
		}
	}

	@Entity
	public static class PropertyAccessOnField {
		@Id
		private Long id;

		@Access(AccessType.PROPERTY)
		private String name;
	}

	@Entity
	public static class FieldAccessOnGetter {
		@Id
		private Long id;
		private String name;

		@Access(AccessType.FIELD)
		public String getName() {
			return name;
		}
	}

	@Entity
	public static class AccessOnSetter {
		@Id
		private Long id;
		private String name;

		public String getName() {
			return name;
		}

		@Access(AccessType.PROPERTY)
		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	public static class FieldAccessEmbeddableOwner {
		@Id
		private Long id;

		@Embedded
		private AccessSensitiveEmbeddable component;
	}

	@Entity
	public static class PropertyAccessEmbeddableOwner {
		private Long id;
		private AccessSensitiveEmbeddable component;

		@Id
		public Long getId() {
			return id;
		}

		@Embedded
		public AccessSensitiveEmbeddable getComponent() {
			return component;
		}
	}

	@Embeddable
	public static class AccessSensitiveEmbeddable {
		private String fieldValue;

		@Transient
		private String propertyValue;

		@Transient
		public String getFieldValue() {
			return fieldValue;
		}

		@Basic
		@Column(name = "property_column")
		public String getPropertyValue() {
			return propertyValue;
		}
	}

	@Entity
	public static class FieldAccessExplicitEmbeddableOwner {
		@Id
		private Long id;

		@Embedded
		private ExplicitPropertyEmbeddable component;
	}

	@Embeddable
	@Access(AccessType.PROPERTY)
	public static class ExplicitPropertyEmbeddable {
		private String fieldValue;

		@Transient
		private String propertyValue;

		@Transient
		public String getFieldValue() {
			return fieldValue;
		}

		@Basic
		@Column(name = "explicit_property_column")
		public String getPropertyValue() {
			return propertyValue;
		}
	}

	@Entity
	public static class FieldAccessIndependentEmbeddableOwner {
		@Id
		private Long id;

		@Embedded
		private AccessIndependentEmbeddable component;
	}

	@Entity
	public static class PropertyAccessIndependentEmbeddableOwner {
		private Long id;
		private AccessIndependentEmbeddable component;

		@Id
		public Long getId() {
			return id;
		}

		@Embedded
		public AccessIndependentEmbeddable getComponent() {
			return component;
		}
	}

	@Embeddable
	public static class AccessIndependentEmbeddable {
		private String value;

		public String getValue() {
			return value;
		}
	}

	@MappedSuperclass
	public static class AccessIndependentMappedSuperclass {
		private String shared;

		public String getShared() {
			return shared;
		}
	}

	@Entity
	public static class FieldAccessMappedSuperclassEntity extends AccessIndependentMappedSuperclass {
		@Id
		private Long id;
	}

	@Entity
	public static class PropertyAccessMappedSuperclassEntity extends AccessIndependentMappedSuperclass {
		private Long id;

		@Id
		public Long getId() {
			return id;
		}
	}

	@Entity
	public static class FieldAccessDependentEmbeddableOwner {
		@Id
		private Long id;

		@Embedded
		private AccessDependentEmbeddable component;
	}

	@Entity
	public static class PropertyAccessDependentEmbeddableOwner {
		private Long id;
		private AccessDependentEmbeddable component;

		@Id
		public Long getId() {
			return id;
		}

		@Embedded
		public AccessDependentEmbeddable getComponent() {
			return component;
		}
	}

	@Embeddable
	public static class AccessDependentEmbeddable {
		private String fieldValue;
		private String propertyValue;

		@Transient
		public String getFieldValue() {
			return fieldValue;
		}

		public String getPropertyValue() {
			return propertyValue;
		}
	}

	@MappedSuperclass
	public static class AccessDependentMappedSuperclass {
		private String fieldValue;
		private String propertyValue;

		@Transient
		public String getFieldValue() {
			return fieldValue;
		}

		public String getPropertyValue() {
			return propertyValue;
		}
	}

	@Entity
	public static class FieldAccessDependentMappedSuperclassEntity extends AccessDependentMappedSuperclass {
		@Id
		private Long id;
	}

	@Entity
	public static class PropertyAccessDependentMappedSuperclassEntity extends AccessDependentMappedSuperclass {
		private Long id;

		@Id
		public Long getId() {
			return id;
		}
	}
}
