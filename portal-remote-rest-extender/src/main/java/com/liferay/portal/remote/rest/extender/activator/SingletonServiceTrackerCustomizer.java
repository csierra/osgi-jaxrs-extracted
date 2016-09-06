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
import org.apache.cxf.Bus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * @author Carlos Sierra Andrés
 */
class SingletonServiceTrackerCustomizer
	implements ServiceTrackerCustomizer
		<Object, SingletonServiceTrackerCustomizer.Tracked> {

	private BundleContext _bundleContext;
	private Bus _bus;

	public SingletonServiceTrackerCustomizer(
		BundleContext bundleContext, Bus bus) {

		_bundleContext = bundleContext;
		_bus = bus;
	}

	@Override
	public Tracked addingService(
		ServiceReference<Object> serviceReference) {

		final Object service = _bundleContext.getService(
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
				serviceReference.getProperty("osgi.jaxrs.resource.base").
					toString());

			CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator =
				new CXFJaxRsServiceRegistrator(properties);

			cxfJaxRsServiceRegistrator.addBus(_bus);
			cxfJaxRsServiceRegistrator.addApplication(new Application() {
				@Override
				public Set<Object> getSingletons() {
					return Collections.singleton(service);
				}
			});

			return new Tracked(
				cxfJaxRsServiceRegistrator, service,
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
		ServiceReference<Object> serviceReference, Tracked tracked) {

		removedService(serviceReference, tracked);

		addingService(serviceReference);
	}

	@Override
	public void removedService(
		ServiceReference<Object> reference, Tracked tracked) {

		_bundleContext.ungetService(reference);

		Object service = tracked.getService();

		CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator =
			tracked.getCxfJaxRsServiceRegistrator();

		cxfJaxRsServiceRegistrator.removeService(service);

		cxfJaxRsServiceRegistrator.removeBus(_bus);

		tracked.getCxfJaxRsServiceRegistratorServiceRegistration().unregister();
	}

	public static class Tracked {

		private final CXFJaxRsServiceRegistrator _cxfJaxRsServiceRegistrator;
		private final Object _service;
		private final ServiceRegistration<CXFJaxRsServiceRegistrator>
			_cxfJaxRsServiceRegistratorServiceRegistration;

		public Object getService() {
			return _service;
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
			Object service,
			ServiceRegistration<CXFJaxRsServiceRegistrator>
				cxfJaxRsServiceRegistratorServiceRegistration) {

			_cxfJaxRsServiceRegistrator = cxfJaxRsServiceRegistrator;
			_service = service;
			_cxfJaxRsServiceRegistratorServiceRegistration =
				cxfJaxRsServiceRegistratorServiceRegistration;
		}

	}

}


