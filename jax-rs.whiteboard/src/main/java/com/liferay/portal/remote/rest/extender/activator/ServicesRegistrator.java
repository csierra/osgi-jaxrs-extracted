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
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @author Carlos Sierra Andrés
 */
public class ServicesRegistrator 
	implements ServiceTrackerCustomizer<ServletContextHelper, Object> {

	public ServicesRegistrator(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	@Override
	public Object addingService(
		ServiceReference<ServletContextHelper> reference) {
		
		String contextPath = (String)reference.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);
		
		CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();

		CXFBusFactory cxfBusFactory =
			(CXFBusFactory) CXFBusFactory.newInstance(
				CXFBusFactory.class.getName());

		Bus bus = cxfBusFactory.createBus();

		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
			"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + 
				HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME + ")");
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
		properties.put(Constants.SERVICE_RANKING, -1);

		cxfNonSpringServlet.setBus(bus);

		_servletServiceRegistration = _bundleContext.registerService(
			Servlet.class, cxfNonSpringServlet, properties);

		properties = new Hashtable<>();

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
			contextPath);

		_busServiceRegistration = _bundleContext.registerService(
			Bus.class, bus, properties);

		return new Object();
	}

	@Override
	public void modifiedService(
		ServiceReference<ServletContextHelper> reference, Object object) {		
	}

	@Override
	public void removedService(
		ServiceReference<ServletContextHelper> reference, Object object) {

		try {
			_busServiceRegistration.unregister();
		}
		catch (Exception e) {
			if (_logger.isWarnEnabled()) {
				_logger.warn(
					"Unable to unregister CXF bus service registration " +
						_busServiceRegistration);
			}
		}

		try {
			_servletServiceRegistration.unregister();
		}
		catch (Exception e) {
			if (_logger.isWarnEnabled()) {
				_logger.warn(
					"Unable to unregister servlet service registration " +
						_servletServiceRegistration);
			}
		}
	}

	public void start() throws InvalidSyntaxException {
		Filter filter = _bundleContext.createFilter(
			"(&(objectClass=" + ServletContextHelper.class.getName() + ")(" +
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + 
				HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME +"))");

		_serviceTracker = new ServiceTracker<>(_bundleContext, filter, this);

		_serviceTracker.open();
	}

	public void stop() {
		_serviceTracker.close();
	}

	private static final Logger _logger = LoggerFactory.getLogger(
		ServicesRegistrator.class);

	private final BundleContext _bundleContext;
	private ServiceRegistration<Bus> _busServiceRegistration;
	private ServiceTracker<ServletContextHelper, Object> _serviceTracker;
	private ServiceRegistration<Servlet> _servletServiceRegistration;

}
