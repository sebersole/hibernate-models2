/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.BasicJavaType;

import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static jakarta.persistence.EnumType.ORDINAL;
import static org.hibernate.annotations.TimeZoneStorageType.AUTO;
import static org.hibernate.annotations.TimeZoneStorageType.COLUMN;

/**
 * @author Steve Ebersole
 */
public class BasicValueBinder {

	public static void bindJavaType(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		// todo : do we need to account for JavaTypeRegistration here?
		final var javaTypeAnn = member.getDirectAnnotationUsage( JavaType.class );
		if ( javaTypeAnn == null ) {
			return;
		}

		basicValue.setExplicitJavaTypeAccess( (typeConfiguration) -> {
			final Class<BasicJavaType<?>> javaClass = (Class<BasicJavaType<?>>) javaTypeAnn.value();
			try {
				return javaClass.getConstructor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				final ModelsException modelsException = new ModelsException( "Error instantiating local @JavaType - " + member.getName() );
				modelsException.addSuppressed( e );
				throw modelsException;
			}
		} );
	}

	public static void bindJdbcType(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		// todo : do we need to account for JdbcTypeRegistration here?
		final var jdbcTypeAnn = member.getDirectAnnotationUsage( JdbcType.class );
		final var jdbcTypeCodeAnn = member.getDirectAnnotationUsage( JdbcTypeCode.class );

		if ( jdbcTypeAnn != null ) {
			if ( jdbcTypeCodeAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @JdbcType and @JdbcTypeCode - " + member.getName()
				);
			}

			basicValue.setExplicitJdbcTypeAccess( (typeConfiguration) -> {
				final Class<org.hibernate.type.descriptor.jdbc.JdbcType> javaClass = (Class<org.hibernate.type.descriptor.jdbc.JdbcType>) jdbcTypeAnn.value();
				try {
					return javaClass.getConstructor().newInstance();
				}
				catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					final ModelsException modelsException = new ModelsException( "Error instantiating local @JdbcType - " + member.getName() );
					modelsException.addSuppressed( e );
					throw modelsException;
				}
			} );
		}
		else if ( jdbcTypeCodeAnn != null ) {
			basicValue.setExplicitJdbcTypeCode( jdbcTypeCodeAnn.value() );
		}
	}

	public static void bindNationalized(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( member.hasDirectAnnotationUsage( Nationalized.class ) ) {
			basicValue.makeNationalized();
		}
	}

	public static void bindLob(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		if ( member.hasDirectAnnotationUsage( Lob.class ) ) {
			basicValue.makeLob();
		}
	}

	public static void bindEnumerated(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Enumerated enumerated = member.getDirectAnnotationUsage( Enumerated.class );
		if ( enumerated == null ) {
			return;
		}

		basicValue.setEnumerationStyle( enumerated.value() == null ? ORDINAL : enumerated.value() );
	}

	public static void bindTemporalPrecision(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Temporal temporalAnn = member.getDirectAnnotationUsage( Temporal.class );
		if ( temporalAnn == null ) {
			return;
		}

		//noinspection deprecation
		final TemporalType precision = temporalAnn.value();
		basicValue.setTemporalPrecision( precision );
	}

	public static void bindTimeZoneStorage(
			MemberDetails member,
			Property property,
			BasicValue basicValue,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final TimeZoneStorage storageAnn = member.getDirectAnnotationUsage( TimeZoneStorage.class );
		final TimeZoneColumn columnAnn = member.getDirectAnnotationUsage( TimeZoneColumn.class );
		if ( storageAnn != null ) {
			final TimeZoneStorageType strategy = storageAnn.value() == null ? AUTO : storageAnn.value();
			if ( strategy != COLUMN && columnAnn != null ) {
				throw new AnnotationPlacementException(
						"Illegal combination of @TimeZoneStorage(" + strategy.name() + ") and @TimeZoneColumn"
				);
			}
			basicValue.setTimeZoneStorageType( strategy );
		}

		if ( columnAnn != null ) {
			final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) basicValue.getColumn();
			column.setName( columnAnn.name().isEmpty() ? property.getName() + "_tz" : columnAnn.name() );
			column.setSqlType( columnAnn.columnDefinition().isEmpty() ? null : columnAnn.columnDefinition() );

			final var tableName = columnAnn.table().isEmpty() ? null : columnAnn.table();
			TableReference tableByName = null;
			if ( tableName != null ) {
				final Identifier identifier = Identifier.toIdentifier( tableName );
				tableByName = bindingState.getTableByName( identifier.getCanonicalName() );
				basicValue.setTable( tableByName.binding() );
			}

			property.setInsertable( columnAnn.insertable() );
			property.setUpdateable( columnAnn.updatable() );
		}
	}

}
