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
import org.hibernate.mapping.RootClass;
import org.hibernate.models.orm.AnnotationPlacementException;
import org.hibernate.models.orm.bind.internal.binders.DelegateBinders;
import org.hibernate.models.orm.bind.internal.binders.EntityTypeBinder;
import org.hibernate.models.orm.bind.internal.binders.MappedSuperTypeBinder;
import org.hibernate.models.orm.categorize.spi.AttributeMetadata;
import org.hibernate.models.orm.categorize.spi.CategorizedDomainModel;
import org.hibernate.models.orm.categorize.spi.EntityHierarchy;
import org.hibernate.models.orm.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.orm.categorize.spi.GlobalRegistrations;
import org.hibernate.models.orm.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.models.orm.categorize.spi.ManagedTypeMetadata;
import org.hibernate.models.orm.categorize.spi.MappedSuperclassTypeMetadata;
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
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	private final DelegateBinders delegateBinders;

	public BindingCoordinator(
			CategorizedDomainModel categorizedDomainModel,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this.categorizedDomainModel = categorizedDomainModel;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;

		this.delegateBinders = new DelegateBinders( bindingState, bindingOptions, bindingContext );
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

		// process hierarchy
		categorizedDomainModel.forEachEntityHierarchy( bindingCoordinator::processHierarchy );

		// complete tables
		bindingCoordinator.delegateBinders.getTableBinder().processSecondPasses();
		bindingCoordinator.delegateBinders.getTableBinder().processSecondPasses();

		// process identifiers
		categorizedDomainModel.forEachEntityHierarchy( (index, hierarchy) -> {
			final EntityTypeBinder typeBinder = (EntityTypeBinder) state.getTypeBinder( hierarchy.getRoot() );
			final RootClass binding = (RootClass) typeBinder.getTypeBinding();

		} );

		state.forEachType( (name, managedTypeBinder) -> {
			managedTypeBinder.processSecondPasses();
		} );
	}

	private void processHierarchy(int index, EntityHierarchy hierarchy) {
		hierarchy.forEachType( this::processIdentifiableType );
	}


	private void processIdentifiableType(
			IdentifiableTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy hierarchy,
			EntityHierarchy.HierarchyRelation relation) {
		processGenerators( type );

		if ( type.getManagedTypeKind() == ManagedTypeMetadata.Kind.ENTITY ) {
			final EntityTypeBinder binder = new EntityTypeBinder(
					(EntityTypeMetadata) type,
					superType,
					relation,
					delegateBinders,
					bindingState,
					bindingOptions,
					bindingContext
			);
			bindingState.registerTypeBinder( type, binder );
		}
		else {
			assert type.getManagedTypeKind() == ManagedTypeMetadata.Kind.MAPPED_SUPER;
			final MappedSuperTypeBinder binder = new MappedSuperTypeBinder(
					(MappedSuperclassTypeMetadata) type,
					superType,
					relation,
					bindingState,
					bindingOptions,
					bindingContext
			);
			bindingState.registerTypeBinder( type, binder );
		}
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
			bindingState.apply( filterDefRegistration );
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

}
