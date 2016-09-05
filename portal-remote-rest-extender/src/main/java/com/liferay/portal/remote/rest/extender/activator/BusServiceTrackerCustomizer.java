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

import org.apache.cxf.Bus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import javax.ws.rs.core.Application;

/**
 * @author Carlos Sierra Andrés
 */
public class BusServiceTrackerCustomizer
	implements ServiceTrackerCustomizer<Bus, ServiceTracker<Application,
		ApplicationServiceTrackerCustomizer.Tracked>> {

	private BundleContext _bundleContext;

	public BusServiceTrackerCustomizer(
		BundleContext bundleContext) {

		_bundleContext = bundleContext;
	}

	@Override
	public ServiceTracker
		<Application, ApplicationServiceTrackerCustomizer.Tracked>
	addingService(ServiceReference<Bus> serviceReference) {

		Bus bus = _bundleContext.getService(serviceReference);

		try {
			ServiceTracker
				<Application,
					ApplicationServiceTrackerCustomizer.Tracked>
				applicationTracker =
				new ServiceTracker<>(_bundleContext, Application.class,
					new ApplicationServiceTrackerCustomizer(
						_bundleContext, bus));

			applicationTracker.open();

			return applicationTracker;
		}
		catch (Exception e) {
			_bundleContext.ungetService(serviceReference);

			throw e;
		}
	}

	@Override
	public void modifiedService(
		ServiceReference<Bus> reference,
		ServiceTracker
			<Application, ApplicationServiceTrackerCustomizer.Tracked>
			service) {

		removedService(reference, service);

		addingService(reference);
	}

	@Override
	public void removedService(
		ServiceReference<Bus> serviceReference,
		ServiceTracker
			<Application, ApplicationServiceTrackerCustomizer.Tracked>
			serviceTracker) {

		_bundleContext.ungetService(serviceReference);

		serviceTracker.close();
	}
}
