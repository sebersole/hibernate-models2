/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.dynamic;

import org.hibernate.models.source.internal.MutableMemberDetails;
import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.MethodDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

/**
 * @author Steve Ebersole
 */
public class DynamicMethodDetails extends AbstractAnnotationTarget implements MethodDetails, MutableMemberDetails {
	private final String name;
	private final ClassDetails type;
	private final MethodKind methodKind;
	private final boolean isPersistable;

	public DynamicMethodDetails(
			String name,
			ClassDetails type,
			MethodKind methodKind,
			boolean isPersistable,
			SourceModelBuildingContext buildingContext) {
		super( buildingContext );
		this.name = name;
		this.type = type;
		this.methodKind = methodKind;
		this.isPersistable = isPersistable;
	}

	@Override
	public String getName() {
		return name;
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
		return isPersistable;
	}
}
