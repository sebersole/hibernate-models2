/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.source.xml;

/**
 * Categorization-facing result of XML processing.
 * <p>
 * Applying the result performs the deferred XML overlay/override work collected
 * during XML processing.
 *
 * @author Steve Ebersole
 */
public interface XmlProcessingResult {
	void apply();
}
