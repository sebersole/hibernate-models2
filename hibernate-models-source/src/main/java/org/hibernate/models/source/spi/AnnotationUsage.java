/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.spi;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Describes the usage of an annotation.  That is, not the
 * {@linkplain AnnotationDescriptor annotation class} itself, but
 * rather a particular usage of the annotation on one of its
 * allowable {@linkplain AnnotationTarget targets}.
 * <p/>
 * The standard way to access values is using {@linkplain #getAttributeValue}.  Convenience
 * methods have been added for the allowable annotation types.  These methods may throw
 * exceptions (generally {@linkplain ClassCastException}, if the expected type does not match).<ul>
 *     <li>{@linkplain #getBoolean}</li>
 *     <li>{@linkplain #getByte}</li>
 *     <li>{@linkplain #getShort}</li>
 *     <li>{@linkplain #getInteger}</li>
 *     <li>{@linkplain #getLong}</li>
 *     <li>{@linkplain #getFloat}</li>
 *     <li>{@linkplain #getDouble}</li>
 *     <li>{@linkplain #getClassDetails}</li>
 *     <li>{@linkplain #getNestedUsage}</li>
 *     <li>{@linkplain #getList}</li>
 * </ul>
 *
 * @apiNote Abstracts the underlying source of the annotation information,
 * whether that is the {@linkplain Annotation annotation} itself, JAXB, Jandex,
 * HCANN, etc.
 *
 * @author Steve Ebersole
 */
public interface AnnotationUsage<A extends Annotation> {
	/**
	 * Type of the used annotation
	 */
	Class<A> getAnnotationType();

	/**
	 * The target where this usage occurs
	 */
	AnnotationTarget getAnnotationTarget();

	/**
	 * The value of the named annotation attribute
	 */
	<V> V getAttributeValue(String name);

	default <V> V getAttributeValue(AttributeDescriptor<V> attributeDescriptor) {
		return getAttributeValue( attributeDescriptor.getName() );
	}

	default String getString(String name) {
		return getAttributeValue( name );
	}

	default Boolean getBoolean(String name) {
		return getAttributeValue( name );
	}

	default Byte getByte(String name) {
		return getAttributeValue( name );
	}

	default Short getShort(String name) {
		return getAttributeValue( name );
	}

	default Integer getInteger(String name) {
		return getAttributeValue( name );
	}

	default Long getLong(String name) {
		return getAttributeValue( name );
	}

	default Float getFloat(String name) {
		return getAttributeValue( name );
	}

	default Double getDouble(String name) {
		return getAttributeValue( name );
	}

	default <E extends Enum<E>> E getEnum(String name) {
		return getAttributeValue( name );
	}

	default ClassDetails getClassDetails(String name) {
		return getAttributeValue( name );
	}

	default <X extends Annotation> AnnotationUsage<X> getNestedUsage(String name) {
		return getAttributeValue( name );
	}

	default <E> List<E> getList(String name) {
		return getAttributeValue( name );
	}
}
