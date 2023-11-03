/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;

import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.Subselect;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.AnnotationPlacementException;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.spi.BindingHelper;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.bind.spi.QuotedIdentifierTarget;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ManagedTypeMetadata;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;

import jakarta.persistence.SecondaryTable;

import static org.hibernate.models.orm.bind.spi.QuotedIdentifierTarget.CATALOG_NAME;
import static org.hibernate.models.orm.bind.spi.QuotedIdentifierTarget.SCHEMA_NAME;
import static org.hibernate.models.orm.bind.spi.QuotedIdentifierTarget.TABLE_NAME;

/**
 * @author Steve Ebersole
 */
public class TableBinder {
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	private final ImplicitNamingStrategy implicitNamingStrategy;
	private final PhysicalNamingStrategy physicalNamingStrategy;
	private final JdbcEnvironment jdbcEnvironment;

	private List<TableSecondPass> callbackQueue;

	public TableBinder(
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;

		this.implicitNamingStrategy = bindingContext
				.getBootstrapContext()
				.getMetadataBuildingOptions()
				.getImplicitNamingStrategy();
		this.physicalNamingStrategy = bindingContext
				.getBootstrapContext()
				.getMetadataBuildingOptions()
				.getPhysicalNamingStrategy();
		this.jdbcEnvironment = bindingContext.getServiceRegistry().getService( JdbcEnvironment.class );
	}

	@FunctionalInterface
	public interface TableSecondPass {
		boolean processTable();
	}

	public void processQueue() {
		if ( callbackQueue == null ) {
			return;
		}

		final Iterator<TableSecondPass> secondPassItr = callbackQueue.iterator();
		while ( secondPassItr.hasNext() ) {
			final TableSecondPass secondPass = secondPassItr.next();
			try {
				final boolean success = secondPass.processTable();
				if ( success ) {
					secondPassItr.remove();
				}
			}
			catch (Exception ignoreForNow) {
			}
		}
	}

	public void processTables(IdentifiableTypeMetadata type) {
		if ( type.getManagedTypeKind() != ManagedTypeMetadata.Kind.ENTITY ) {
			// tables are only valid on entity, not mapped-super
			return;
		}

		final ClassDetails typeClassDetails = type.getClassDetails();
		final AnnotationUsage<jakarta.persistence.Table> tableAnn = typeClassDetails.getAnnotationUsage( jakarta.persistence.Table.class );
		final AnnotationUsage<Subselect> subselectAnn = typeClassDetails.getAnnotationUsage( Subselect.class );

		if ( subselectAnn != null ) {
			if ( tableAnn != null ) {
				throw new AnnotationPlacementException( "Illegal combination of @Table and @Subselect on " + typeClassDetails.getName() );
			}
			processVirtualTable( (EntityTypeMetadata) type, subselectAnn );
		}
		else {
			// either an explicit or implicit @Table
			processPhysicalTable( (EntityTypeMetadata) type, tableAnn );
		}

		final List<AnnotationUsage<SecondaryTable>> secondaryTableAnns = typeClassDetails.getRepeatedAnnotationUsages( SecondaryTable.class );
		secondaryTableAnns.forEach( (secondaryTableAnn) -> {
			final AnnotationUsage<SecondaryRow> secondaryRowAnn = typeClassDetails.getNamedAnnotationUsage(
					SecondaryRow.class,
					secondaryTableAnn.getString( "name" ),
					"table"
			);
			processSecondaryTable( (EntityTypeMetadata) type, secondaryTableAnn, secondaryRowAnn );
		} );
	}

	private void processVirtualTable(EntityTypeMetadata type, AnnotationUsage<Subselect> subselectAnn) {
		final Identifier logicalName = implicitNamingStrategy.determinePrimaryTableName(
				new ImplicitEntityNameSource() {
					@Override
					public EntityNaming getEntityNaming() {
						return type;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						throw new UnsupportedOperationException( "Not (yet) implemented" );
					}
				}
		);
		final InLineView binding = new InLineView(
				logicalName,
				BindingHelper.getString( subselectAnn, "value", Subselect.class, bindingContext )
		);
		bindingState.addTable( binding );
	}

	private void processPhysicalTable(EntityTypeMetadata type, AnnotationUsage<jakarta.persistence.Table> tableAnn) {
		final PhysicalTable physicalTable;

		if ( tableAnn != null ) {
			physicalTable = createExplicitPhysicalTable( type, tableAnn );
		}
		else {
			physicalTable = createImplicitPhysicalTable( type );
		}

		bindingState.addTable( physicalTable );
	}

