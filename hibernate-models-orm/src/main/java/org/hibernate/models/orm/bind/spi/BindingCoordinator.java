/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import java.lang.annotation.Annotation;
import java.util.List;

import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.Subselect;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.models.internal.StringHelper;
import org.hibernate.models.orm.AnnotationPlacementException;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.GlobalRegistrations;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ManagedTypeMetadata;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;

import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import static org.hibernate.models.orm.bind.spi.QuotedIdentifierTarget.CATALOG_NAME;
import static org.hibernate.models.orm.bind.spi.QuotedIdentifierTarget.SCHEMA_NAME;
import static org.hibernate.models.orm.bind.spi.QuotedIdentifierTarget.TABLE_NAME;

/**
 * Responsible for processing {@linkplain org.hibernate.boot.model.process.spi.ManagedResources managed-resources}
 * and binding them into Hibernate's {@linkplain org.hibernate.mapping boot-time model}.
 *
 * @author Steve Ebersole
 */
public class BindingCoordinator {
	private final CategorizedDomainModel categorizedDomainModel;
	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext bindingContext;

	private final ImplicitNamingStrategy implicitNamingStrategy;
	private final PhysicalNamingStrategy physicalNamingStrategy;
	private final JdbcEnvironment jdbcEnvironment;

	public BindingCoordinator(
			CategorizedDomainModel categorizedDomainModel,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		this.categorizedDomainModel = categorizedDomainModel;
		this.options = options;
		this.state = state;
		this.bindingContext = bindingContext;

		this.implicitNamingStrategy = new ImplicitNamingStrategyComponentPathImpl();
		this.physicalNamingStrategy = new PhysicalNamingStrategyStandardImpl();
		this.jdbcEnvironment = bindingContext.getServiceRegistry().getService( JdbcEnvironment.class );
	}

	/**
	 * Main entry point into this binding coordination
	 *
	 * @param categorizedDomainModel The model to be processed
	 * @param options Options for the binding
	 * @param bindingContext Access to needed information and delegates
	 */
	public static void coordinateBinding(
			CategorizedDomainModel categorizedDomainModel,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		final BindingCoordinator bindingCoordinator = new BindingCoordinator(
				categorizedDomainModel,
				state,
				options,
				bindingContext
		);

		// todo : to really work on these, need to expose MetadataBuildingContext/InFlightMetadataCollector

		// "global" bindings
		bindingCoordinator.processGenerators( categorizedDomainModel.getGlobalRegistrations() );
		bindingCoordinator.processConverters( categorizedDomainModel.getGlobalRegistrations() );
		bindingCoordinator.processJavaTypeRegistrations( categorizedDomainModel.getGlobalRegistrations() );
		bindingCoordinator.processJdbcTypeRegistrations( categorizedDomainModel.getGlobalRegistrations() );
		bindingCoordinator.processCustomTypes( categorizedDomainModel.getGlobalRegistrations() );
		bindingCoordinator.processInstantiators( categorizedDomainModel.getGlobalRegistrations() );
		bindingCoordinator.processEventListeners( categorizedDomainModel.getGlobalRegistrations() );
		bindingCoordinator.processFilterDefinitions( categorizedDomainModel.getGlobalRegistrations() );

		categorizedDomainModel.forEachEntityHierarchy( (typeIndex, hierarchy) -> {
			// hierarchy bindings
			hierarchy.getIdMapping().forEachAttribute( (attributeIndex, attribute) -> {
				bindingCoordinator.processLocalGenerators( attribute );
			} );

			hierarchy.forEachType( (type) -> {
				// type bindings
				bindingCoordinator.processTables( type );

				type.forEachAttribute( (attributeIndex, attribute) -> {
					// attribute bindings
					bindingCoordinator.processTables( attribute );
				} );
			} );
		} );
	}

	private void processGenerators(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getSequenceGeneratorRegistrations();
		globalRegistrations.getTableGeneratorRegistrations();
		globalRegistrations.getGenericGeneratorRegistrations();
	}

	private void processConverters(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getConverterRegistrations();
	}

	private void processJavaTypeRegistrations(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getJavaTypeRegistrations();
	}

	private void processJdbcTypeRegistrations(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getJdbcTypeRegistrations();
	}

	private void processCustomTypes(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getUserTypeRegistrations();
		globalRegistrations.getCompositeUserTypeRegistrations();
		globalRegistrations.getCollectionTypeRegistrations();
	}

	private void processInstantiators(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getEmbeddableInstantiatorRegistrations();
	}

