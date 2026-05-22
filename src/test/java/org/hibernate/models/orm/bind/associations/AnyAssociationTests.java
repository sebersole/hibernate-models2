/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.associations;

import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.AnyKeyJdbcTypeCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.mapping.DiscriminatorValue;
import org.hibernate.models.orm.bind.BindingTestingHelper;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Tests for singular Hibernate `@Any` association binding.
///
/// @author Steve Ebersole
public class AnyAssociationTests {
	@Test
	@ServiceRegistry
	void testExplicitAnyAssociation(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( Holder.class.getName() );
					final Property property = entityBinding.getProperty( "target" );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) property.getValue();

					assertThat( value.isLazy() ).isFalse();
					assertThat( property.isOptional() ).isFalse();

					final BasicValue discriminator = value.getDiscriminatorDescriptor();
					assertThat( ( (Column) discriminator.getColumn() ).getName() ).isEqualTo( "target_type" );
					assertThat( discriminator.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );

					final BasicValue key = value.getKeyDescriptor();
					assertThat( ( (Column) key.getColumn() ).getName() ).isEqualTo( "target_id" );
					assertThat( key.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );

					assertThat( value.getMetaValues() )
							.containsEntry( DiscriminatorValue.of( 1 ), TargetOne.class.getName() )
							.containsEntry( DiscriminatorValue.of( 2 ), TargetTwo.class.getName() );
				},
				scope.getRegistry(),
				Holder.class,
				TargetOne.class,
				TargetTwo.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyRequiresExplicitKeyJavaClass(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> BindingTestingHelper.checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				MissingKeyTypeHolder.class,
				TargetOne.class
		) ).isInstanceOf( UnsupportedOperationException.class )
				.hasMessageContaining( "@Any requires @AnyKeyJavaClass" );
	}

	@Test
	@ServiceRegistry
	void testAnyDiscriminatorCharType(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( CharDiscriminatorHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();

					final BasicValue discriminator = value.getDiscriminatorDescriptor();
					assertThat( discriminator.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Character.class );
					assertThat( value.getMetaValues() )
							.containsEntry( DiscriminatorValue.of( 'A' ), TargetOne.class.getName() );
				},
				scope.getRegistry(),
				CharDiscriminatorHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyImplicitDiscriminatorValues(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ImplicitDiscriminatorHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();

					assertThat( value.getMetaValues() ).isEmpty();
					assertThat( value.getDiscriminatorDescriptor().resolve().getDomainJavaType().getJavaType() )
							.isEqualTo( String.class );
				},
				scope.getRegistry(),
				ImplicitDiscriminatorHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyDiscriminatorJdbcTypeCode(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( JdbcTypeCodeDiscriminatorHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();
					final BasicValue discriminator = value.getDiscriminatorDescriptor();

					assertThat( discriminator.resolve().getJdbcType().getJdbcTypeCode() )
							.isEqualTo( SqlTypes.CHAR );
				},
				scope.getRegistry(),
				JdbcTypeCodeDiscriminatorHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyKeyJdbcTypeCode(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( JdbcTypeCodeKeyHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();
					final BasicValue key = value.getKeyDescriptor();

					assertThat( key.resolve().getJdbcType().getJdbcTypeCode() )
							.isEqualTo( SqlTypes.BIGINT );
				},
				scope.getRegistry(),
				JdbcTypeCodeKeyHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testAnyKeyJavaType(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( JavaTypeKeyHolder.class.getName() );
					final org.hibernate.mapping.Any value = (org.hibernate.mapping.Any) entityBinding.getProperty( "target" )
							.getValue();
					final BasicValue key = value.getKeyDescriptor();

					assertThat( key.resolve().getDomainJavaType() )
							.isInstanceOf( AnyIntegerJavaType.class );
				},
				scope.getRegistry(),
				JavaTypeKeyHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToAnyAssociation(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ManyHolder.class.getName() );
					final Property property = entityBinding.getProperty( "targets" );
					final Collection collection = (Collection) property.getValue();
					final org.hibernate.mapping.Any element = (org.hibernate.mapping.Any) collection.getElement();

					assertThat( collection.getCollectionTable().getName() ).isEqualTo( "many_holder_targets" );
					assertThat( collection.getKey().getColumns() ).extracting( Column::getName )
							.containsExactly( "holder_id" );

					final BasicValue discriminator = element.getDiscriminatorDescriptor();
					assertThat( ( (Column) discriminator.getColumn() ).getName() ).isEqualTo( "target_type" );
					assertThat( discriminator.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );

					final BasicValue key = element.getKeyDescriptor();
					assertThat( ( (Column) key.getColumn() ).getName() ).isEqualTo( "target_id" );
					assertThat( key.resolve().getDomainJavaType().getJavaType() ).isEqualTo( Integer.class );

					assertThat( element.getMetaValues() )
							.containsEntry( DiscriminatorValue.of( 1 ), TargetOne.class.getName() )
							.containsEntry( DiscriminatorValue.of( 2 ), TargetTwo.class.getName() );
				},
				scope.getRegistry(),
				ManyHolder.class,
				TargetOne.class,
				TargetTwo.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToAnyKeyJdbcTypeCode(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( JdbcTypeCodeKeyManyHolder.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "targets" ).getValue();
					final org.hibernate.mapping.Any element = (org.hibernate.mapping.Any) collection.getElement();
					final BasicValue key = element.getKeyDescriptor();

					assertThat( key.resolve().getJdbcType().getJdbcTypeCode() )
							.isEqualTo( SqlTypes.BIGINT );
				},
				scope.getRegistry(),
				JdbcTypeCodeKeyManyHolder.class,
				TargetOne.class
		);
	}

	@Test
	@ServiceRegistry
	void testManyToAnyImplicitJoinTable(ServiceRegistryScope scope) {
		BindingTestingHelper.checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( ImplicitJoinTableManyHolder.class.getName() );
					final Collection collection = (Collection) entityBinding.getProperty( "targets" ).getValue();
					final org.hibernate.mapping.Any element = (org.hibernate.mapping.Any) collection.getElement();

					assertThat( collection.getCollectionTable() ).isNotSameAs( entityBinding.getTable() );
					assertThat( collection.getKey().getColumns() ).extracting( Column::getName )
							.containsExactly( "id" );
					assertThat( ( (Column) element.getDiscriminatorDescriptor().getColumn() ).getName() )
							.isEqualTo( "targets_type" );
					assertThat( ( (Column) element.getKeyDescriptor().getColumn() ).getName() )
							.isEqualTo( "targets_id" );
				},
				scope.getRegistry(),
				ImplicitJoinTableManyHolder.class,
				TargetOne.class
		);
	}

	@Entity(name = "AnyHolder")
	public static class Holder {
		@Id
		private Integer id;

		@Any(optional = false)
		@AnyDiscriminator(DiscriminatorType.INTEGER)
		@AnyDiscriminatorValue(discriminator = "1", entity = TargetOne.class)
		@AnyDiscriminatorValue(discriminator = "2", entity = TargetTwo.class)
		@AnyKeyJavaClass(Integer.class)
		@jakarta.persistence.Column(name = "target_type")
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "MissingAnyKeyTypeHolder")
	public static class MissingKeyTypeHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		private Object target;
	}

	@Entity(name = "CharAnyDiscriminatorHolder")
	public static class CharDiscriminatorHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminator(DiscriminatorType.CHAR)
		@AnyDiscriminatorValue(discriminator = "A", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "ImplicitAnyDiscriminatorHolder")
	public static class ImplicitDiscriminatorHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminatorImplicitValues(AnyDiscriminatorImplicitValues.Strategy.SHORT_NAME)
		@AnyKeyJavaClass(Integer.class)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "JdbcTypeCodeAnyDiscriminatorHolder")
	public static class JdbcTypeCodeDiscriminatorHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminator(DiscriminatorType.STRING)
		@AnyDiscriminatorValue(discriminator = "A", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@JdbcTypeCode(SqlTypes.CHAR)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "JdbcTypeCodeAnyKeyHolder")
	public static class JdbcTypeCodeKeyHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@AnyKeyJdbcTypeCode(SqlTypes.BIGINT)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "JavaTypeAnyKeyHolder")
	public static class JavaTypeKeyHolder {
		@Id
		private Integer id;

		@Any
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@AnyKeyJavaType(AnyIntegerJavaType.class)
		@JoinColumn(name = "target_id")
		private Object target;
	}

	@Entity(name = "ManyAnyHolder")
	public static class ManyHolder {
		@Id
		private Integer id;

		@ManyToAny
		@JoinTable(
				name = "many_holder_targets",
				joinColumns = @JoinColumn(name = "holder_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id")
		)
		@AnyDiscriminator(DiscriminatorType.INTEGER)
		@AnyDiscriminatorValue(discriminator = "1", entity = TargetOne.class)
		@AnyDiscriminatorValue(discriminator = "2", entity = TargetTwo.class)
		@AnyKeyJavaClass(Integer.class)
		@jakarta.persistence.Column(name = "target_type")
		private List<Object> targets;
	}

	@Entity(name = "JdbcTypeCodeAnyKeyManyHolder")
	public static class JdbcTypeCodeKeyManyHolder {
		@Id
		private Integer id;

		@ManyToAny
		@JoinTable(
				name = "jdbc_type_key_many_holder_targets",
				joinColumns = @JoinColumn(name = "holder_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id")
		)
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		@AnyKeyJdbcTypeCode(SqlTypes.BIGINT)
		private List<Object> targets;
	}

	@Entity(name = "ImplicitJoinTableManyAnyHolder")
	public static class ImplicitJoinTableManyHolder {
		@Id
		private Integer id;

		@ManyToAny
		@AnyDiscriminatorValue(discriminator = "one", entity = TargetOne.class)
		@AnyKeyJavaClass(Integer.class)
		private List<Object> targets;
	}

	@Entity(name = "AnyTargetOne")
	public static class TargetOne {
		@Id
		private Integer id;
	}

	@Entity(name = "AnyTargetTwo")
	public static class TargetTwo {
		@Id
		private Integer id;
	}

	public static class AnyIntegerJavaType extends AbstractClassJavaType<Integer> {
		public AnyIntegerJavaType() {
			super( Integer.class );
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return indicators.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.INTEGER );
		}

		@Override
		public Integer fromString(CharSequence string) {
			return string == null ? null : Integer.valueOf( string.toString() );
		}

		@Override
		public <X> X unwrap(Integer value, Class<X> type, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( type.isAssignableFrom( Integer.class ) ) {
				return type.cast( value );
			}
			throw unknownUnwrap( type );
		}

		@Override
		public <X> Integer wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( value instanceof Integer integer ) {
				return integer;
			}
			throw unknownWrap( value.getClass() );
		}
	}
}
