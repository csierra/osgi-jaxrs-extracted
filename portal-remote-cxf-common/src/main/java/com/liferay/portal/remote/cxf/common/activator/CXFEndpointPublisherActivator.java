/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.remote.cxf.common.activator;

import com.liferay.portal.remote.cxf.common.CXFEndpointPublisher;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;

/**
 * @author Carlos Sierra Andrés
 */
public class CXFEndpointPublisherActivator extends DependencyActivatorBase {

	@Override
	public void init(BundleContext context, DependencyManager manager)
		throws Exception {

		Component component = manager.
			createFactoryConfigurationAdapterService(
				"com.liferay.portal.remote.cxf.common.configuration." +
					"CXFEndpointPublisherConfiguration",
				"update", false).
			setImplementation(CXFEndpointPublisher.class);

		manager.add(component);

		component.start();
	}

}
