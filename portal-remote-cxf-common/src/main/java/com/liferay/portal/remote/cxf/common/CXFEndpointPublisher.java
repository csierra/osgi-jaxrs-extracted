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

package com.liferay.portal.remote.cxf.common;

import aQute.bnd.annotation.metatype.Configurable;
import com.liferay.portal.remote.cxf.common.configuration.CXFEndpointPublisherConfiguration;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Carlos Sierra Andrés
 */
@Component(
	configurationPid = "com.liferay.portal.remote.cxf.common.configuration.CXFEndpointPublisherConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class CXFEndpointPublisher {

	@Activate
	protected void activate(
		BundleContext bundleContext, Map<String, Object> properties) {

		_dependencyManager = new DependencyManager(bundleContext);

		org.apache.felix.dm.Component component =
			_dependencyManager.createComponent();

		CXFEndpointPublisherConfiguration cxfEndpointPublisherConfiguration =
			Configurable.createConfigurable(
				CXFEndpointPublisherConfiguration.class, properties);

		ServicesRegistrator servicesRegistrator = new ServicesRegistrator(
			bundleContext, properties);

		component.setImplementation(servicesRegistrator);

		String[] extensions = cxfEndpointPublisherConfiguration.extensions();

		if (extensions != null) {
			for (String extension : extensions) {
				ServiceDependency serviceDependency =
					_dependencyManager.createServiceDependency();

				serviceDependency.setCallbacks(
					servicesRegistrator, "addExtension", "-");
				serviceDependency.setRequired(true);
				serviceDependency.setService(Object.class, extension);

				component.add(serviceDependency);
			}
		}

		_dependencyManager.add(component);
	}

	@Deactivate
	protected void deactivate() {
		_dependencyManager.clear();
	}

	@Modified
	protected void modified(
		BundleContext bundleContext, Map<String, Object> properties) {

		deactivate();

		activate(bundleContext, properties);
	}

	private DependencyManager _dependencyManager;

	private static class ServicesRegistrator {

		public ServicesRegistrator(
			BundleContext bundleContext, Map<String, Object> properties) {

			_bundleContext = bundleContext;

			_properties = properties;
		}

		@SuppressWarnings("unused")
		protected void addExtension(
			Map<String, Object> properties, Object extension) {

			Class<?> extensionClass = (Class<?>)properties.get(
				"cxf.extension.class");

			if (extensionClass == null) {
				extensionClass = extension.getClass();
			}

			_extensions.put(extensionClass, extension);
		}

		@SuppressWarnings("unused")
		protected void start() {
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
					new ServletContextHelper(_bundleContext.getBundle()) {},
					properties);

			CXFNonSpringServlet cxfNonSpringServlet = new CXFNonSpringServlet();

			CXFBusFactory cxfBusFactory =
				(CXFBusFactory)CXFBusFactory.newInstance(
					CXFBusFactory.class.getName());

			Bus bus = cxfBusFactory.createBus(_extensions);

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

		@SuppressWarnings("unused")
		protected void stop() {
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
			CXFEndpointPublisher.class);

		private final BundleContext _bundleContext;
		private ServiceRegistration<Bus> _busServiceRegistration;
		private final Map<Class<?>, Object> _extensions = new HashMap<>();
		private final Map<String, Object> _properties;
		private ServiceRegistration<ServletContextHelper>
			_servletContextHelperServiceRegistration;
		private ServiceRegistration<Servlet> _servletServiceRegistration;

	}

}