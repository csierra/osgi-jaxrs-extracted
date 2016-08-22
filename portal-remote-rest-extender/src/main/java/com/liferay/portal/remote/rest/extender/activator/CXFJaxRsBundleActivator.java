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

package com.liferay.portal.remote.rest.extender.activator;

import javax.ws.rs.ext.RuntimeDelegate;

import com.liferay.portal.remote.dependency.manager.tccl.TCCLDependencyManager;
import com.liferay.portal.remote.rest.extender.internal.RestExtender;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Carlos Sierra Andrés
 */
public class CXFJaxRsBundleActivator implements BundleActivator {

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Thread thread = Thread.currentThread();

		ClassLoader contextClassLoader = thread.getContextClassLoader();

		ClassLoader classLoader = RuntimeDelegate.class.getClassLoader();

		thread.setContextClassLoader(classLoader);

		try {

			// Initialize instance so it is never looked up again

			RuntimeDelegate.getInstance();
		}
		finally {
			thread.setContextClassLoader(contextClassLoader);
		}

		_manager = new TCCLDependencyManager(bundleContext);

		Component restExtenderComponent =
			_manager.
				createFactoryConfigurationAdapterService(
					"com.liferay.portal.remote.rest.extender.configuration." +
						"RestExtenderConfiguration",
					"update", false).
				setImplementation(RestExtender.class);

		_manager.add(restExtenderComponent);

		restExtenderComponent.start();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_manager.clear();
	}

	private TCCLDependencyManager _manager;
}