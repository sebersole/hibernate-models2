/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.categorize.spi;

import java.util.List;

import org.hibernate.models.source.spi.ClassDetails;
import org.hibernate.models.source.spi.MemberDetails;

import jakarta.persistence.AccessType;

/**
 * Contract responsible for resolving the members that identify the persistent
 * attributes for a given class descriptor representing a managed type.
 * <p/>
 * These members (field or method) would be where we look for mapping annotations
 * for the attribute.
 * <p/>
 * Additionally, whether the member is a field or method would tell us the default
 * runtime {@linkplain org.hibernate.property.access.spi.PropertyAccessStrategy access strategy}
 *
 * @author Steve Ebersole
 */
public interface PersistentAttributeMemberResolver {
	/**
	 * Given the class descriptor representing a ManagedType and the implicit AccessType
	 * to use, resolve the members that indicate persistent attributes.
	 *
	 * @param classDetails Descriptor of the class
	 * @param classLevelAccessType The implicit AccessType
	 * @param allMemberConsumer Optional callback for each member on the class
	 * @param buildingContext The local context
	 *
	 * @return The list of "backing members"
	 */
	List<MemberDetails> resolveAttributesMembers(
			ClassDetails classDetails,
			AccessType classLevelAccessType,
			AllMemberConsumer allMemberConsumer,
			ModelCategorizationContext buildingContext);

}
