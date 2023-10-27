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
 * based on the XML.<ol>
 *     <li>
 *         First performs some {@linkplain org.hibernate.models.orm.xml.spi.XmlPreProcessor pre-processing}
 *         which aggregates information across all XML mappings
 *     </li>
 *     <li>
 *         Next performs XML {@linkplain org.hibernate.models.orm.xml.spi.XmlProcessor processing} which
 *         applies metadata-complete mappings and collects overlay/override XML for later application.
 *     </li>
 *     <li>
 *         Performs XML {@linkplain org.hibernate.models.orm.xml.spi.XmlProcessingResult post-processing} which
 *         applies overlay/override XML.
 *     </li>
 * </ol>
 * <p/>
 * First step is some {@linkplain }
 *
 * @author Steve Ebersole
 */
package org.hibernate.models.orm.xml;