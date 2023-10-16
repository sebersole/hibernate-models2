/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.source.internal.jdk;

import java.lang.reflect.Field;

import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.FieldDetails;
import org.hibernate.models.source.spi.SourceModelBuildingContext;

import static org.hibernate.models.source.internal.ModifierUtils.isPersistableField;


/**
 * @author Steve Ebersole
 */
public class JdkFieldDetails extends AbstractAnnotationTarget implements FieldDetails {
	private final Field field;
	private final ClassDetails type;

	public JdkFieldDetails(Field field, SourceModelBuildingContext buildingContext) {
		super( field::getAnnotations, buildingContext );
		this.field = field;
		this.type = buildingContext.getClassDetailsRegistry().resolveClassDetails(
				field.getType().getName(),
				() -> JdkBuilders.buildClassDetailsStatic( field.getType(), getBuildingContext() )
		);
	}

	@Override
	public String getName() {
		return field.getName();
	}

	@Override
	public ClassDetails getType() {
		return type;
	}

	@Override
	public boolean isPersistable() {
		return isPersistableField( field.getModifiers() );
	}
}
