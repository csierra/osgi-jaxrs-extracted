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

import com.liferay.portal.remote.rest.extender.internal.BusServiceTrackerCustomizer;
import com.liferay.portal.remote.rest.extender.internal.ServicesServiceTrackerCustomizer;
import org.apache.cxf.Bus;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Carlos Sierra Andrés
 */
public class CXFJaxRsBundleActivator implements BundleActivator {

	private ServiceTracker<?, ?> _busServiceTracker;
	private ServiceTracker<?, ?> _singletonsTracker;

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

		_busServiceTracker = new ServiceTracker<>(
			bundleContext, Bus.class,
			new BusServiceTrackerCustomizer(bundleContext));

		_busServiceTracker.open();

		Filter filter = bundleContext.createFilter(
			"(jaxrs.application.select=*)");

		_singletonsTracker = new ServiceTracker<>(
			bundleContext, filter,
			new ServicesServiceTrackerCustomizer(bundleContext));

		_singletonsTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_busServiceTracker.close();

		_singletonsTracker.close();
	}

}