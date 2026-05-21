/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm.process;

import org.hibernate.models.orm.SourceModelTestHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class ProcessorSmokeTests {
	@Test
	void simpleTest() {
		final ModelsContext buildingContext = SourceModelTestHelper.createBuildingContext(
				Person.class,
				MyStringConverter.class,
				MyUuidConverter.class
		);
		final ClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry();
		final ClassDetails personClassDetails = classDetailsRegistry.findClassDetails( Person.class.getName() );
		assertThat( personClassDetails ).isNotNull();
	}
}
