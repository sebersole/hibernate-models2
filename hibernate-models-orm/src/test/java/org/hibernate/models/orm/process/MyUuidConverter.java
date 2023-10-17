/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.process;

import java.util.UUID;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author Steve Ebersole
 */
@Converter
public class MyUuidConverter implements AttributeConverter<UUID, String> {
	@Override
	public String convertToDatabaseColumn(UUID attribute) {
		return null;
	}

	@Override
	public UUID convertToEntityAttribute(String dbData) {
		return null;
	}
}
