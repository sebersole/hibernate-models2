/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.models.orm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.models.xml.XmlResourceException;

import static org.hibernate.boot.jaxb.internal.MappingBinder.NON_VALIDATING;

/**
 * @author Steve Ebersole
 */
public class XmlHelper {
	public static Binding<JaxbEntityMappingsImpl> loadMapping(String resourceName) {
		final ResourceStreamLocatorImpl resourceStreamLocator = new ResourceStreamLocatorImpl();
		final MappingBinder mappingBinder = new MappingBinder( resourceStreamLocator, NON_VALIDATING );
		return mappingBinder.bind(
				resourceStreamLocator.locateResourceStream( resourceName ),
				new Origin( SourceType.RESOURCE, resourceName )
		);
	}

	private static class ResourceStreamLocatorImpl implements ResourceStreamLocator {
		@Override
		public InputStream locateResourceStream(String resourceName) {
			final URL resource = XmlHelper.class.getClassLoader().getResource( resourceName );
			if ( resource == null ) {
				throw new XmlResourceException( "Could not locate XML mapping resource - " + resourceName );
			}
			try {
				return resource.openStream();
			}
			catch (IOException e) {
				throw new XmlResourceException( "Could not open XML mapping resource stream - " + resourceName, e );
			}
		}
	}
}
