/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.models.source.internal.ClassDetailsSupport;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.FieldDetails;
import org.hibernate.models.source.spi.MethodDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

/**
 * @author Steve Ebersole
 */
public class DynamicClassDetails extends AbstractAnnotationTarget implements ClassDetailsSupport {
	private final String name;
	private final String className;
	private final boolean isAbstract;
	private final ClassDetails superType;

	private List<DynamicFieldDetails> fields;
	private List<DynamicMethodDetails> methods;

	private Class<?> javaType;

	public DynamicClassDetails(
			String name,
			String className,
			boolean isAbstract,
			ClassDetails superType,
			SourceModelBuildingContext buildingContext) {
		super( buildingContext );
		this.name = name;
		this.className = className;
		this.isAbstract = isAbstract;
		this.superType = superType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public ClassDetails getSuperType() {
		return superType;
	}

	@Override
	public List<ClassDetails> getImplementedInterfaceTypes() {
		// todo : do we need these for dynamic classes?
		return null;
	}

	@Override
	public List<FieldDetails> getFields() {
		if ( fields == null ) {
			return Collections.emptyList();
		}

		//noinspection unchecked,rawtypes
		return (List) fields;
	}

	public void addField(DynamicFieldDetails fieldDetails) {
		if ( fields == null ) {
			this.fields = new ArrayList<>();
		}
		this.fields.add( fieldDetails );
	}

	@Override
	public List<MethodDetails> getMethods() {
		if ( methods == null ) {
			return Collections.emptyList();
		}

		//noinspection unchecked,rawtypes
		return (List) methods;
	}

	public void addMethod(DynamicMethodDetails methodDetails) {
		if ( methods == null ) {
			this.methods = new ArrayList<>();
		}
		this.methods.add( methodDetails );
	}

	@Override
	public <X> Class<X> toJavaClass() {
		if ( javaType == null ) {
			if ( className != null ) {
				javaType = getBuildingContext().getClassLoadingAccess().classForName( className );
			}
		}
		//noinspection unchecked
		return (Class<X>) javaType;
	}
}
