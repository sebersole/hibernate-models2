/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jdk;

import java.lang.reflect.Method;

import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.MethodDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import static org.hibernate.models.source.internal.ModifierUtils.isPersistableMethod;

/**
 * @author Steve Ebersole
 */
public class JdkMethodDetails extends AbstractAnnotationTarget implements MethodDetails, MutableMemberDetails {
	private final Method method;
	private final MethodKind methodKind;
	private final ClassDetails type;

	public JdkMethodDetails(
			Method method,
			MethodKind methodKind,
			ClassDetails type,
			SourceModelBuildingContext buildingContext) {
		super( method::getAnnotations, buildingContext );
		this.method = method;
		this.methodKind = methodKind;
		this.type = type;
	}

	@Override
	public String getName() {
		return method.getName();
	}

	@Override
	public MethodKind getMethodKind() {
		return methodKind;
	}

	@Override
	public ClassDetails getType() {
		return type;
	}

	@Override
	public boolean isPersistable() {
		if ( method.getParameterCount() > 0 ) {
			// should be the getter
			return false;
		}

		if ( "void".equals( type.getName() ) || "Void".equals( type.getName() ) ) {
			// again, should be the getter
			return false;
		}

		return isPersistableMethod( method.getModifiers() );
	}
}
