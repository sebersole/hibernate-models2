/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */

/// Support for interpreting ORM mapping sources through Hibernate Models and
/// binding the result into Hibernate's boot-time mapping model.
///
/// The module works in three broad phases:
/// <ol>
///     <li>
///         Gather available managed classes, packages, and XML mappings as
///         {@link org.hibernate.boot.models.source.AvailableResources}.
///     </li>
///     <li>
///         Categorize those sources into a
///         {@link org.hibernate.boot.models.categorize.spi.CategorizedDomainModel}.
///     </li>
///     <li>
///         Bind the categorized model into Hibernate's {@code org.hibernate.mapping}
///         boot-time mapping model.
///     </li>
/// </ol>
///
/// @author Steve Ebersole
package org.hibernate.boot.models;
