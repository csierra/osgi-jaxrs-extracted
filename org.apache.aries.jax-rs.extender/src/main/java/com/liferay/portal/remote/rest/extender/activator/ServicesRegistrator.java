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
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * @author Carlos Sierra Andrés
 */
public class ServicesRegistrator {

	public ServicesRegistrator(
		BundleContext bundleContext, Map<String, Object> properties) {

		_bundleContext = bundleContext;

		_properties = properties;
	}

	public void start() {
		Dictionary<String, Object> properties = new Hashtable<>();

		Object contextPathObject = _properties.get("contextPath");

		String contextPath = contextPathObject.toString();

		String contextName = contextPath.substring(1);

		contextName = contextName.replace("/", ".");

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
			contextName);
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
			contextPath);

		_servletContextHelperServiceRegistration =
			_bundleContext.registerService(
				ServletContextHelper.class,
				new ServletContextHelper(_bundleContext.getBundle()) {
				},
				properties);

		CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();

		CXFBusFactory cxfBusFactory =
			(CXFBusFactory) CXFBusFactory.newInstance(
				CXFBusFactory.class.getName());

		Bus bus = cxfBusFactory.createBus();

		properties = new Hashtable<>();

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
			"(osgi.http.whiteboard.context.name=" + contextName + ")");
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME,
			CXFNonSpringServlet.class.getName());
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");

		cxfNonSpringServlet.setBus(bus);

		_servletServiceRegistration = _bundleContext.registerService(
			Servlet.class, cxfNonSpringServlet, properties);

		properties = new Hashtable<>();

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
			contextPath);

		_busServiceRegistration = _bundleContext.registerService(
			Bus.class, bus, properties);
	}

	public void stop() {
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

		try {
			_servletContextHelperServiceRegistration.unregister();
		}
		catch (Exception e) {
			if (_logger.isWarnEnabled()) {
				_logger.warn(
					"Unable to unregister servlet context helper service " +
						"registration " +
						_servletContextHelperServiceRegistration);
			}
		}
	}

	private static final Logger _logger = LoggerFactory.getLogger(
		ServicesRegistrator.class);

	private final BundleContext _bundleContext;
	private ServiceRegistration<Bus> _busServiceRegistration;
	private final Map<String, Object> _properties;
	private ServiceRegistration<ServletContextHelper>
		_servletContextHelperServiceRegistration;
	private ServiceRegistration<Servlet> _servletServiceRegistration;

}
