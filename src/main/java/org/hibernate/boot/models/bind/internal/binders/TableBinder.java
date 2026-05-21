/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.hibernate.boot.models.bind.internal.sources.TableSource;
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
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinTable;
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
		final jakarta.persistence.Table tableAnn = typeClassDetails.getDirectAnnotationUsage( jakarta.persistence.Table.class );
		final JoinTable joinTableAnn = typeClassDetails.getDirectAnnotationUsage( JoinTable.class );
		final Subselect subselectAnn = typeClassDetails.getDirectAnnotationUsage( Subselect.class );

		if ( tableAnn != null && joinTableAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @Table and @JoinTable on " + typeClassDetails.getName() );
		}
		if ( tableAnn != null && subselectAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @Table and @Subselect on " + typeClassDetails.getName() );
		}
		if ( joinTableAnn != null && subselectAnn != null ) {
			throw new AnnotationPlacementException( "Illegal combination of @JoinTable and @Subselect on " + typeClassDetails.getName() );
		}

		final TableReference tableReference;

		if ( type.getHierarchy().getInheritanceType() == InheritanceType.TABLE_PER_CLASS ) {
			assert subselectAnn == null;

			if ( hierarchyRelation == EntityHierarchy.HierarchyRelation.ROOT ) {
				tableReference = bindPhysicalTable( type, TableSource.from( tableAnn ), true );
			}
			else {
				tableReference = bindUnionTable( type, TableSource.from( tableAnn ) );
			}
		}
		else if ( type.getHierarchy().getInheritanceType() == InheritanceType.SINGLE_TABLE ) {
			if ( hierarchyRelation == EntityHierarchy.HierarchyRelation.ROOT ) {
				tableReference = normalTableDetermination( type, subselectAnn, TableSource.from( tableAnn ) );
			}
			else {
				tableReference = null;
			}
		}
		else {
			tableReference = normalTableDetermination( type, subselectAnn, TableSource.from( joinTableAnn ) );
		}

		if ( tableReference != null ) {
			bindingState.addTable( type, tableReference );

			final PrimaryKey primaryKey = new PrimaryKey( tableReference.binding() );
			tableReference.binding().setPrimaryKey( primaryKey );
		}

		return tableReference;
	}

	private TableReference normalTableDetermination(
			EntityTypeMetadata type,
			Subselect subselectAnn,
			TableSource tableSource) {
		final TableReference tableReference;
		if ( subselectAnn != null ) {
			tableReference = bindVirtualTable( type, subselectAnn );
		}
		else {
			// either an explicit or implicit @Table
			tableReference = bindPhysicalTable( type, tableSource, true );
		}
		return tableReference;
	}

	private TableReference bindUnionTable(
			EntityTypeMetadata type,
			TableSource tableSource) {
		assert type.getSuperType() != null;

		final TableReference superTypeTable = bindingState.getTableByOwner( type.getSuperType() );
		final Table unionBaseTable = superTypeTable.binding();

		final Identifier logicalName = determineLogicalName( type, tableSource );
		final Identifier logicalSchemaName = resolveDatabaseIdentifier(
				tableSource == null ? null : tableSource.schema(),
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME
		);
		final Identifier logicalCatalogName = resolveDatabaseIdentifier(
				tableSource == null ? null : tableSource.catalog(),
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

	public List<org.hibernate.boot.models.bind.internal.SecondaryTable> bindSecondaryTables(EntityTypeBinder entityBinder) {
		final ClassDetails typeClassDetails = entityBinder.getManagedType().getClassDetails();

		final List<SecondaryTable> secondaryTableAnns = Arrays.asList( typeClassDetails.getRepeatedAnnotationUsages(
				SecondaryTable.class,
				bindingContext.getBootstrapContext().getModelsContext()
		) );
		final List<org.hibernate.boot.models.bind.internal.SecondaryTable> result = new ArrayList<>( secondaryTableAnns.size() );

		secondaryTableAnns.forEach( (secondaryTableAnn) -> {
			final SecondaryRow secondaryRowAnn = typeClassDetails.getNamedAnnotationUsage(
					SecondaryRow.class,
					secondaryTableAnn.name(),
					"table",
					bindingContext.getBootstrapContext().getModelsContext()
			);
			final org.hibernate.boot.models.bind.internal.SecondaryTable binding = bindSecondaryTable( entityBinder, secondaryTableAnn, secondaryRowAnn );
			result.add( binding );
			bindingState.addSecondaryTable( binding );
		} );
		return result;
	}

	private InLineView bindVirtualTable(EntityTypeMetadata type, Subselect subselectAnn) {
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
						subselectAnn.value(),
						true,
						bindingState.getMetadataBuildingContext(),
						false
				)
		);
	}

	private PhysicalTableReference bindPhysicalTable(
			EntityTypeMetadata type,
			TableSource tableSource,
			boolean isPrimary) {
		if ( tableSource != null ) {
			return bindExplicitPhysicalTable( type, tableSource, isPrimary );
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
				bindingState.getMetadataBuildingContext(),
				false
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

	private Comment findCommentAnnotation(
			EntityTypeMetadata type,
			Identifier logicalTableName,
			boolean isPrimary) {
		if ( isPrimary ) {
			final Comment unnamed = type.getClassDetails().getNamedAnnotationUsage(
					Comment.class,
					"",
					"on",
					bindingContext.getBootstrapContext().getModelsContext()
			);
			if ( unnamed != null ) {
				return unnamed;
			}
		}

		return type.getClassDetails().getNamedAnnotationUsage(
				Comment.class,
				logicalTableName.getCanonicalName(),
				"on",
				bindingContext.getBootstrapContext().getModelsContext()
		);
	}

	private Identifier determineLogicalName(EntityTypeMetadata type, TableSource tableSource) {
		if ( tableSource != null ) {
			final String name = tableSource.nonEmptyName();
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
			TableSource tableSource,
			boolean isPrimary) {
		final Identifier logicalName = determineLogicalName( type, tableSource );
		final Identifier logicalSchemaName = resolveDatabaseIdentifier(
				tableSource.schema(),
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME
		);
		final Identifier logicalCatalogName = resolveDatabaseIdentifier(
				tableSource.catalog(),
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME
		);

		final var binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				logicalSchemaName == null ? null : logicalSchemaName.getCanonicalName(),
				logicalCatalogName == null  ? null : logicalCatalogName.getCanonicalName(),
				logicalName.getCanonicalName(),
				null,
				type.isAbstract(),
				bindingState.getMetadataBuildingContext(),
				false
		);

		applyComment( binding, tableSource, findCommentAnnotation( type, logicalName, isPrimary ) );
		applyOptions( binding, tableSource );

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
			EntityTypeBinder entityBinder,
			SecondaryTable secondaryTableAnn,
			SecondaryRow secondaryRowAnn) {
		final TableSource tableSource = TableSource.from( secondaryTableAnn );
		final Identifier logicalName = determineLogicalName( entityBinder.getManagedType(), tableSource );
		final Identifier schemaName = resolveDatabaseIdentifier(
				tableSource.schema(),
				bindingOptions.getDefaultSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME
		);
		final Identifier catalogName = resolveDatabaseIdentifier(
				tableSource.catalog(),
				bindingOptions.getDefaultCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME
		);

		final var binding = bindingState.getMetadataBuildingContext().getMetadataCollector().addTable(
				toCanonicalName( schemaName ),
				toCanonicalName( catalogName ),
				logicalName.getCanonicalName(),
				null,
				false,
				bindingState.getMetadataBuildingContext(),
				false
		);

		applyComment( binding, tableSource, findCommentAnnotation( entityBinder.getManagedType(), logicalName, false ) );
		applyOptions( binding, tableSource );

		final Join join = new Join();
		join.setTable( binding );
		final boolean optional = secondaryRowAnn == null || secondaryRowAnn.optional();
		final boolean owned = secondaryRowAnn == null || secondaryRowAnn.owned();
		join.setOptional( optional );
		join.setInverse( !owned );
		join.setPersistentClass( entityBinder.getTypeBinding() );
		entityBinder.getTypeBinding().addJoin( join );

		return new org.hibernate.boot.models.bind.internal.SecondaryTable(
				logicalName,
				catalogName,
				schemaName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalCatalogName( catalogName, jdbcEnvironment ),
				physicalNamingStrategy.toPhysicalSchemaName( schemaName, jdbcEnvironment ),
				optional,
				owned,
				binding
		);
	}

	private String toCanonicalName(Identifier name) {
		if ( name == null ) {
			return null;
		}
		return name.getCanonicalName();
	}

	private Identifier resolveDatabaseIdentifier(
			String explicit,
			Identifier fallback,
			QuotedIdentifierTarget target) {
		if ( StringHelper.isNotEmpty( explicit ) ) {
			return BindingHelper.toIdentifier( explicit, target, bindingOptions, jdbcEnvironment );
		}

		if ( fallback != null ) {
			return fallback;
		}

		return null;
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

	private void applyComment(Table table, TableSource tableSource, Comment commentAnn) {
		if ( commentAnn != null ) {
			table.setComment( commentAnn.value() );
		}
		else if ( tableSource != null ) {
			final String comment = tableSource.comment();
			if ( StringHelper.isNotEmpty( comment ) ) {
				table.setComment( comment );
			}
		}
	}

	private void applyOptions(Table table, TableSource tableSource) {
		if ( tableSource != null ) {
			final String options = tableSource.options();
			if ( StringHelper.isNotEmpty( options ) ) {
//				table.setOptions( options );
				throw new UnsupportedOperationException( "Not yet implemented" );
			}
		}
	}
}
