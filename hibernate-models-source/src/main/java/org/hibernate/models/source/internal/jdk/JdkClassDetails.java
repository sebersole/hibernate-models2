/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jdk;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.models.internal.ArrayHelper;
import org.hibernate.models.internal.CollectionHelper;
import org.hibernate.models.source.internal.ClassDetailsSupport;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.ClassDetailsRegistry;
import org.hibernate.models.source.spi.FieldDetails;
import org.hibernate.models.source.spi.MethodDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import static org.hibernate.models.source.internal.jdk.JdkBuilders.buildMethodDetails;

/**
 * ClassDetails implementation based on a {@link Class} reference
 *
 * @author Steve Ebersole
 */
public class JdkClassDetails extends AbstractAnnotationTarget implements ClassDetailsSupport {
	private final String name;
	private final Class<?> managedClass;

	private final ClassDetails superType;
	private List<ClassDetails> interfaces;

	private List<JdkFieldDetails> fields;
	private List<JdkMethodDetails> methods;

	public JdkClassDetails(
			Class<?> managedClass,
			SourceModelBuildingContext buildingContext) {
		this( managedClass.getName(), managedClass, buildingContext );
	}

	public JdkClassDetails(
			String name,
			Class<?> managedClass,
			SourceModelBuildingContext buildingContext) {
		super( managedClass::getAnnotations, buildingContext );
		this.name = name;
		this.managedClass = managedClass;

		final ClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry();

		final Class<?> superclass = managedClass.getSuperclass();
		if ( superclass == null ) {
			superType = null;
		}
		else {
			superType = classDetailsRegistry.resolveClassDetails(
					superclass.getName(),
					() -> JdkBuilders.buildClassDetailsStatic( superclass, buildingContext )
			);
		}

		classDetailsRegistry.addClassDetails( this );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getClassName() {
		return managedClass.getName();
	}

	@Override
	public <X> Class<X> toJavaClass() {
		//noinspection unchecked
		return (Class<X>) managedClass;
	}

	@Override
	public boolean isAbstract() {
		return Modifier.isAbstract( managedClass.getModifiers() );
	}

	@Override
	public ClassDetails getSuperType() {
		return superType;
	}

	@Override
	public List<ClassDetails> getImplementedInterfaceTypes() {
		if ( interfaces == null ) {
			interfaces = collectInterfaces();
		}
		return interfaces;
	}

	private List<ClassDetails> collectInterfaces() {
		final Class<?>[] interfaceClasses = managedClass.getInterfaces();
		if ( ArrayHelper.isEmpty( interfaceClasses ) ) {
			return Collections.emptyList();
		}

		final ArrayList<ClassDetails> result = CollectionHelper.arrayList( interfaceClasses.length );
		for ( int i = 0; i < interfaceClasses.length; i++ ) {
			final Class<?> interfaceClass = interfaceClasses[ i ];
			final ClassDetails interfaceDetails = getBuildingContext().getClassDetailsRegistry().resolveClassDetails(
					interfaceClass.getName(),
					() -> JdkBuilders.buildClassDetailsStatic( interfaceClass, getBuildingContext() )
			);
			result.add( interfaceDetails );
		}
		return result;
	}

	@Override
	public boolean isImplementor(Class<?> checkType) {
		return checkType.isAssignableFrom( managedClass );
	}

	@Override
	public boolean isImplementor(ClassDetails checkType) {
		if ( checkType instanceof JdkClassDetails ) {
			return isImplementor( ( (JdkClassDetails) checkType ).managedClass );
		}
		return ClassDetailsSupport.super.isImplementor( checkType );
	}

	@Override
	public List<FieldDetails> getFields() {
		if ( fields == null ) {
			final Field[] reflectionFields = managedClass.getFields();
			this.fields = CollectionHelper.arrayList( reflectionFields.length );
			for ( int i = 0; i < reflectionFields.length; i++ ) {
				final Field reflectionField = reflectionFields[i];
				fields.add( new JdkFieldDetails( reflectionField, getBuildingContext() ) );
			}
		}
		//noinspection unchecked,rawtypes
		return (List) fields;
	}

	@Override
	public List<MethodDetails> getMethods() {
		if ( methods == null ) {
			final Method[] reflectionMethods = managedClass.getMethods();
			this.methods = CollectionHelper.arrayList( reflectionMethods.length );
			for ( int i = 0; i < reflectionMethods.length; i++ ) {
				this.methods.add( buildMethodDetails( reflectionMethods[i], getBuildingContext() ) );
			}
		}
		//noinspection unchecked,rawtypes
		return (List) methods;
	}

	@Override
	public String toString() {
		return "JdkClassDetails(" + name + ")";
	}
}
