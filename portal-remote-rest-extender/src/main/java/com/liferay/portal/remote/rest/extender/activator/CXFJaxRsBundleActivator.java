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

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import com.liferay.portal.remote.rest.extender.internal.CXFJaxRsServiceRegistrator;
import org.apache.cxf.Bus;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Carlos Sierra Andrés
 */
public class CXFJaxRsBundleActivator implements BundleActivator {

	private ServiceTracker<Object, ServiceTracker<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator>> _singletonsTracker;
	private ServiceTracker<Bus, ServiceTracker<Application, ApplicationServiceTrackerCustomizer.Tracked>> _busServiceTracker;

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
		_singletonsTracker.close();

		_busServiceTracker.close();
	}

	private static class ServicesServiceTrackerCustomizer
		implements ServiceTrackerCustomizer
			<Object, ServiceTracker
				<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator>> {

		private final BundleContext _bundleContext;

		public ServicesServiceTrackerCustomizer(BundleContext bundleContext) {
			_bundleContext = bundleContext;
		}

		@Override
		public ServiceTracker
			<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator>
				addingService(ServiceReference<Object> reference) {

			String applicationSelector =
				reference.getProperty("jaxrs.application.select").toString();

			Bundle bundle = reference.getBundle();

			BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

			ClassLoader classLoader = bundleWiring.getClassLoader();

			Object service = _bundleContext.getService(reference);

			try {
				Filter filter = _bundleContext.createFilter(
					"(&(objectClass=" + CXFJaxRsServiceRegistrator.class.getName() + ")" +
						applicationSelector + ")");

				ServiceTracker
					<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator>
						serviceTracker = new ServiceTracker<>(
							_bundleContext, filter,
							new SingletonsServiceTrackerCustomizer(
								_bundleContext, classLoader,
								service));

				serviceTracker.open();

				return serviceTracker;
			}
			catch (InvalidSyntaxException ise) {
				_bundleContext.ungetService(reference);

				throw new RuntimeException(ise);
			}
		}

		@Override
		public void modifiedService(
			ServiceReference<Object> reference,
			ServiceTracker
				<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator>
					serviceTracker) {

			removedService(reference, serviceTracker);

			addingService(reference);
		}

		@Override
		public void removedService(
			ServiceReference<Object> reference,
			ServiceTracker
				<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator>
				serviceTracker) {

			serviceTracker.close();

			_bundleContext.ungetService(reference);
		}

	}
}