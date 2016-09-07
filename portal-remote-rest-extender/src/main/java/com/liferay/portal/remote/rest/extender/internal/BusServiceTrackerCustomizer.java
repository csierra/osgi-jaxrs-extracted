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
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Carlos Sierra Andrés
 */
public class BusServiceTrackerCustomizer
	implements ServiceTrackerCustomizer<Bus, Collection<ServiceTracker<?, ?>>> {

	private BundleContext _bundleContext;

	public BusServiceTrackerCustomizer(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	@Override
	public Collection<ServiceTracker<?, ?>>
	addingService(ServiceReference<Bus> serviceReference) {

		Bus bus = _bundleContext.getService(serviceReference);

		try {
			ServiceTracker<Application,?> applicationTracker =
				new ServiceTracker<>(_bundleContext, getApplicationFilter(),
					new ApplicationServiceTrackerCustomizer(
						_bundleContext, bus));

			applicationTracker.open();

			ServiceTracker<Object, ?> singletonsServiceTracker =
				new ServiceTracker<>(_bundleContext, getSingletonsFilter(),
					new SingletonServiceTrackerCustomizer(_bundleContext, bus));

			singletonsServiceTracker.open();

			return Arrays.asList(applicationTracker, singletonsServiceTracker);
		}
		catch (InvalidSyntaxException ise) {
			throw new RuntimeException(ise);
		}
		catch (Exception e) {
			_bundleContext.ungetService(serviceReference);

			throw e;
		}
	}

	private Filter getApplicationFilter() throws InvalidSyntaxException {
		return _bundleContext.createFilter(
			"(&(objectClass=" + Application.class.getName() + ")" +
				"(osgi.jaxrs.application.base=*))");
	}

	private Filter getSingletonsFilter() throws InvalidSyntaxException {
		return _bundleContext.createFilter("(osgi.jaxrs.resource.base=*)");
	}

	@Override
	public void modifiedService(
		ServiceReference<Bus> reference,
		Collection<ServiceTracker<?, ?>> serviceTrackers) {

		removedService(reference, serviceTrackers);

		addingService(reference);
	}

	@Override
	public void removedService(
		ServiceReference<Bus> serviceReference,
		Collection<ServiceTracker<?, ?>> serviceTrackers) {

		_bundleContext.ungetService(serviceReference);

		for (ServiceTracker<?, ?> serviceTracker : serviceTrackers) {
			serviceTracker.close();
		}
	}

}
