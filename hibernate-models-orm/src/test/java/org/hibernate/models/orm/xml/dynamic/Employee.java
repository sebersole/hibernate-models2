/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.xml.dynamic;

import java.util.List;

import org.hibernate.models.orm.xml.SimpleEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Employee {
	@Id
	private String name;
	@Id
	private int number;

	private List<SimpleEntity> oneToMany;
}
