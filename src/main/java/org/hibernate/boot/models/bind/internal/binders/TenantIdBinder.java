/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import org.hibernate.MappingException;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.singletonMap;
import static org.hibernate.boot.models.bind.internal.binders.AttributeBinder.processColumn;

/// @author Steve Ebersole
public class TenantIdBinder {
	public static final String FILTER_NAME = "_tenantId";
	public static final String PARAMETER_NAME = "tenantId";

	public static void bindTenantId(
			AttributeMetadata attributeMetadata,
			EntityTypeMetadata managedType,
			RootClass typeBinding,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final InFlightMetadataCollector collector = bindingState.getMetadataBuildingContext().getMetadataCollector();
		final TypeConfiguration typeConfiguration = collector.getTypeConfiguration();

		final MemberDetails memberDetails = attributeMetadata.getMember();
		final String returnedClassName = memberDetails.getType().determineRawClass().getClassName();
		final BasicType<?> tenantIdType = typeConfiguration
				.getBasicTypeRegistry()
				.getRegisteredType( returnedClassName );

		final FilterDefinition filterDefinition = collector.getFilterDefinition( FILTER_NAME );
		if ( filterDefinition == null ) {
			collector.addFilterDefinition( new FilterDefinition(
					FILTER_NAME,
					"",
					singletonMap( PARAMETER_NAME, tenantIdType )
			) );
		}
		else {
			final JavaType<?> tenantIdTypeJtd = tenantIdType.getJavaTypeDescriptor();
			final JavaType<?> parameterJtd = filterDefinition
					.getParameterJdbcMapping( PARAMETER_NAME )
					.getJavaTypeDescriptor();
			if ( !parameterJtd.getJavaTypeClass().equals( tenantIdTypeJtd.getJavaTypeClass() ) ) {
				throw new MappingException(
						"all @TenantId fields must have the same type: "
								+ parameterJtd.getJavaType().getTypeName()
								+ " differs from "
								+ tenantIdTypeJtd.getJavaType().getTypeName()
				);
			}
		}

		final Property property = new Property();
		typeBinding.addProperty( property );
		property.setName( attributeMetadata.getName() );

		final BasicValue basicValue = new BasicValue( bindingState.getMetadataBuildingContext(), typeBinding.getRootTable() );
		property.setValue( basicValue );

		processColumn(
				memberDetails,
				property,
				basicValue,
				typeBinding.getRootTable(),
				bindingOptions,
				bindingState,
				bindingContext
		);
		BasicValueBinder.bindBasicValue(
				org.hibernate.boot.models.bind.internal.sources.BasicValueSource.attribute( memberDetails ),
				property,
				basicValue,
				bindingOptions,
				bindingState,
				bindingContext
		);

		property.resetUpdateable( false );
		property.resetOptional( false );
	}
}