	private void processEventListeners(GlobalRegistrations globalRegistrations) {
		// todo : process these
		globalRegistrations.getEntityListenerRegistrations();
	}

	private void processFilterDefinitions(GlobalRegistrations globalRegistrations) {

	}

	private void processLocalGenerators(AttributeMetadata idAttribute) {

	}

	private void processTables(AttributeMetadata attribute) {
		// join tables
		// collection tables
	}

	private void processGenerators(IdentifiableTypeMetadata type) {
		final ClassDetails typeClassDetails = type.getClassDetails();

		final List<AnnotationUsage<TableGenerator>> tableGenerators = typeClassDetails.getRepeatedAnnotationUsages( TableGenerator.class );
		tableGenerators.forEach( (tableGeneratorAnn) -> {
			// process both the table and the generator
		} );

		final List<AnnotationUsage<SequenceGenerator>> sequenceGenerators = typeClassDetails.getRepeatedAnnotationUsages( SequenceGenerator.class );
		sequenceGenerators.forEach( (sequenceGeneratorAnn) -> {
			// process both the sequence and the generator
		} );

	}

	private void processTables(IdentifiableTypeMetadata type) {
		if ( type.getManagedTypeKind() != ManagedTypeMetadata.Kind.ENTITY ) {
			// tables are only valid on entity, not mapped-super
			return;
		}

		final ClassDetails typeClassDetails = type.getClassDetails();
		final AnnotationUsage<Table> tableAnn = typeClassDetails.getAnnotationUsage( Table.class );
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
		secondaryTableAnns.forEach( (secondaryTableAnnotation) -> {
			final AnnotationUsage<SecondaryRow> secondaryRowAnn = typeClassDetails.getNamedAnnotationUsage(
					SecondaryRow.class,
					secondaryTableAnnotation.getString( "name" ),
					"table"
			);
		} );
	}

	private void processPhysicalTable(EntityTypeMetadata type, AnnotationUsage<Table> tableAnn) {
		final TableBinding tableBinding;

		if ( tableAnn != null ) {
			tableBinding = createExplicitPhysicalTable( type, tableAnn );
		}
		else {
			tableBinding = createImplicitPhysicalTable( type );
		}

		state.addTableBinding( tableBinding );
	}

	private TableBinding createImplicitPhysicalTable(EntityTypeMetadata type) {
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

		return new TableBinding(
				logicalName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				resolveDatabaseIdentifier(
						null,
						"catalog",
						Table.class,
						options.getDefaultCatalogName(),
						CATALOG_NAME
				),
				resolveDatabaseIdentifier(
						null,
						"schema",
						Table.class,
						options.getDefaultSchemaName(),
						SCHEMA_NAME
				),
				null,
				null
		);
	}

	private TableBinding createExplicitPhysicalTable(EntityTypeMetadata type, AnnotationUsage<Table> tableAnn) {
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
			logicalName = BindingHelper.toIdentifier( name, TABLE_NAME, options, jdbcEnvironment );
		}

		return new TableBinding(
				logicalName,
				physicalNamingStrategy.toPhysicalTableName( logicalName, jdbcEnvironment ),
				resolveDatabaseIdentifier( tableAnn, "catalog", Table.class, options.getDefaultCatalogName(), CATALOG_NAME ),
				resolveDatabaseIdentifier( tableAnn, "schema", Table.class, options.getDefaultSchemaName(), SCHEMA_NAME ),
//				BindingHelper.getString( tableAnn, "comment", Table.class, bindingContext ),
//				BindingHelper.getString( tableAnn, "options", Table.class, bindingContext )
				null,
				null
		);
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
		final VirtualTableBinding binding = new VirtualTableBinding(
				logicalName,
				BindingHelper.getString( subselectAnn, "value", Subselect.class, bindingContext )
		);
		state.addVirtualTableBinding( binding );
	}

	private <A extends Annotation> Identifier getDatabaseIdentifier(
			AnnotationUsage<A> annotationUsage,
			String attributeName,
			Class<A> annotationType,
			QuotedIdentifierTarget target) {
		return BindingHelper.getIdentifier(
				annotationUsage,
				attributeName,
				annotationType,
				target,
				options,
				jdbcEnvironment,
				bindingContext
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
			return BindingHelper.toIdentifier( explicit, target, options, jdbcEnvironment );
		}

		if ( fallback != null ) {
			return fallback;
		}

		final String defaultValue = BindingHelper.getDefaultValue( attributeName, annotationType, bindingContext );
		return BindingHelper.toIdentifier(defaultValue, target, options, jdbcEnvironment );
	}
}
