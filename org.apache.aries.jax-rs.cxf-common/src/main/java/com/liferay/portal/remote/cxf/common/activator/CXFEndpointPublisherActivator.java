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

import com.liferay.portal.remote.cxf.common.ServicesRegistrator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.HashMap;

/**
 * @author Carlos Sierra Andrés
 */
public class CXFEndpointPublisherActivator implements BundleActivator {

	private ServicesRegistrator _servicesRegistrator;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		HashMap<String, Object> properties = new HashMap<String, Object>() {{
			put("contextPath", "/cxf");
		}};

		_servicesRegistrator = new ServicesRegistrator(
			bundleContext, properties);

		_servicesRegistrator.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_servicesRegistrator.stop();
	}
}

