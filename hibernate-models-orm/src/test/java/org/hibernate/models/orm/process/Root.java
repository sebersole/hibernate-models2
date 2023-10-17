/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.process;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Root")
@Table(name = "roots")
@Inheritance(strategy = InheritanceType.JOINED)
public class Root {
	@Id
	private Integer id;
	private String name;
}
