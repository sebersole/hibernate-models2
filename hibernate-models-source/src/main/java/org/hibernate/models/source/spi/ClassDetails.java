/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.spi;

import java.util.List;
import java.util.function.Predicate;

import org.hibernate.models.internal.IndexedConsumer;
import org.hibernate.models.source.internal.ClassDetailsHelper;

/**
 * Abstraction for what Hibernate understands about a "class", generally before it has access to
 * the actual {@link Class} reference, if there is a {@code Class} at all (dynamic models).
 *
 * @see ClassDetailsRegistry
 *
 * @author Steve Ebersole
 */
public interface ClassDetails extends AnnotationTarget {
	/**
	 * The name of the class.
	 * <p/>
	 * Generally this is the same as the {@linkplain #getClassName() class name}.
	 * But in the case of Hibernate's {@code entity-name} feature, this would
	 * be the {@code entity-name}
	 */
	String getName();

	/**
	 * The name of the {@link Class}, or {@code null} for dynamic models.
	 *
	 * @apiNote Will be {@code null} for dynamic models
	 */
	String getClassName();

	@Override
	default Kind getKind() {
		return Kind.CLASS;
	}

	/**
	 * Whether the class should be considered abstract.
	 */
	boolean isAbstract();

	/**
	 * Details for the class that is the super type for this class.
	 */
	ClassDetails getSuperType();

	/**
	 * Details for the interfaces this class implements.
	 */
	List<ClassDetails> getImplementedInterfaceTypes();

	/**
	 * Whether the described class is an implementor of the given {@code checkType}.
	 */
	default boolean isImplementor(Class<?> checkType) {
		return ClassDetailsHelper.isImplementor( checkType, this );
	}

	/**
	 * Whether the described class is an implementor of the given {@code checkType}.
	 */
	default boolean isImplementor(ClassDetails checkType) {
		return ClassDetailsHelper.isImplementor( checkType, this );
	}

	/**
	 * Get the fields for this class
	 */
	List<FieldDetails> getFields();

	/**
	 * Visit each field
	 */
	void forEachField(IndexedConsumer<FieldDetails> consumer);

	/**
	 * Find a field by check
	 */
	default FieldDetails findField(Predicate<FieldDetails> check) {
		final List<FieldDetails> fields = getFields();
		for ( int i = 0; i < fields.size(); i++ ) {
			final FieldDetails fieldDetails = fields.get( i );
			if ( check.test( fieldDetails ) ) {
				return fieldDetails;
			}
		}
		return null;
	}

	/**
	 * Find a field by name
	 */
	default FieldDetails findFieldByName(String name) {
		assert name != null;
		return findField( fieldDetails -> name.equals( fieldDetails.getName() ) );
	}

	/**
	 * Get the methods for this class
	 */
	List<MethodDetails> getMethods();

	/**
	 * Find a method by check
	 */
	default MethodDetails findMethod(Predicate<MethodDetails> check) {
		final List<MethodDetails> methods = getMethods();
		for ( int i = 0; i < methods.size(); i++ ) {
			final MethodDetails methodDetails = methods.get( i );
			if ( check.test( methodDetails ) ) {
				return methodDetails;
			}
		}
		return null;
	}

	/**
	 * Find a method by name
	 */
	default MethodDetails findMethodByName(String name) {
		assert name != null;
		return findMethod( methodDetails -> name.equals( methodDetails.getName() ) );
	}

	/**
	 * Visit each method
	 */
	void forEachMethod(IndexedConsumer<MethodDetails> consumer);

	/**
	 * Know what you are doing before calling this method
	 */
	<X> Class<X> toJavaClass();
}
