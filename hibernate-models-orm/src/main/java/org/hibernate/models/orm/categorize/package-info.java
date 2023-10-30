/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

/**
 * Support for processing an application's domain model, as known through
 * {@linkplain org.hibernate.boot.model.process.spi.ManagedResources} and ultimately
 * producing a mildly {@linkplain org.hibernate.models.orm.categorize.spi.CategorizedDomainModel categorized model}
 * representing entities, embeddables, etc.
 * <p/>
 * Happens in 2 steps -<ol>
 *     <li>
 *         Create the "source metamodel" ({@linkplain org.hibernate.models.source.spi.ClassDetails classes},
 *         {@linkplain org.hibernate.models.source.spi.AnnotationUsage annotations},
 *         {@linkplain org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl XML}, etc.)
 *     </li>
 *     <li>
 *         Process this "source metamodel" and produce the {@linkplain org.hibernate.models.orm.categorize.spi.CategorizedDomainModel categorized model}
 *     </li>
 * </ol>
 *
 * @author Steve Ebersole
 */
package org.hibernate.models.orm.categorize;
