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

package com.liferay.portal.rest.example;

import org.osgi.service.component.annotations.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * @author Carlos Sierra Andrés
 */
@Component(
	immediate = true,
	property = "jaxrs.application.select=(component.name=com.liferay.portal.rest.example.ExampleApplication)",
	service = ExampleAddon.class
)
public class ExampleAddon {

	@GET
	@Path("/{name}")
	public String sayHello(@PathParam("name") String name) {
		return "Hello " + name;
	}

}
