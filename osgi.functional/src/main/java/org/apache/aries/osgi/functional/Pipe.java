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

package org.apache.aries.osgi.functional;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Carlos Sierra Andrés
 */
public class Pipe<I, O> {

	Function<I, O> pipe;

	private Pipe(Function<I, O> fun) {
		this.pipe = fun;
	}

	public <U> Pipe<I, U> map(Function<O, U> fun) {
		this.pipe = (Function)this.pipe.andThen(fun);

		return (Pipe<I, U>)this;
	}

	public Consumer<I> getSource() {
		return i -> {
			pipe.apply(i);
		};
	}

	public static <T> Pipe<T, T> create() {
		return new Pipe<>(x -> x);
	}
}
