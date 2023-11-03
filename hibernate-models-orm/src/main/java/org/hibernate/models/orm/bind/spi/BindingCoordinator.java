/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.bind.spi;

import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.models.orm.AnnotationPlacementException;
import org.hibernate.models.orm.bind.internal.TableBinder;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.GlobalRegistrations;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.source.spi.AnnotationUsage;
import org.hibernate.models.source.spi.ClassDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

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

	private final TableBinder tableBinder;

	public BindingCoordinator(
			CategorizedDomainModel categorizedDomainModel,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		this.categorizedDomainModel = categorizedDomainModel;
		this.options = options;
		this.state = state;
		this.bindingContext = bindingContext;

		this.tableBinder = new TableBinder( state, options, bindingContext );
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
			hierarchy.forEachType( (type) -> {
				// type bindings
				bindingCoordinator.processTables( type );
				bindingCoordinator.processGenerators( type );

				type.forEachAttribute( (attributeIndex, attribute) -> {
					// attribute bindings
					bindingCoordinator.processTables( attribute );
				} );
			} );
		} );

		bindingCoordinator.processQueues();
	}

	private void processQueues() {
		tableBinder.processQueue();
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
		globalRegistrations.getFilterDefRegistrations().forEach( (s, filterDefRegistration) -> {
			state.apply( filterDefRegistration );
		} );

	}

	private void processTables(AttributeMetadata attribute) {
		final AnnotationUsage<JoinTable> joinTableAnn = attribute.getMember().getAnnotationUsage( JoinTable.class );
		final AnnotationUsage<CollectionTable> collectionTableAnn = attribute.getMember().getAnnotationUsage( CollectionTable.class );

		final AnnotationUsage<OneToOne> oneToOneAnn = attribute.getMember().getAnnotationUsage( OneToOne.class );
		final AnnotationUsage<ManyToOne> manyToOneAnn = attribute.getMember().getAnnotationUsage( ManyToOne.class );
		final AnnotationUsage<ElementCollection> elementCollectionAnn = attribute.getMember().getAnnotationUsage( ElementCollection.class );
		final AnnotationUsage<OneToMany> oneToManyAnn = attribute.getMember().getAnnotationUsage( OneToMany.class );
		final AnnotationUsage<Any> anyAnn = attribute.getMember().getAnnotationUsage( Any.class );
		final AnnotationUsage<ManyToAny> manyToAnyAnn = attribute.getMember().getAnnotationUsage( ManyToAny.class );

		final boolean hasAnyTableAnnotations = joinTableAnn != null
				|| collectionTableAnn != null;

		final boolean hasAnyAssociationAnnotations = oneToOneAnn != null
				|| manyToOneAnn != null
				|| elementCollectionAnn != null
				|| oneToManyAnn != null
				|| anyAnn != null
				|| manyToAnyAnn != null;

		if ( !hasAnyAssociationAnnotations ) {
			if ( hasAnyTableAnnotations ) {
				throw new AnnotationPlacementException(
						"@JoinTable or @CollectionTable used on non-association attribute - " + attribute.getMember()
				);
			}
		}

		if ( elementCollectionAnn != null ) {
			if ( joinTableAnn != null ) {
				throw new AnnotationPlacementException(
						"@JoinTable should not be used with @ElementCollection; use @CollectionTable instead - " + attribute.getMember()
				);
			}

			// an element-collection "owns" the collection table, so create it right away

		}

		// ^^ accounting for owning v. "inverse" side
		//
		// on the owning side we get/create the reference and configure it
		//
		// on the inverse side we just get the reference.
		//
		// a cool idea here for "smarter second-pass"... on the inverse side -
		// 		TableReference mappedTable = bindingState.
		//

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
		tableBinder.processTables( type );
	}

}
