/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

/**
 * Support for processing mapping XML files, ultimately creating/updating
 * {@linkplain org.hibernate.models.source.spi.AnnotationUsage annotation} references
 * on the model's {@linkplain org.hibernate.models.source.spi.AnnotationTarget targets}
 * based on the XML.
 *
 * @author Steve Ebersole
 */
package org.hibernate.models.orm.xml;