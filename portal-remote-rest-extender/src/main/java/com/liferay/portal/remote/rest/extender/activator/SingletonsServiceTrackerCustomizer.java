/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.remote.rest.extender.activator;

import com.liferay.portal.remote.rest.extender.internal.CXFJaxRsServiceRegistrator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.ws.rs.ext.Provider;

/**
 * @author Carlos Sierra Andrés
 */
public class SingletonsServiceTrackerCustomizer
	implements ServiceTrackerCustomizer<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator> {

	private final BundleContext _bundleContext;
	private final ClassLoader _classLoader;
	private final Class<?> _serviceClass;
	private final Object _service;

	public SingletonsServiceTrackerCustomizer(
		BundleContext bundleContext, ClassLoader classLoader,
		Object service) {

		_bundleContext = bundleContext;
		_classLoader = classLoader;
		_service = service;

		_serviceClass = service.getClass();
	}

	@Override
	public CXFJaxRsServiceRegistrator addingService(
		ServiceReference<CXFJaxRsServiceRegistrator> reference) {

		Thread thread = Thread.currentThread();

		ClassLoader contextClassLoader =
			thread.getContextClassLoader();

		CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator =
			_bundleContext.getService(reference);

		try {
			thread.setContextClassLoader(_classLoader);

			if (_serviceClass.isAnnotationPresent(Provider.class)) {
				cxfJaxRsServiceRegistrator.addProvider(_service);
			} else {
				cxfJaxRsServiceRegistrator.addService(_service);
			}

			return cxfJaxRsServiceRegistrator;
		}
		catch (Exception e) {
			_bundleContext.ungetService(reference);

			throw e;
		}
		finally {
			thread.setContextClassLoader(contextClassLoader);
		}
	}

	@Override
	public void modifiedService(
		ServiceReference<CXFJaxRsServiceRegistrator> reference,
		CXFJaxRsServiceRegistrator registrator) {

		removedService(reference, registrator);

		addingService(reference);
	}

	@Override
	public void removedService(
		ServiceReference<CXFJaxRsServiceRegistrator> reference,
		CXFJaxRsServiceRegistrator registrator) {

		if (_serviceClass.isAnnotationPresent(Provider.class)) {
			registrator.removeProvider(_service);
		} else {
			registrator.removeService(_service);
		}

		_bundleContext.ungetService(reference);
	}
}
