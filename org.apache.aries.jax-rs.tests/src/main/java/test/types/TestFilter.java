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

package test.types;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

//	property = {
//		"jaxrs.application.select=(component.name=com.liferay.portal.rest.example.ExampleApplication)",
//		"osgi.jaxrs.filter.base=/examples"
//	}
@Provider
public class TestFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext)
		throws IOException {

		System.out.println("FILTERED!");
	}

}
