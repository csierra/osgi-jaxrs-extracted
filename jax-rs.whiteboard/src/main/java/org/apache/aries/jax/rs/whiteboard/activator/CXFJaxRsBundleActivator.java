/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.jax.rs.whiteboard.activator;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.Servlet;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.aries.jax.rs.whiteboard.internal.CXFJaxRsServiceRegistrator;
import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiResult;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.aries.jax.rs.whiteboard.internal.BusServiceTrackerCustomizer;

import static org.apache.aries.osgi.functional.OSGi.bundleContext;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.onClose;
import static org.apache.aries.osgi.functional.OSGi.register;
import static org.apache.aries.osgi.functional.OSGi.serviceReferences;
import static org.apache.aries.osgi.functional.OSGi.services;

/**
 * @author Carlos Sierra Andrés
 */
public class CXFJaxRsBundleActivator implements BundleActivator {

	private OSGiResult<ServiceRegistration<Servlet>> _serviceRegistrationOSGiResult;
	private OSGiResult<Void> _applicationSelectorResult;

	public static <S> OSGi<S> SERVLET_CONTEXT_HELPER_REFS(
		Function<ServiceReference<ServletContextHelper>, OSGi<S>> fun) {
		return OSGi.serviceReferences(ServletContextHelper.class, "(" +
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" +
			HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME + ")").
			flatMap(fun);
	}
	private ServiceTracker<?, ?> _busServiceTracker;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Thread thread = Thread.currentThread();

		ClassLoader contextClassLoader = thread.getContextClassLoader();

		Bundle bundle = bundleContext.getBundle();

		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);

		thread.setContextClassLoader(bundleWiring.getClassLoader());

		try {

			// Initialize instance so it is never looked up again

			RuntimeDelegate.getInstance();
		}
		finally {
			thread.setContextClassLoader(contextClassLoader);
		}

		Dictionary<String, Object> runtimeProperties = new Hashtable<>();

		runtimeProperties.put("endpoints", new ArrayList<String>());

		// TODO make the context path of the JAX-RS Whiteboard configurable.

		_serviceRegistrationOSGiResult =
			SERVLET_CONTEXT_HELPER_REFS(schr ->
			just(createBus()).flatMap(bus ->
			just(new CXFNonSpringServlet()).flatMap(servlet ->
				register(Bus.class, bus, busProperties(schr)).flatMap(ign -> {
					servlet.setBus(bus);

				return register(Servlet.class, servlet, servletProperties());
			})))).
		run(bundleContext);

		_busServiceTracker = new ServiceTracker<>(
			bundleContext, Bus.class,
			new BusServiceTrackerCustomizer(bundleContext));

		_busServiceTracker.open();

		_applicationSelectorResult =
			serviceReferences("(jaxrs.application.select=*)").flatMap(ref ->
			just(ref.getProperty("jaxrs.application.select").toString()).flatMap(selector ->
			services(CXFJaxRsServiceRegistrator.class, selector).flatMap(registrator ->
			getService(ref).flatMap(service ->
				registerEndpoint(ref, registrator, service).then(
				onClose(() -> unregisterEndpoint(registrator, service))
			))))).
		run(bundleContext);
	}

	private void unregisterEndpoint(CXFJaxRsServiceRegistrator registrator, Object service) {
		Class<?> serviceClass = service.getClass();

		if (serviceClass.isAnnotationPresent(Provider.class)) {
			registrator.removeProvider(service);
		} else {
			registrator.removeService(service);
		}
	}

	private OSGi<Object> registerEndpoint(
		ServiceReference<Object> ref,
		CXFJaxRsServiceRegistrator registrator, Object service) {

		Class<?> serviceClass = service.getClass();

		Thread thread = Thread.currentThread();

		ClassLoader contextClassLoader = thread.getContextClassLoader();

		ClassLoader classLoader = ref.getBundle().adapt(BundleWiring.class).
			getClassLoader();

		try {
			thread.setContextClassLoader(classLoader);

			if (serviceClass.isAnnotationPresent(Provider.class)) {
				registrator.addProvider(service);
			} else {
				registrator.addService(service);
			}
		}
		finally {
			thread.setContextClassLoader(contextClassLoader);
		}

		return just(service);
	}

	private Map<String, Object> appProperties(
		ServiceReference<Application> appRef) {

		String[] propertyKeys = appRef.getPropertyKeys();

		Map<String, Object> properties = new HashMap<>(
			propertyKeys.length);

		for (String propertyKey : propertyKeys) {
			properties.put(
				propertyKey, appRef.getProperty(propertyKey));
		}

		properties.put(
			"CXF_ENDPOINT_ADDRESS",
			appRef.getProperty("osgi.jaxrs.application.base").
				toString());

		return properties;
	}

	private <T> OSGi<T> getService(ServiceReference<T> appRef) {
		return bundleContext().flatMap(bc ->
		just(bc.getService(appRef)).flatMap(service ->
		onClose(() -> bc.ungetService(appRef)).then(
		just(service)
		)));
	}

	private Map<String, Object> servletProperties() {
		Map<String, Object> properties2 = new HashMap<>();

		properties2.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
			"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" +
				HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME + ")");
		properties2.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/*");
		properties2.put(Constants.SERVICE_RANKING, -1);
		return properties2;
	}

	private Map<String, Object> busProperties(ServiceReference<ServletContextHelper> schr) {
		String contextPath = (String) schr.getProperty(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);

		Map<String, Object> properties = new HashMap<>();

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
			contextPath);
		return properties;
	}

	private Bus createBus() {
		CXFBusFactory cxfBusFactory =
			(CXFBusFactory) CXFBusFactory.newInstance(
				CXFBusFactory.class.getName());

		return cxfBusFactory.createBus();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_busServiceTracker.close();

		_applicationSelectorResult.close();

		_serviceRegistrationOSGiResult.close();


	}

}