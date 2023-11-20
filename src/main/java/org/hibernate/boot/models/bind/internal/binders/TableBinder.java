/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

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
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.internal.BindingHelper;
import org.hibernate.boot.models.bind.internal.InLineView;
import org.hibernate.boot.models.bind.internal.PhysicalTable;
import org.hibernate.boot.models.bind.internal.SecondPass;
import org.hibernate.boot.models.bind.internal.UnionTable;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.PhysicalTableReference;
import org.hibernate.boot.models.bind.spi.QuotedIdentifierTarget;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;

/**
 * @author Steve Ebersole
 */
public class TableBinder {
	private final ModelBinders modelBinders;

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
			ModelBinders modelBinders) {
		this.bindingState = bindingState;
		this.bindingOptions = bindingOptions;
		this.bindingContext = bindingContext;
		this.modelBinders = modelBinders;

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

	public TableReference bindPrimaryTable(EntityTypeMetadata type, EntityHierarchy.HierarchyRelation hierarchyRelation) {
		final ClassDetails typeClassDetails = type.getClassDetails();
		final AnnotationUsage<jakarta.persistence.Table> tableAnn = typeClassDetails.getAnnotationUsage( jakarta.persistence.Table.class );
		final AnnotationUsage<Subselect> subselectAnn = typeClassDetails.getAnnotationUsage( Subselect.class );

		final TableReference tableReference;

		if ( type.getHierarchy().getInheritanceType() == InheritanceType.TABLE_PER_CLASS ) {
			assert subselectAnn == null;

			if ( hierarchyRelation == EntityHierarchy.HierarchyRelation.ROOT ) {
				tableReference = bindPhysicalTable( type, tableAnn, true );
			}
			else {
				tableReference = bindUnionTable( type, tableAnn );
			}
		}
		else {
			if ( subselectAnn != null ) {
				if ( tableAnn != null ) {
					throw new AnnotationPlacementException( "Illegal combination of @Table and @Subselect on " + typeClassDetails.getName() );
				}
				tableReference = bindVirtualTable( type, subselectAnn );
			}
			else {
				// either an explicit or implicit @Table
				tableReference = bindPhysicalTable( type, tableAnn, true );
			}
		}

		bindingState.addTable( type, tableReference );

		final PrimaryKey primaryKey = new PrimaryKey( tableReference.binding() );
		tableReference.binding().setPrimaryKey( primaryKey );

		return tableReference;
	}

	private TableReference bindUnionTable(
			EntityTypeMetadata type,
			AnnotationUsage<jakarta.persistence.Table> tableAnn) {
		assert type.getSuperType() != null;

		final TableReference superTypeTable = bindingState.getTableByOwner( type.getSuperType() );
		final Table unionBaseTable = superTypeTable.binding();

		final Identifier logicalName = determineLogicalName( type, tableAnn );
		final Identifier logicalSchemaName = resolveDatabaseIdentifier(
				tableAnn,
				"schema",
				jakarta.persistence.Table.class,
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME
		);
		final Identifier logicalCatalogName = resolveDatabaseIdentifier(
				tableAnn,
				"catalog",
				jakarta.persistence.Table.class,
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME
		);

		final DenormalizedTable binding = (DenormalizedTable) bindingState.getMetadataBuildingContext().getMetadataCollector().addDenormalizedTable(
				logicalSchemaName == null ? null : logicalSchemaName.getCanonicalName(),
				logicalCatalogName == null  ? null : logicalCatalogName.getCanonicalName(),
				logicalName.getCanonicalName(),
				type.isAbstract(),
				null,
				unionBaseTable,
				bindingState.getMetadataBuildingContext()
		);

		return new UnionTable( logicalName, superTypeTable, binding, !type.hasSubTypes() );
	}

	public List<org.hibernate.boot.models.bind.internal.SecondaryTable> bindSecondaryTables(EntityTypeMetadata type) {
		final ClassDetails typeClassDetails = type.getClassDetails();

		final List<AnnotationUsage<SecondaryTable>> secondaryTableAnns = typeClassDetails.getRepeatedAnnotationUsages( SecondaryTable.class );
		final List<org.hibernate.boot.models.bind.internal.SecondaryTable> result = new ArrayList<>( secondaryTableAnns.size() );

		secondaryTableAnns.forEach( (secondaryTableAnn) -> {
			final AnnotationUsage<SecondaryRow> secondaryRowAnn = typeClassDetails.getNamedAnnotationUsage(
					SecondaryRow.class,
					secondaryTableAnn.getString( "name" ),
					"table"
			);
			final org.hibernate.boot.models.bind.internal.SecondaryTable binding = bindSecondaryTable( type, secondaryTableAnn, secondaryRowAnn );
			result.add( binding );
			bindingState.addSecondaryTable( binding );
		} );
		return result;
	}

	private InLineView bindVirtualTable(EntityTypeMetadata type, AnnotationUsage<Subselect> subselectAnn) {
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

	private PhysicalTableReference bindPhysicalTable(
			EntityTypeMetadata type,
			AnnotationUsage<jakarta.persistence.Table> tableAnn,
			boolean isPrimary) {
		if ( tableAnn != null ) {
			return bindExplicitPhysicalTable( type, tableAnn, isPrimary );
		}
		else {
			return bindImplicitPhysicalTable( type, isPrimary );
		}
	}

	private PhysicalTable bindImplicitPhysicalTable(EntityTypeMetadata type, boolean isPrimary) {
		final Identifier logicalName = determineLogicalName( type, null );

		final Table binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				bindingOptions.getDefaultSchemaName() == null ? null : bindingOptions.getDefaultSchemaName().getCanonicalName(),
				bindingOptions.getDefaultCatalogName() == null ? null : bindingOptions.getDefaultCatalogName().getCanonicalName(),
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
				return BindingHelper.toIdentifier( name, QuotedIdentifierTarget.TABLE_NAME, bindingOptions, jdbcEnvironment );
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
						return bindingState.getMetadataBuildingContext();
					}
				}
		);
	}

	private PhysicalTable bindExplicitPhysicalTable(
			EntityTypeMetadata type,
			AnnotationUsage<jakarta.persistence.Table> tableAnn,
			boolean isPrimary) {
		final Identifier logicalName = determineLogicalName( type, tableAnn );
		final Identifier logicalSchemaName = resolveDatabaseIdentifier(
				tableAnn,
				"schema",
				jakarta.persistence.Table.class,
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME
		);
		final Identifier logicalCatalogName = resolveDatabaseIdentifier(
				tableAnn,
				"catalog",
				jakarta.persistence.Table.class,
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME
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

	private org.hibernate.boot.models.bind.internal.SecondaryTable bindSecondaryTable(
			EntityTypeMetadata type,
			AnnotationUsage<SecondaryTable> secondaryTableAnn,
			AnnotationUsage<SecondaryRow> secondaryRowAnn) {
		final Identifier logicalName = determineLogicalName( type, secondaryTableAnn );
		final Identifier schemaName = resolveDatabaseIdentifier(
				secondaryTableAnn,
				"schema",
				SecondaryTable.class,
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME
		);
		final Identifier catalogName = resolveDatabaseIdentifier(
				secondaryTableAnn,
				"catalog",
				SecondaryTable.class,
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME
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

		return new org.hibernate.boot.models.bind.internal.SecondaryTable(
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
