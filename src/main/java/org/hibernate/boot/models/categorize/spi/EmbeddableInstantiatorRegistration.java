/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableInstantiatorRegistrationImpl;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.ClassDetails;

/// Global registration for an {@linkplain EmbeddableInstantiator embeddable instantiator}.
///
/// @param embeddableClass The embeddable class handled by the instantiator
/// @param instantiator The instantiator class
///
/// @author Steve Ebersole
/// @see org.hibernate.annotations.EmbeddableInstantiatorRegistration
/// @see JaxbEmbeddableInstantiatorRegistrationImpl
public record EmbeddableInstantiatorRegistration(ClassDetails embeddableClass, ClassDetails instantiator) {
}
