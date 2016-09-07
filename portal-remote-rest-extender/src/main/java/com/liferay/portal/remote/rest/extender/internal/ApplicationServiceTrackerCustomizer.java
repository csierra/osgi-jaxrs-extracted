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

package com.liferay.portal.remote.rest.extender.internal;

import org.apache.cxf.Bus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.ws.rs.core.Application;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author Carlos Sierra Andrés
 */
class ApplicationServiceTrackerCustomizer
	implements ServiceTrackerCustomizer
		<Application, ApplicationServiceTrackerCustomizer.Tracked> {

	private BundleContext _bundleContext;
	private Bus _bus;

	public ApplicationServiceTrackerCustomizer(
		BundleContext bundleContext, Bus bus) {

		_bundleContext = bundleContext;
		_bus = bus;
	}

	@Override
	public Tracked addingService(
		ServiceReference<Application> serviceReference) {

		Application application = _bundleContext.getService(
			serviceReference);

		try {
			String[] propertyKeys = serviceReference.getPropertyKeys();

			Map<String, Object> properties = new HashMap<>(
				propertyKeys.length);

			for (String propertyKey : propertyKeys) {
				properties.put(
					propertyKey, serviceReference.getProperty(propertyKey));
			}

			properties.put(
				"CXF_ENDPOINT_ADDRESS",
				serviceReference.getProperty("osgi.jaxrs.application.base").
					toString());

			CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator =
				new CXFJaxRsServiceRegistrator(_bus, application, properties);

			return new Tracked(
				cxfJaxRsServiceRegistrator, application,
				_bundleContext.registerService(
					CXFJaxRsServiceRegistrator.class,
					cxfJaxRsServiceRegistrator, new Hashtable<>(properties)));
		}
		catch (Exception e) {
			_bundleContext.ungetService(serviceReference);

			throw e;
		}
	}

	@Override
	public void modifiedService(
		ServiceReference<Application> serviceReference, Tracked tracked) {

		removedService(serviceReference, tracked);

		addingService(serviceReference);
	}

	@Override
	public void removedService(
		ServiceReference<Application> reference, Tracked tracked) {

		_bundleContext.ungetService(reference);

		tracked.getCxfJaxRsServiceRegistrator().close();

		tracked.getCxfJaxRsServiceRegistratorServiceRegistration().unregister();
	}

	public static class Tracked {

		private final CXFJaxRsServiceRegistrator _cxfJaxRsServiceRegistrator;
		private final Application _application;
		private final ServiceRegistration<CXFJaxRsServiceRegistrator>
			_cxfJaxRsServiceRegistratorServiceRegistration;

		public Application getApplication() {
			return _application;
		}

		public CXFJaxRsServiceRegistrator getCxfJaxRsServiceRegistrator() {
			return _cxfJaxRsServiceRegistrator;
		}

		public ServiceRegistration<CXFJaxRsServiceRegistrator>
			getCxfJaxRsServiceRegistratorServiceRegistration() {

			return _cxfJaxRsServiceRegistratorServiceRegistration;
		}

		public Tracked(
			CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator,
			Application application,
			ServiceRegistration<CXFJaxRsServiceRegistrator>
				cxfJaxRsServiceRegistratorServiceRegistration) {

			_cxfJaxRsServiceRegistrator = cxfJaxRsServiceRegistrator;
			_application = application;
			_cxfJaxRsServiceRegistratorServiceRegistration =
				cxfJaxRsServiceRegistratorServiceRegistration;
		}

	}
}


