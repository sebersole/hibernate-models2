/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.resources;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class SimpleEntity {
	@Id
	private Integer id;
	private String name;
}
