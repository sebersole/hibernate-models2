/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.models.bind.ModelBindingLogging;
import org.hibernate.boot.models.bind.internal.binders.ManagedTypeBinder;
import org.hibernate.mapping.RootClass;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.internal.binders.ModelBinders;
import org.hibernate.boot.models.bind.internal.binders.EntityTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.MappedSuperTypeBinder;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;

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

	private final ModelBinders modelBinders;

	public BindingCoordinator(
			CategorizedDomainModel categorizedDomainModel,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this.categorizedDomainModel = categorizedDomainModel;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;

		this.modelBinders = new ModelBinders( bindingState, bindingOptions, bindingContext );
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
		final BindingCoordinator coordinator = new BindingCoordinator(
				categorizedDomainModel,
				state,
				options,
				bindingContext
		);

		coordinator.coordinateBinding();
	}

	private void coordinateBinding() {
		// todo : to really work on these, need to changes to MetadataBuildingContext/InFlightMetadataCollector

		coordinateGlobalBindings();
		coordinateModelBindings();
	}

	private void coordinateModelBindings() {
		// process hierarchy
		categorizedDomainModel.forEachEntityHierarchy( this::processHierarchy );

		// complete tables
		modelBinders.getTableBinder().processSecondPasses();

		// process identifiers
		categorizedDomainModel.forEachEntityHierarchy( (index, hierarchy) -> {
			final EntityTypeBinder typeBinder = (EntityTypeBinder) bindingState.getTypeBinder( hierarchy.getRoot() );
			final RootClass binding = (RootClass) typeBinder.getTypeBinding();
			ModelBindingLogging.MODEL_BINDING_LOGGER.tracef( "Bound entity hierarchy - %s", binding.getEntityName() );
		} );

		bindingState.forEachType( this::processModelSecondPasses );
	}

	private void processModelSecondPasses(String typeName, ManagedTypeBinder binder) {
		binder.processSecondPasses();
	}

	private void coordinateGlobalBindings() {
		processGenerators( categorizedDomainModel.getGlobalRegistrations() );
		processConverters( categorizedDomainModel.getGlobalRegistrations() );
		processJavaTypeRegistrations( categorizedDomainModel.getGlobalRegistrations() );
		processJdbcTypeRegistrations( categorizedDomainModel.getGlobalRegistrations() );
		processCustomTypes( categorizedDomainModel.getGlobalRegistrations() );
		processInstantiators( categorizedDomainModel.getGlobalRegistrations() );
		processEventListeners( categorizedDomainModel.getGlobalRegistrations() );
		processFilterDefinitions( categorizedDomainModel.getGlobalRegistrations() );
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
					modelBinders,
					bindingState,
					bindingOptions,
					bindingContext
			);
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
