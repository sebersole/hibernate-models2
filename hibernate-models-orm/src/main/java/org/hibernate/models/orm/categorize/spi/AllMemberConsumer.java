/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.spi;

import org.hibernate.models.spi.MemberDetails;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface AllMemberConsumer {
	void acceptMember(MemberDetails memberDetails);
}
