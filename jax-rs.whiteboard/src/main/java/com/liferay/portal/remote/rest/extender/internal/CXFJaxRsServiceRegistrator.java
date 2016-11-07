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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;

/**
 * @author Carlos Sierra Andrés
 */
public class CXFJaxRsServiceRegistrator {

	public CXFJaxRsServiceRegistrator(
		Bus bus, Application application, Map<String, Object> properties) {

		_bus = bus;
		_application = application;
		_properties = properties;

		rewire();
	}

	public void close() {
		if (_closed) {
			return;
		}

		if (_server != null) {
			_server.destroy();
		}

		_closed = true;
	}

	public void addProvider(Object provider) {
		if (_closed) {
			return;
		}

		_providers.add(provider);

		rewire();
	}

	public void addService(Object service) {
		if (_closed) {
			return;
		}

		_services.add(service);

		rewire();
	}

	public void removeProvider(Object provider) {
		if (_closed) {
			return;
		}

		_providers.remove(provider);

		rewire();
	}

	public void removeService(Object service) {
		if (_closed) {
			return;
		}

		_services.remove(service);

		rewire();
	}

	protected synchronized void rewire() {
		if (_server != null) {
			_server.destroy();
		}

		RuntimeDelegate runtimeDelegate = RuntimeDelegate.getInstance();

		JAXRSServerFactoryBean jaxRsServerFactoryBean =
			runtimeDelegate.createEndpoint(
				_application, JAXRSServerFactoryBean.class);

		jaxRsServerFactoryBean.setBus(_bus);
		jaxRsServerFactoryBean.setProperties(_properties);

		JSONProvider<Object> jsonProvider = new JSONProvider<>();

		jsonProvider.setDropCollectionWrapperElement(true);
		jsonProvider.setDropRootElement(true);
		jsonProvider.setSerializeAsArray(true);
		jsonProvider.setSupportUnwrapped(true);

		jaxRsServerFactoryBean.setProvider(jsonProvider);

		for (Object provider : _providers) {
			jaxRsServerFactoryBean.setProvider(provider);
		}

		for (Object service : _services) {
			jaxRsServerFactoryBean.setResourceProvider(
				new SingletonResourceProvider(service, true));
		}

		String address = _properties.get("CXF_ENDPOINT_ADDRESS").toString();

		if (address != null) {
			jaxRsServerFactoryBean.setAddress(address);
		}

		_server = jaxRsServerFactoryBean.create();

		_server.start();
	}

	private volatile boolean _closed = false;
	private final Application _application;
	private final Bus _bus;
	private final Map<String, Object> _properties;
	private final Collection<Object> _providers = new ArrayList<>();
	private Server _server;
	private final Collection<Object> _services = new ArrayList<>();

}