/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.ModelBindingLogging;
import org.hibernate.boot.models.bind.internal.binders.EntityTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.ManagedTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.MappedSuperTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.ModelBinders;
import org.hibernate.boot.models.bind.internal.binders.TypeBindingPhase;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.ClassDetails;

/// Coordinates binding of a categorized domain model into Hibernate's boot-time
/// mapping model.
///
/// The coordinator is the entry point for the binding phase.  It applies global
/// registrations, visits each categorized entity hierarchy, creates the appropriate
/// type binders, and then runs binding second passes that require tables or types to
/// be known first.
///
/// @author Steve Ebersole
public class BindingCoordinator {
	private final CategorizedDomainModel categorizedDomainModel;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	private final ModelBinders modelBinders;

	/// Create a binding coordinator for a categorized model.
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

	/// Main entry point for binding a categorized domain model.
	///
	/// @param categorizedDomainModel The categorized model to bind
	/// @param state Mutable binding state and produced mapping objects
	/// @param options Binding options in effect
	/// @param bindingContext Access to binding services and shared categorization state
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
		final List<ManagedTypeBinder> binders = new ArrayList<>();
		categorizedDomainModel.forEachEntityHierarchy( (index, hierarchy) -> {
			hierarchy.forEachType( (type, superType, entityHierarchy, relation) -> {
				binders.add( createIdentifiableTypeBinder( type, superType, entityHierarchy, relation ) );
			} );
		} );

		runPhase( binders, TypeBindingPhase.Tables.class, TypeBindingPhase.Tables::bindTables );
		runPhase( binders, TypeBindingPhase.SuperType.class, TypeBindingPhase.SuperType::bindSuperType );
		runPhase( binders, TypeBindingPhase.EntityMetadata.class, TypeBindingPhase.EntityMetadata::bindEntityMetadata );
		runPhase( binders, TypeBindingPhase.Identifiers.class, TypeBindingPhase.Identifiers::bindIdentifier );
		runPhase( binders, TypeBindingPhase.TableKeys.class, TypeBindingPhase.TableKeys::bindTableKeys );
		runPhase( binders, TypeBindingPhase.Members.class, TypeBindingPhase.Members::bindMembers );

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

	private <P> void runPhase(List<ManagedTypeBinder> binders, Class<P> phaseType, Consumer<P> phaseAction) {
		binders.forEach( (binder) -> {
			if ( phaseType.isInstance( binder ) ) {
				phaseAction.accept( phaseType.cast( binder ) );
			}
		} );
	}


	private ManagedTypeBinder createIdentifiableTypeBinder(
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
			bindTypeSkeleton( binder );
			return binder;
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
			bindTypeSkeleton( binder );
			return binder;
		}
	}

	private void bindTypeSkeleton(ManagedTypeBinder binder) {
		( (TypeBindingPhase.TypeSkeleton) binder ).bindTypeSkeleton();
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
		final JoinTable joinTableAnn = attribute.getMember().getDirectAnnotationUsage( JoinTable.class );
		final CollectionTable collectionTableAnn = attribute.getMember().getDirectAnnotationUsage( CollectionTable.class );

		final OneToOne oneToOneAnn = attribute.getMember().getDirectAnnotationUsage( OneToOne.class );
		final ManyToOne manyToOneAnn = attribute.getMember().getDirectAnnotationUsage( ManyToOne.class );
		final ElementCollection elementCollectionAnn = attribute.getMember().getDirectAnnotationUsage( ElementCollection.class );
		final OneToMany oneToManyAnn = attribute.getMember().getDirectAnnotationUsage( OneToMany.class );
		final Any anyAnn = attribute.getMember().getDirectAnnotationUsage( Any.class );
		final ManyToAny manyToAnyAnn = attribute.getMember().getDirectAnnotationUsage( ManyToAny.class );

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

		final TableGenerator[] tableGenerators = typeClassDetails.getRepeatedAnnotationUsages(
				TableGenerator.class,
				bindingContext.getBootstrapContext().getModelsContext()
		);
		for ( TableGenerator tableGeneratorAnn : tableGenerators ) {
			// process both the table and the generator
		}

		final SequenceGenerator[] sequenceGenerators = typeClassDetails.getRepeatedAnnotationUsages(
				SequenceGenerator.class,
				bindingContext.getBootstrapContext().getModelsContext()
		);
		for ( SequenceGenerator sequenceGeneratorAnn : sequenceGenerators ) {
			// process both the sequence and the generator
		}

	}

}
