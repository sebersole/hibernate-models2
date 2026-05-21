/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

/**
 * Categorization-facing facade for processing XML mapping files.
 * <p>
 * This package intentionally keeps a narrow local contract and delegates the XML
 * processing internals to {@code org.hibernate.boot.models.xml} in upstream ORM.
 * <ol>
 *     <li>
 *         First performs {@linkplain org.hibernate.boot.models.source.xml.AvailableXmlMappingsPreProcessor pre-processing}
 *         which aggregates information across all XML mappings
 *     </li>
 *     <li>
 *         Next performs XML {@linkplain org.hibernate.boot.models.source.xml.AvailableXmlMappingsProcessor processing} which
 *         applies metadata-complete mappings and collects overlay/override XML for later application.
 *     </li>
 *     <li>
 *         Finally applies the deferred {@linkplain org.hibernate.boot.models.source.xml.XmlProcessingResult XML result},
 *         applying overlay/override XML through an upstream-backed adapter.
 *     </li>
 * </ol>
 *
 * @author Steve Ebersole
 */
package org.hibernate.boot.models.source.xml;
