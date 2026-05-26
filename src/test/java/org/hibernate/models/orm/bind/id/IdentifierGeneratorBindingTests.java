/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.id;

import org.hibernate.MappingException;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.models.orm.bind.BindingTestingHelper.checkDomainModel;

/**
 * Tests identifier generator resolution that ORM historically delayed through
 * {@code IdGeneratorResolverSecondPass}.
 */
public class IdentifierGeneratorBindingTests {
	@Test
	@ServiceRegistry
	void sequenceGeneratedValueResolvesGenerator(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> assertGeneratedIdentifier( context.getMetadataCollector()
						.getEntityBinding( SequenceGeneratedEntity.class.getName() ) ),
				scope.getRegistry(),
				SequenceGeneratedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void tableGeneratedValueResolvesGenerator(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> assertGeneratedIdentifier( context.getMetadataCollector()
						.getEntityBinding( TableGeneratedEntity.class.getName() ) ),
				scope.getRegistry(),
				TableGeneratedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void identityGeneratedValueMarksIdentifierColumn(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final PersistentClass entityBinding = context.getMetadataCollector()
							.getEntityBinding( IdentityGeneratedEntity.class.getName() );
					final BasicValue identifier = (BasicValue) entityBinding.getIdentifier();

					assertThat( identifier.getCustomIdGeneratorCreator().isAssigned() ).isFalse();
					assertThat( identifier.getColumns().get( 0 ).isIdentity() ).isTrue();
				},
				scope.getRegistry(),
				IdentityGeneratedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void uuidGeneratedValueResolvesGenerator(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> assertGeneratedIdentifier( context.getMetadataCollector()
						.getEntityBinding( UuidGeneratedEntity.class.getName() ) ),
				scope.getRegistry(),
				UuidGeneratedEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void sequenceGeneratedValueRejectsTableGeneratorReference(ServiceRegistryScope scope) {
		assertThatThrownBy( () -> checkDomainModel(
				(context) -> {
				},
				scope.getRegistry(),
				SequenceReferencingTableGeneratorEntity.class
		) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "specified SEQUENCE generation, but referred to a @TableGenerator" );
	}

	private static void assertGeneratedIdentifier(PersistentClass entityBinding) {
		final BasicValue identifier = (BasicValue) entityBinding.getIdentifier();
		assertThat( identifier.getCustomIdGeneratorCreator().isAssigned() ).isFalse();
	}

	@Entity(name = "SequenceGeneratedEntity")
	@SequenceGenerator(name = "named_sequence", sequenceName = "named_sequence")
	public static class SequenceGeneratedEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "named_sequence")
		private Long id;
	}

	@Entity(name = "TableGeneratedEntity")
	@TableGenerator(name = "named_table", table = "named_table_generator")
	public static class TableGeneratedEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "named_table")
		private Long id;
	}

	@Entity(name = "IdentityGeneratedEntity")
	public static class IdentityGeneratedEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
	}

	@Entity(name = "UuidGeneratedEntity")
	public static class UuidGeneratedEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.UUID)
		private java.util.UUID id;
	}

	@Entity(name = "SequenceReferencingTableGeneratorEntity")
	@TableGenerator(name = "wrong_kind", table = "wrong_kind_generator")
	public static class SequenceReferencingTableGeneratorEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wrong_kind")
		private Long id;
	}
}
