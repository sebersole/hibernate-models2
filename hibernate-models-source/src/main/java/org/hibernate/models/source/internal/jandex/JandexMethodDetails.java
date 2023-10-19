/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jandex;

import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.MethodDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.MethodInfo;

import static org.hibernate.models.source.internal.ModifierUtils.isPersistableMethod;

/**
 * @author Steve Ebersole
 */
public class JandexMethodDetails extends AbstractAnnotationTarget implements MethodDetails, MutableMemberDetails {
	private final MethodInfo methodInfo;
	private final MethodKind methodKind;
	private final ClassDetails type;

	public JandexMethodDetails(
			MethodInfo methodInfo,
			MethodKind methodKind,
			ClassDetails type,
			SourceModelBuildingContext buildingContext) {
		super( buildingContext );
		this.methodInfo = methodInfo;
		this.methodKind = methodKind;
		this.type = type;
	}

	@Override
	protected AnnotationTarget getJandexAnnotationTarget() {
		return methodInfo;
	}

	@Override
	public String getName() {
		return methodInfo.name();
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
		if ( methodInfo.parametersCount() > 0 ) {
			return false;
		}

		if ( "void".equals( type.getName() ) || "Void".equals( type.getName() ) ) {
			return false;
		}

		return isPersistableMethod( methodInfo.flags() );
	}
}
