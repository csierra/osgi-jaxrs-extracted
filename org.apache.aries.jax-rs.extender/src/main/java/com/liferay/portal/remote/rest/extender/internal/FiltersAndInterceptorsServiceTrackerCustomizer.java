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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Carlos Sierra Andrés
 */
public class FiltersAndInterceptorsServiceTrackerCustomizer
	implements ServiceTrackerCustomizer<Object, ServiceTracker<?, ?>> {

	private BundleContext _bundleContext;

	public FiltersAndInterceptorsServiceTrackerCustomizer(
		BundleContext bundleContext) {

		_bundleContext = bundleContext;
	}

	@Override
	public ServiceTracker<?, ?> addingService(final ServiceReference<Object> reference) {
		final String filterBase =
			reference.getProperty("osgi.jaxrs.filter.base").toString();

		final Object service = _bundleContext.getService(reference);

		ServiceTracker<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator> serviceTracker = new ServiceTracker<>(
			_bundleContext, CXFJaxRsServiceRegistrator.class,
			new ServiceTrackerCustomizer
				<CXFJaxRsServiceRegistrator, CXFJaxRsServiceRegistrator>() {

				@Override
				public CXFJaxRsServiceRegistrator addingService(
					ServiceReference<CXFJaxRsServiceRegistrator> cxfReference) {

					Object resourceBaseObject =
						cxfReference.getProperty("CXF_ENDPOINT_ADDRESS");

					if (resourceBaseObject == null) {
						return null;
					}

					String resourceBase = resourceBaseObject.toString();

					if (resourceBase.startsWith(filterBase)) {
						CXFJaxRsServiceRegistrator serviceRegistrator =
							_bundleContext.getService(cxfReference);
						try {
							serviceRegistrator.addProvider(service);

							return serviceRegistrator;
						}
						finally {
							_bundleContext.ungetService(reference);
						}
					}

					return null;
				}

				@Override
				public void modifiedService(
					ServiceReference<CXFJaxRsServiceRegistrator> reference,
					CXFJaxRsServiceRegistrator service) {

					removedService(reference, service);
					addingService(reference);
				}

				@Override
				public void removedService(
					ServiceReference<CXFJaxRsServiceRegistrator> reference,
					CXFJaxRsServiceRegistrator service) {

					CXFJaxRsServiceRegistrator serviceRegistrator =
						_bundleContext.getService(reference);
					try {
						serviceRegistrator.removeProvider(service);
					}
					finally {
						_bundleContext.ungetService(reference);
					}
				}
			});

		serviceTracker.open();

		return serviceTracker;
	}

	@Override
	public void modifiedService(
		ServiceReference<Object> reference, ServiceTracker<?, ?> serviceTracker) {

		removedService(reference, serviceTracker);
		addingService(reference);
	}

	@Override
	public void removedService(
		ServiceReference<Object> reference, ServiceTracker<?, ?> serviceTracker) {

		_bundleContext.ungetService(reference);

		serviceTracker.close();
	}
}
