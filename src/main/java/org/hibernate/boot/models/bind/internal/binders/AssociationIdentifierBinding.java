/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import jakarta.persistence.JoinColumn;

public record AssociationIdentifierBinding(
		EntityTypeMetadata ownerType,
		PersistentClass ownerBinding,
		Property property,
		ManyToOne value,
		EntityTypeBinder targetTypeBinder,
		List<JoinColumn> joinColumns,
		ForeignKeySource foreignKeySource,
		List<Column> identifierColumns) {
}
