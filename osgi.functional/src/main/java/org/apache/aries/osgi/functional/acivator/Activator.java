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

package org.apache.aries.osgi.functional.acivator;

import org.apache.aries.osgi.functional.OSGi;
import org.apache.aries.osgi.functional.OSGiOperation.OSGiResult;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ManagedService;

import java.util.HashMap;

import static org.apache.aries.osgi.functional.OSGi.bundles;
import static org.apache.aries.osgi.functional.OSGi.changeContext;
import static org.apache.aries.osgi.functional.OSGi.close;
import static org.apache.aries.osgi.functional.OSGi.just;
import static org.apache.aries.osgi.functional.OSGi.onClose;
import static org.apache.aries.osgi.functional.OSGi.register;
import static org.apache.aries.osgi.functional.OSGi.runOsgi;
import static org.apache.aries.osgi.functional.OSGi.services;

/**
 * @author Carlos Sierra Andrés
 */
public class Activator implements BundleActivator {

	private OSGiResult<?> _result;

	public static class Component {
		ManagedService _managedService;
		int a;

		public Component(ManagedService managedService, int a) {
			_managedService = managedService;
			this.a = a;
		}

		@Override
		public String toString() {
			return "Component{" +
				"_managedService=" + _managedService +
				", a=" + a +
				'}';
		}
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		OSGi<Component> program =
			bundles(Bundle.ACTIVE).map(Bundle::getBundleContext).flatMap(bc ->
			changeContext(bc,
				onClose(() -> System.out.println("BC " + bc + " is gone")).then(
				services(ManagedService.class).flatMap(ms ->
				just(25).flatMap(a ->
				just(30).map(b -> {
					System.out.println("YAY! " + ms + " : " + a + b);

					return new Component(ms, a + b);
				})
				))
			)));

		_result = runOsgi(bundleContext, program.flatMap(c -> {
			System.out.println("COMPONENT: " + c);

			return register(Component.class, c, new HashMap<>());
		}));
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		close(_result);
	}
}
