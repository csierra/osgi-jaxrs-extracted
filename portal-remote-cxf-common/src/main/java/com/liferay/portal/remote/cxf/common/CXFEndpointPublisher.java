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
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;

/**
 * @author Carlos Sierra Andrés
 */
public class CXFEndpointPublisher {

	private Component _serviceRegistratorComponent;

	protected void update(
		Component component, Dictionary<String, Object> properties) {

		_dependencyManager = component.getDependencyManager();

		if (_serviceRegistratorComponent != null) {
			_serviceRegistratorComponent.stop();

			_dependencyManager.remove(_serviceRegistratorComponent);
		}

		_serviceRegistratorComponent = _dependencyManager.createComponent();

		CXFEndpointPublisherConfiguration cxfEndpointPublisherConfiguration =
			Configurable.createConfigurable(
				CXFEndpointPublisherConfiguration.class, properties);

		ServicesRegistrator servicesRegistrator = new ServicesRegistrator(
			_dependencyManager.getBundleContext(), (Hashtable)properties);

		_serviceRegistratorComponent.setImplementation(servicesRegistrator);

		String[] extensions = cxfEndpointPublisherConfiguration.extensions();

		if (extensions != null) {
			for (String extension : extensions) {
				ServiceDependency serviceDependency =
					_dependencyManager.createServiceDependency();

				serviceDependency.setCallbacks(
					servicesRegistrator, "addExtension", "-");
				serviceDependency.setRequired(true);
				serviceDependency.setService(Object.class, extension);

				_serviceRegistratorComponent.add(serviceDependency);
			}
		}

		_dependencyManager.add(_serviceRegistratorComponent);
	}

	private DependencyManager _dependencyManager;

}