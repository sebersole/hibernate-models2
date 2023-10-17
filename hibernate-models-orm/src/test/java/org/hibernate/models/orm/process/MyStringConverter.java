/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.process;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter(autoApply = true)
public class MyStringConverter implements AttributeConverter<String, String> {
	@Override
	public String convertToDatabaseColumn(String attribute) {
		return null;
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		return null;
	}
}
