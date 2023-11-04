/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.internal.binders;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.Subselect;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Table;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.AnnotationPlacementException;
import org.hibernate.models.orm.bind.internal.InLineView;
import org.hibernate.models.orm.bind.internal.PhysicalTable;
import org.hibernate.models.orm.bind.internal.SecondPass;
import org.hibernate.models.orm.bind.spi.BindingContext;
import org.hibernate.models.orm.bind.internal.BindingHelper;
import org.hibernate.models.orm.bind.spi.BindingOptions;
import org.hibernate.models.orm.bind.spi.BindingState;
import org.hibernate.models.orm.bind.spi.PhysicalTableReference;
import org.hibernate.models.orm.bind.spi.QuotedIdentifierTarget;
import org.hibernate.models.orm.bind.spi.TableReference;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
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
	private final DelegateBinders delegateBinders;

	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	private final ImplicitNamingStrategy implicitNamingStrategy;
	private final PhysicalNamingStrategy physicalNamingStrategy;

	private final JdbcEnvironment jdbcEnvironment;

	private List<TableBinder.TableSecondPass> secondPasses;

	public TableBinder(
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext,
			DelegateBinders delegateBinders) {
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;
		this.delegateBinders = delegateBinders;

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

	public TableReference processPrimaryTable(EntityTypeMetadata type) {
		final ClassDetails typeClassDetails = type.getClassDetails();
		final AnnotationUsage<jakarta.persistence.Table> tableAnn = typeClassDetails.getAnnotationUsage( jakarta.persistence.Table.class );
		final AnnotationUsage<Subselect> subselectAnn = typeClassDetails.getAnnotationUsage( Subselect.class );

		final TableReference tableReference;

		if ( subselectAnn != null ) {
			if ( tableAnn != null ) {
				throw new AnnotationPlacementException( "Illegal combination of @Table and @Subselect on " + typeClassDetails.getName() );
			}
			tableReference = processVirtualTable( type, subselectAnn );
		}
		else {
			// either an explicit or implicit @Table
			tableReference = processPhysicalTable( type, tableAnn, true );
		}

		bindingState.addTable( tableReference );
		return tableReference;
	}

	public List<org.hibernate.models.orm.bind.internal.SecondaryTable> processSecondaryTables(EntityTypeMetadata type) {
		final ClassDetails typeClassDetails = type.getClassDetails();

		final List<AnnotationUsage<SecondaryTable>> secondaryTableAnns = typeClassDetails.getRepeatedAnnotationUsages( SecondaryTable.class );
		final List<org.hibernate.models.orm.bind.internal.SecondaryTable > result = new ArrayList<>( secondaryTableAnns.size() );

		secondaryTableAnns.forEach( (secondaryTableAnn) -> {
			final AnnotationUsage<SecondaryRow> secondaryRowAnn = typeClassDetails.getNamedAnnotationUsage(
					SecondaryRow.class,
					secondaryTableAnn.getString( "name" ),
					"table"
			);
			final org.hibernate.models.orm.bind.internal.SecondaryTable binding = processSecondaryTable( type, secondaryTableAnn, secondaryRowAnn );
			result.add( binding );
			bindingState.addTable( binding );
		} );
		return result;
	}

	private InLineView processVirtualTable(EntityTypeMetadata type, AnnotationUsage<Subselect> subselectAnn) {
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

		return new InLineView(
				logicalName,
				bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
						null,
						null,
						logicalName.getCanonicalName(),
						BindingHelper.getString( subselectAnn, "value", Subselect.class, bindingContext ),
						true,
						bindingState.getMetadataBuildingContext()
				)
		);
	}

	private PhysicalTableReference processPhysicalTable(
			EntityTypeMetadata type,
			AnnotationUsage<jakarta.persistence.Table> tableAnn,
			boolean isPrimary) {
		final PhysicalTable physicalTable;

		if ( tableAnn != null ) {
			return createExplicitPhysicalTable( type, tableAnn, isPrimary );
		}
		else {
			return createImplicitPhysicalTable( type, isPrimary );
		}
	}

	private PhysicalTable createImplicitPhysicalTable(EntityTypeMetadata type, boolean isPrimary) {
		final Identifier logicalName = determineLogicalName( type, null );

		final Table binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				bindingOptions.getDefaultSchemaName().getCanonicalName(),
				bindingOptions.getDefaultCatalogName().getCanonicalName(),
				logicalName.getCanonicalName(),
				null,
				type.isAbstract(),
				bindingState.getMetadataBuildingContext()
		);

		applyComment(
				binding,
				null,
				findCommentAnnotation( type, logicalName, isPrimary )
		);

		return new PhysicalTable(
				logicalName,
				bindingOptions.getDefaultCatalogName(),
				bindingOptions.getDefaultSchemaName(),
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalCatalogName( bindingOptions.getDefaultCatalogName(), jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalSchemaName( bindingOptions.getDefaultSchemaName(), jdbcEnvironment ),
				binding
		);
	}

	private AnnotationUsage<Comment> findCommentAnnotation(
			EntityTypeMetadata type,
			Identifier logicalTableName,
			boolean isPrimary) {
		if ( isPrimary ) {
			final AnnotationUsage<Comment> unnamed = type.getClassDetails().getNamedAnnotationUsage(
					Comment.class,
					"",
					"on"
			);
			if ( unnamed != null ) {
				return unnamed;
			}
		}

		return type.getClassDetails().getNamedAnnotationUsage(
				Comment.class,
				logicalTableName.getCanonicalName(),
				"on"
		);
	}

	private Identifier determineLogicalName(EntityTypeMetadata type, AnnotationUsage<?> tableAnn) {
		if ( tableAnn != null ) {
			final String name = StringHelper.nullIfEmpty( tableAnn.getString( "name" ) );
			if ( name != null ) {
				return BindingHelper.toIdentifier( name, TABLE_NAME, bindingOptions, jdbcEnvironment );
			}
		}

		return implicitNamingStrategy.determinePrimaryTableName(
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

	private PhysicalTable createExplicitPhysicalTable(
			EntityTypeMetadata type,
			AnnotationUsage<jakarta.persistence.Table> tableAnn,
			boolean isPrimary) {
		final Identifier logicalName = determineLogicalName( type, tableAnn );
		final Identifier logicalSchemaName = resolveDatabaseIdentifier(
				tableAnn,
				"schema",
				jakarta.persistence.Table.class,
				bindingOptions.getDefaultSchemaName(),
				SCHEMA_NAME
		);
		final Identifier logicalCatalogName = resolveDatabaseIdentifier(
				tableAnn,
				"catalog",
				jakarta.persistence.Table.class,
				bindingOptions.getDefaultCatalogName(),
				CATALOG_NAME
		);

		final var binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				logicalSchemaName == null ? null : logicalSchemaName.getCanonicalName(),
				logicalCatalogName == null  ? null : logicalCatalogName.getCanonicalName(),
				logicalName.getCanonicalName(),
				null,
				type.isAbstract(),
				bindingState.getMetadataBuildingContext()
		);

		applyComment( binding, tableAnn, findCommentAnnotation( type, logicalName, isPrimary ) );
		applyOptions( binding, tableAnn );

		return new PhysicalTable(
				logicalName,
				logicalCatalogName,
				logicalSchemaName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				logicalCatalogName == null ? null : physicalNamingStrategy.toPhysicalCatalogName( logicalCatalogName, jdbcEnvironment ),
				logicalSchemaName == null ? null : physicalNamingStrategy.toPhysicalSchemaName( logicalSchemaName, jdbcEnvironment ),
				binding
		);
	}

	private org.hibernate.models.orm.bind.internal.SecondaryTable processSecondaryTable(
			EntityTypeMetadata type,
			AnnotationUsage<SecondaryTable> secondaryTableAnn,
			AnnotationUsage<SecondaryRow> secondaryRowAnn) {
		final Identifier logicalName = determineLogicalName( type, secondaryTableAnn );
		final Identifier schemaName = resolveDatabaseIdentifier(
				secondaryTableAnn,
				"schema",
				SecondaryTable.class,
				bindingOptions.getDefaultSchemaName(),
				SCHEMA_NAME
		);
		final Identifier catalogName = resolveDatabaseIdentifier(
				secondaryTableAnn,
				"catalog",
				SecondaryTable.class,
				bindingOptions.getDefaultCatalogName(),
				CATALOG_NAME
		);

		final var binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				schemaName.getCanonicalName(),
				catalogName.getCanonicalName(),
				logicalName.getCanonicalName(),
				null,
				false,
				bindingState.getMetadataBuildingContext()
		);

		applyComment( binding, secondaryTableAnn, findCommentAnnotation( type, logicalName, false ) );
		applyOptions( binding, secondaryTableAnn );

		return new org.hibernate.models.orm.bind.internal.SecondaryTable (
				logicalName,
				catalogName,
				schemaName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalCatalogName( catalogName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalSchemaName( schemaName, jdbcEnvironment ),
				BindingHelper.getValue( secondaryRowAnn, "optional", true ),
				BindingHelper.getValue( secondaryRowAnn, "owned", true ),
				binding
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


	@FunctionalInterface
	public interface TableSecondPass extends SecondPass {
		boolean processTable();

		@Override
		default boolean process() {
			return processTable();
		}
	}

	public void processSecondPasses() {
		BindingHelper.processSecondPassQueue( secondPasses );
	}

	private void applyComment(Table table, AnnotationUsage<?> tableAnn, AnnotationUsage<Comment> commentAnn) {
		if ( commentAnn != null ) {
			table.setComment( commentAnn.getString( "value" ) );
		}
		else if ( tableAnn != null ) {
			final String comment = tableAnn.getString( "comment" );
			if ( StringHelper.isNotEmpty( comment ) ) {
				table.setComment( comment );
			}
		}
	}

	private void applyOptions(Table table, AnnotationUsage<?> tableAnn) {
		if ( tableAnn != null ) {
			final String options = tableAnn.getString( "options" );
			if ( StringHelper.isNotEmpty( options ) ) {
//				table.setOptions( options );
				throw new UnsupportedOperationException( "Not yet implemented" );
			}
		}
	}
}
