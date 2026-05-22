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
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.internal.AnnotationHelper;
import org.hibernate.boot.model.internal.GeneratorParameters;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.models.bind.ModelBindingLogging;
import org.hibernate.boot.models.bind.internal.binders.EntityTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.ManagedTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.MappedSuperTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.ModelBinders;
import org.hibernate.boot.models.bind.internal.binders.TypeBindingPhase;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.CollectionTypeRegistration;
import org.hibernate.boot.models.categorize.spi.CompositeUserTypeRegistration;
import org.hibernate.boot.models.categorize.spi.ConversionRegistration;
import org.hibernate.boot.models.categorize.spi.EmbeddableInstantiatorRegistration;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.GenericGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.JavaTypeRegistration;
import org.hibernate.boot.models.categorize.spi.JdbcTypeRegistration;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.models.categorize.spi.MappedSuperclassTypeMetadata;
import org.hibernate.boot.models.categorize.spi.SequenceGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.TableGeneratorRegistration;
import org.hibernate.boot.models.categorize.spi.UserTypeRegistration;
import org.hibernate.generator.Generator;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.ModelsException;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/// Coordinates binding of a categorized domain model into Hibernate's boot-time
/// mapping model.
///
/// The coordinator is the entry point for the binding phase.  It applies global
/// registrations, visits each categorized entity hierarchy, creates the appropriate
/// type binders, and then runs ordered binding phases that make type, table,
/// identifier, member, and association state available to later phases.
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
		runPhase( binders, TypeBindingPhase.AssociationIdentifiers.class, TypeBindingPhase.AssociationIdentifiers::bindAssociationIdentifiers );
		runPhase( binders, TypeBindingPhase.Members.class, TypeBindingPhase.Members::bindMembers );
		runPhase( binders, TypeBindingPhase.CollectionIndexes.class, TypeBindingPhase.CollectionIndexes::bindCollectionIndexes );
		runPhase( binders, TypeBindingPhase.CollectionOrderings.class, TypeBindingPhase.CollectionOrderings::bindCollectionOrderings );
		runPhase( binders, TypeBindingPhase.AssociationTargets.class, TypeBindingPhase.AssociationTargets::bindAssociationTargets );
		runPhase( binders, TypeBindingPhase.DerivedIdentifiers.class, TypeBindingPhase.DerivedIdentifiers::bindDerivedIdentifiers );
		runPhase( binders, TypeBindingPhase.TableKeys.class, TypeBindingPhase.TableKeys::bindTableKeys );
		runPhase( binders, TypeBindingPhase.InverseAssociations.class, TypeBindingPhase.InverseAssociations::bindInverseAssociations );
		runPhase( binders, TypeBindingPhase.ForeignKeys.class, TypeBindingPhase.ForeignKeys::bindForeignKeys );

		// process identifiers
		categorizedDomainModel.forEachEntityHierarchy( (index, hierarchy) -> {
			final EntityTypeBinder typeBinder = (EntityTypeBinder) bindingState.getTypeBinder( hierarchy.getRoot() );
			final RootClass binding = (RootClass) typeBinder.getTypeBinding();
			ModelBindingLogging.MODEL_BINDING_LOGGER.tracef( "Bound entity hierarchy - %s", binding.getEntityName() );
		} );
	}

	private void coordinateGlobalBindings() {
		final GlobalRegistrations globalRegistrations = categorizedDomainModel.getGlobalRegistrations();
		processGenerators( globalRegistrations );
		processConverters( globalRegistrations );
		processJavaTypeRegistrations( globalRegistrations );
		processJdbcTypeRegistrations( globalRegistrations );
		processCustomTypes( globalRegistrations );
		processInstantiators( globalRegistrations );
		processEventListeners( globalRegistrations );
		processFilterDefinitions( globalRegistrations );
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
		globalRegistrations.getSequenceGeneratorRegistrations().values().forEach( (registration) -> {
			bindingState.getMetadataBuildingContext()
					.getMetadataCollector()
					.addIdentifierGenerator( buildSequenceGeneratorDefinition( registration ) );
		} );
		globalRegistrations.getTableGeneratorRegistrations().values().forEach( (registration) -> {
			bindingState.getMetadataBuildingContext()
					.getMetadataCollector()
					.addIdentifierGenerator( buildTableGeneratorDefinition( registration ) );
		} );
		globalRegistrations.getGenericGeneratorRegistrations().values().forEach( (registration) -> {
			bindingState.getMetadataBuildingContext()
					.getMetadataCollector()
					.addIdentifierGenerator( buildGenericGeneratorDefinition( registration ) );
		} );
	}

	private void processConverters(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getConverterRegistrations().forEach( this::processConverter );
	}

	private void processJavaTypeRegistrations(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getJavaTypeRegistrations().forEach( this::processJavaTypeRegistration );
	}

	private void processJdbcTypeRegistrations(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getJdbcTypeRegistrations().forEach( this::processJdbcTypeRegistration );
	}

	private void processCustomTypes(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getUserTypeRegistrations().forEach( this::processUserTypeRegistration );
		globalRegistrations.getCompositeUserTypeRegistrations().forEach( this::processCompositeUserTypeRegistration );
		globalRegistrations.getCollectionTypeRegistrations().forEach( this::processCollectionTypeRegistration );
	}

	private void processInstantiators(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getEmbeddableInstantiatorRegistrations().forEach( this::processEmbeddableInstantiatorRegistration );
	}

	private void processEventListeners(GlobalRegistrations globalRegistrations) {
		// JPA event listeners are consumed by EntityTypeMetadata#getCompleteJpaEventListeners()
		// during entity metadata binding.  There is no separate mapping collector
		// registration to apply here.
	}

	private void processFilterDefinitions(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getFilterDefRegistrations().forEach( (s, filterDefRegistration) -> {
			bindingState.apply( filterDefRegistration );
		} );

	}

	private IdentifierGeneratorDefinition buildSequenceGeneratorDefinition(SequenceGeneratorRegistration registration) {
		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		GeneratorParameters.interpretSequenceGenerator( registration.configuration(), definitionBuilder );
		return definitionBuilder.build();
	}

	private IdentifierGeneratorDefinition buildTableGeneratorDefinition(TableGeneratorRegistration registration) {
		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		GeneratorParameters.interpretTableGenerator( registration.configuration(), definitionBuilder );
		return definitionBuilder.build();
	}

	@SuppressWarnings("removal")
	private IdentifierGeneratorDefinition buildGenericGeneratorDefinition(GenericGeneratorRegistration registration) {
		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		definitionBuilder.setName( registration.name() );
		final Class<? extends Generator> generatorClass = registration.configuration().type();
		final String strategy = generatorClass.equals( Generator.class )
				? registration.configuration().strategy()
				: generatorClass.getName();
		if ( isNotEmpty( strategy ) ) {
			definitionBuilder.setStrategy( strategy );
		}
		definitionBuilder.addParams( AnnotationHelper.extractParameterMap( registration.configuration().parameters() ) );
		return definitionBuilder.build();
	}

	private void processConverter(ConversionRegistration registration) {
		bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.addRegisteredConversion( new RegisteredConversion(
						registration.explicitDomainType() == null ? null : registration.explicitDomainType().toJavaClass(),
						attributeConverterClass( registration.converterType() ),
						registration.autoApply()
				) );
	}

	private void processJavaTypeRegistration(JavaTypeRegistration registration) {
		bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.addJavaTypeRegistration(
						registration.domainType().toJavaClass(),
						instantiate( registration.descriptor(), JavaType.class, "Java type descriptor" )
				);
	}

	private void processJdbcTypeRegistration(JdbcTypeRegistration registration) {
		bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.addJdbcTypeRegistration(
						registration.code(),
						instantiate( registration.descriptor(), JdbcType.class, "JDBC type descriptor" )
				);
	}

	private void processUserTypeRegistration(UserTypeRegistration registration) {
		bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.registerUserType(
						registration.domainClass().toJavaClass(),
						userTypeClass( registration.userTypeClass() )
				);
	}

	private void processCompositeUserTypeRegistration(CompositeUserTypeRegistration registration) {
		bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.registerCompositeUserType(
						registration.embeddableClass().toJavaClass(),
						compositeUserTypeClass( registration.userTypeClass() )
				);
	}

	private void processCollectionTypeRegistration(CollectionTypeRegistration registration) {
		bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.addCollectionTypeRegistration(
						registration.classification(),
						new org.hibernate.boot.spi.InFlightMetadataCollector.CollectionTypeRegistrationDescriptor(
								instantiateClass( registration.userTypeClass(), UserCollectionType.class, "collection user type" ),
								registration.parameterMap()
						)
				);
	}

	private void processEmbeddableInstantiatorRegistration(EmbeddableInstantiatorRegistration registration) {
		bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.registerEmbeddableInstantiator(
						registration.embeddableClass().toJavaClass(),
						instantiateClass( registration.instantiator(), EmbeddableInstantiator.class, "embeddable instantiator" )
				);
	}

	private <T> T instantiate(ClassDetails classDetails, Class<T> expectedType, String registrationRole) {
		final Class<? extends T> javaClass = instantiateClass( classDetails, expectedType, registrationRole );
		try {
			return javaClass.getConstructor().newInstance();
		}
		catch (Exception e) {
			final ModelsException modelsException = new ModelsException(
					"Error instantiating global " + registrationRole + " registration - " + classDetails.getName()
			);
			modelsException.addSuppressed( e );
			throw modelsException;
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Class<? extends T> instantiateClass(ClassDetails classDetails, Class<T> expectedType, String registrationRole) {
		final Class<?> javaClass = classDetails.toJavaClass();
		if ( !expectedType.isAssignableFrom( javaClass ) ) {
			throw new ModelsException(
					"Global " + registrationRole + " registration class `" + classDetails.getName()
							+ "` did not implement " + expectedType.getName()
			);
		}
		return (Class<? extends T>) javaClass;
	}

	@SuppressWarnings("unchecked")
	private Class<? extends UserType<?>> userTypeClass(ClassDetails classDetails) {
		return (Class<? extends UserType<?>>) (Class<?>) instantiateClass( classDetails, UserType.class, "user type" );
	}

	@SuppressWarnings("unchecked")
	private Class<? extends CompositeUserType<?>> compositeUserTypeClass(ClassDetails classDetails) {
		return (Class<? extends CompositeUserType<?>>) (Class<?>) instantiateClass(
				classDetails,
				CompositeUserType.class,
				"composite user type"
		);
	}

	@SuppressWarnings("unchecked")
	private Class<? extends AttributeConverter<?, ?>> attributeConverterClass(ClassDetails classDetails) {
		return (Class<? extends AttributeConverter<?, ?>>) (Class<?>) instantiateClass(
				classDetails,
				AttributeConverter.class,
				"attribute converter"
		);
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
