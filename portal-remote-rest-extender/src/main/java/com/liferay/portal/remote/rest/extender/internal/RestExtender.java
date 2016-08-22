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

package com.liferay.portal.remote.rest.extender.internal;

import aQute.bnd.annotation.metatype.Configurable;
import com.liferay.portal.remote.dependency.manager.tccl.TCCLDependencyManager;
import com.liferay.portal.remote.rest.extender.configuration.RestExtenderConfiguration;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.ServiceDependency;

import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Carlos Sierra Andrés
 */
public class RestExtender {

	public RestExtenderConfiguration getRestExtenderConfiguration() {
		return _restExtenderConfiguration;
	}

	protected void update(
		Component component, Dictionary<String, Object> properties) {

		_dependencyManager =
			(TCCLDependencyManager)component.getDependencyManager();

		if (_component != null) {
			_component.stop();

			_dependencyManager.remove(_component);
		}

		_restExtenderConfiguration = Configurable.createConfigurable(
			RestExtenderConfiguration.class, properties);

		_component = _dependencyManager.createComponent();

		CXFJaxRsServiceRegistrator cxfJaxRsServiceRegistrator =
			new CXFJaxRsServiceRegistrator((Hashtable)properties);

		_component.setImplementation(cxfJaxRsServiceRegistrator);

		addBusDependencies();
		addJaxRsApplicationDependencies();
		addJaxRsProviderServiceDependencies();
		addJaxRsServiceDependencies();

		_dependencyManager.add(_component);

		_component.start();
	}

	protected void addBusDependencies() {
		RestExtenderConfiguration restExtenderConfiguration =
			getRestExtenderConfiguration();

		String[] contextPaths = restExtenderConfiguration.contextPaths();

		if (contextPaths == null) {
			return;
		}

		for (String contextPath : contextPaths) {
			if (contextPath == null || contextPath.isEmpty()) {
				continue;
			}

			addTCCLServiceDependency(
				true, Bus.class,
				"(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH +
					"=" + contextPath + ")",
				"addBus", "removeBus");
		}
	}

	protected void addJaxRsApplicationDependencies() {
		RestExtenderConfiguration restExtenderConfiguration =
			getRestExtenderConfiguration();

		String[] jaxRsApplicationFilterStrings =
			restExtenderConfiguration.jaxRsApplicationFilterStrings();

		if (jaxRsApplicationFilterStrings == null) {
			addTCCLServiceDependency(
				false, Application.class, null, "addApplication",
				"removeApplication");

			return;
		}

		for (String jaxRsApplicationFilterString :
				jaxRsApplicationFilterStrings) {

			addTCCLServiceDependency(
				false, Application.class, jaxRsApplicationFilterString,
				"addApplication", "removeApplication");
		}
	}

	protected void addJaxRsProviderServiceDependencies() {
		RestExtenderConfiguration soapExtenderConfiguration =
			getRestExtenderConfiguration();

		String[] jaxRsProviderFilterStrings =
			soapExtenderConfiguration.jaxRsProviderFilterStrings();

		if (jaxRsProviderFilterStrings == null) {
			return;
		}

		for (String jaxRsProviderFilterString : jaxRsProviderFilterStrings) {
			if (jaxRsProviderFilterString == null ||
				jaxRsProviderFilterString.isEmpty()) {

				continue;
			}

			addTCCLServiceDependency(
				false, null, jaxRsProviderFilterString, "addProvider",
				"removeProvider");
		}
	}

	protected void addJaxRsServiceDependencies() {
		RestExtenderConfiguration soapExtenderConfiguration =
			getRestExtenderConfiguration();

		String[] jaxRsServiceFilterStrings =
			soapExtenderConfiguration.jaxRsServiceFilterStrings();

		if (jaxRsServiceFilterStrings == null) {
			return;
		}

		for (String jaxRsServiceFilterString : jaxRsServiceFilterStrings) {
			if (jaxRsServiceFilterString == null ||
				jaxRsServiceFilterString.isEmpty()) {

				continue;
			}

			addTCCLServiceDependency(
				false, null, jaxRsServiceFilterString, "addService",
				"removeService");
		}
	}

	protected ServiceDependency addTCCLServiceDependency(
		boolean required, Class<?> clazz, String filter, String addName,
		String removeName) {

		ServiceDependency serviceDependency =
			_dependencyManager.createTCCLServiceDependency();

		serviceDependency.setCallbacks(addName, removeName);
		serviceDependency.setRequired(required);

		if (clazz == null) {
			serviceDependency.setService(filter);
		}
		else {
			if (filter == null) {
				serviceDependency.setService(clazz);
			}
			else {
				serviceDependency.setService(clazz, filter);
			}
		}

		_component.add(serviceDependency);

		return serviceDependency;
	}

	private Component _component;
	private TCCLDependencyManager _dependencyManager;
	private RestExtenderConfiguration _restExtenderConfiguration;

}