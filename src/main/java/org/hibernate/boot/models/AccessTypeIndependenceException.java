/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.AccessType;

/// Indicates that a defaulted embeddable or mapped superclass was used in differing
/// access contexts but did not expose an access-independent persistent attribute shape.
///
/// @author Steve Ebersole
public class AccessTypeIndependenceException extends MappingException {
	public AccessTypeIndependenceException(
			ClassDetails managedClass,
			AccessType accessType,
			String expectedShape,
			String actualShape) {
		super(
				String.format(
						Locale.ROOT,
						"Defaulted access type for `%s` is not access-independent when used with `%s` access; "
								+ "expected persistent attributes [%s], but found [%s] - see Jakarta Persistence 2.3.4",
						managedClass.getName(),
						accessType,
						expectedShape,
						actualShape
				)
		);
	}
}