	private PhysicalTable createImplicitPhysicalTable(EntityTypeMetadata type) {
		final Identifier logicalName = implicitNamingStrategy.determinePrimaryTableName(
				new ImplicitEntityNameSource() {
					@Override
					public EntityNaming getEntityNaming() {
						return type;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						throw new UnsupportedOperationException( "Not (yet) implemented" );
					}
				}
		);

		return new PhysicalTable(
				logicalName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				resolveDatabaseIdentifier(
						null,
						"catalog",
						jakarta.persistence.Table.class,
						bindingOptions.getDefaultCatalogName(),
						CATALOG_NAME
				),
				resolveDatabaseIdentifier(
						null,
						"schema",
						jakarta.persistence.Table.class,
						bindingOptions.getDefaultSchemaName(),
						SCHEMA_NAME
				),
				type.isAbstract(),
				null,
				null
		);
	}

	private PhysicalTable createExplicitPhysicalTable(EntityTypeMetadata type, AnnotationUsage<jakarta.persistence.Table> tableAnn) {
		final String name = StringHelper.nullIfEmpty( tableAnn.getString( "name" ) );
		final Identifier logicalName;

		if ( name == null ) {
			logicalName = implicitNamingStrategy.determinePrimaryTableName(
					new ImplicitEntityNameSource() {
						@Override
						public EntityNaming getEntityNaming() {
							return type;
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							throw new UnsupportedOperationException( "Not (yet) implemented" );
						}
					}
			);
		}
		else {
			logicalName = BindingHelper.toIdentifier( name, TABLE_NAME, bindingOptions, jdbcEnvironment );
		}

		return new PhysicalTable(
				logicalName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				resolveDatabaseIdentifier( tableAnn, "catalog", jakarta.persistence.Table.class, bindingOptions.getDefaultCatalogName(), CATALOG_NAME ),
				resolveDatabaseIdentifier( tableAnn, "schema", jakarta.persistence.Table.class, bindingOptions.getDefaultSchemaName(), SCHEMA_NAME ),
				false,
				BindingHelper.getString( tableAnn, "comment", jakarta.persistence.Table.class, bindingContext ),
				BindingHelper.getString( tableAnn, "options", jakarta.persistence.Table.class, bindingContext )
		);
	}

	private <A extends Annotation> Identifier resolveDatabaseIdentifier(
			AnnotationUsage<A> annotationUsage,
			String attributeName,
			Class<A> annotationType,
			Identifier fallback,
			QuotedIdentifierTarget target) {
		final String explicit = BindingHelper.getStringOrNull( annotationUsage, attributeName );
		if ( StringHelper.isNotEmpty( explicit ) ) {
			return BindingHelper.toIdentifier( explicit, target, bindingOptions, jdbcEnvironment );
		}

		if ( fallback != null ) {
			return fallback;
		}

		final String defaultValue = BindingHelper.getDefaultValue( attributeName, annotationType, bindingContext );
		return BindingHelper.toIdentifier(defaultValue, target, bindingOptions, jdbcEnvironment );
	}

	private void processSecondaryTable(
			EntityTypeMetadata type,
			AnnotationUsage<SecondaryTable> secondaryTableAnn,
			AnnotationUsage<SecondaryRow> secondaryRowAnn) {
		final String name = StringHelper.nullIfEmpty( secondaryTableAnn.getString( "name" ) );
		final Identifier logicalName;

		if ( name == null ) {
			logicalName = implicitNamingStrategy.determinePrimaryTableName(
					new ImplicitEntityNameSource() {
						@Override
						public EntityNaming getEntityNaming() {
							return type;
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							throw new UnsupportedOperationException( "Not (yet) implemented" );
						}
					}
			);
		}
		else {
			logicalName = BindingHelper.toIdentifier( name, TABLE_NAME, bindingOptions, jdbcEnvironment );
		}

		final PhysicalTable physicalTable = new PhysicalTable(
				logicalName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				resolveDatabaseIdentifier(
						secondaryTableAnn,
						"catalog",
						SecondaryTable.class,
						bindingOptions.getDefaultCatalogName(),
						CATALOG_NAME
				),
				resolveDatabaseIdentifier(
						secondaryTableAnn,
						"schema",
						SecondaryTable.class,
						bindingOptions.getDefaultSchemaName(),
						SCHEMA_NAME
				),
				false,
				BindingHelper.getString( secondaryTableAnn, "comment", SecondaryTable.class, bindingContext ),
				BindingHelper.getString( secondaryTableAnn, "options", SecondaryTable.class, bindingContext )
		);

		bindingState.addTable( physicalTable );
	}

}
