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

import org.apache.aries.osgi.functional.OSGiOperation.OSGiResult;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class OSGi<T> {

	private static final Consumer<Void> NOOP = x -> {
	};

	protected OSGiOperation<T> _operation;

	public OSGi(OSGiOperation<T> operation) {
		_operation = operation;
	}

	public <S> OSGi<S> map(Function<T, S> function) {
		return new OSGi<>(((bundleContext) -> {
			OSGiResult<T> osgiResult = _operation.run(bundleContext);

			return new OSGiResult<>(
				osgiResult.added.map(function),
				osgiResult.removed.map(function),
				osgiResult.start, osgiResult.close);
		}));
	}

	public static <S> OSGi<S> just(S s) {
		return new OSGi<>(((bundleContext) -> {

			Pipe<S, S> added = Pipe.create();
			Consumer<S> source = added.getSource();

			return new OSGiResult<>(
				added, Pipe.create(), x -> source.accept(s), NOOP);
		}));
	}

	public static <S> OSGi<S> nothing() {
		return new OSGi<>(((bundleContext) -> new OSGiResult<>(
			Pipe.create(), Pipe.create(), NOOP, NOOP)));
	}

	public <S> OSGi<S> flatMap(Function<T, OSGi<S>> fun) {
		return new OSGi<>(
			((bundleContext) -> {

				Map<T, OSGiResult<S>> map = new IdentityHashMap<>();

				AtomicReference<Consumer<Void>> closeReference =
					new AtomicReference<>(x -> {});

				Pipe<S, S> added = Pipe.create();

				Consumer<S> addedSource = added.getSource();

				OSGiResult<S> osgiResult = new OSGiResult<>(
					added, Pipe.create(), null,
					x -> {
						synchronized (map) {
							for (OSGiResult<S> result : map.values()) {
								close(result);
							}
						}

						closeReference.get().accept(null);
					});

				osgiResult.start = s -> {
					OSGiResult<T> or1 = _operation.run(bundleContext);

					closeReference.set(or1.close);

					or1.added.map(t -> {
						OSGi<S> program = fun.apply(t);

						OSGiResult<S> or2 = program._operation.run(
							bundleContext);

						map.put(t, or2);

						Consumer<Boolean> close = x -> {
							synchronized (map) {
								OSGiResult<S> closer = map.remove(t);

								if (closer != null) {
									OSGi.close(closer);
								}
							}
						};

						or2.added.map(r -> {addedSource.accept(r); return null;});
//						or2.added.onClose(() -> close.accept(null));

						or2.start.accept(null);

						return null;
					});

					or1.removed.map(t -> {
						synchronized (map) {
							OSGiResult<S> osgiResult1 = map.remove(t);

							if (osgiResult1 != null) {
								OSGi.close(osgiResult1);
							}
						}

						return null;
					});

					or1.start.accept(null);
				};

				return osgiResult;
			}
		));
	}

	public <S> OSGi<S> then(OSGi<S> next) {
		return flatMap(ignored -> next);
	}

	public <S> OSGi<Void> foreach(Function<T, OSGi<S>> fun) {
		return this.flatMap(fun).map(x -> null);
	}

	public static OSGi<Dictionary<String, ?>> configurations(
		String factoryPid) {

		return new OSGi<>(bundleContext -> {
			Map<String, Dictionary<String, ?>> results =
				new ConcurrentHashMap<>();

			AtomicReference<ServiceRegistration<ManagedServiceFactory>>
				serviceRegistrationReference = new AtomicReference<>(null);

			Pipe<Dictionary<String, ?>, Dictionary<String, ?>> added =
				Pipe.create();

			Consumer<Dictionary<String, ?>> addedSource = added.getSource();

			Pipe<Dictionary<String, ?>, Dictionary<String, ?>> removed =
				Pipe.create();

			Consumer<Dictionary<String, ?>> removedSource = removed.getSource();

			Consumer<Void> start = x ->
				serviceRegistrationReference.set(bundleContext.registerService(
					ManagedServiceFactory.class, new ManagedServiceFactory() {
						@Override
						public String getName() {
							return "Functional OSGi Managed Service Factory";
						}

						@Override
						public void updated(
							String s, Dictionary<String, ?> dictionary)
							throws ConfigurationException {

							results.put(s, dictionary);

							addedSource.accept(dictionary);
						}

						@Override
						public void deleted(String s) {
							Dictionary<String, ?> remove = results.remove(s);

							removedSource.accept(remove);

						}
					},
					new Hashtable<String, Object>() {{
						put("service.pid", factoryPid);
					}}));


			return new OSGiResult<>(added, removed, start,
				x -> {
					serviceRegistrationReference.get().unregister();

					for (Dictionary<String, ?> dictionary : results.values()) {
						removedSource.accept(dictionary);
					}
				});
		});
	}

	public static OSGi<Dictionary<String, ?>> configuration(String pid) {
		return new OSGi<>(bundleContext -> {
			AtomicReference<Dictionary<?, ?>> atomicReference =
				new AtomicReference<>(null);

			AtomicReference<ServiceRegistration<ManagedService>>
				serviceRegistrationReferece = new AtomicReference<>(null);

			Pipe<Dictionary<String, ?>, Dictionary<String, ?>> added =
				Pipe.create();

			Consumer<Dictionary<String, ?>> addedSource = added.getSource();

			Pipe<Dictionary<String, ?>, Dictionary<String, ?>> removed =
				Pipe.create();

			Consumer<Dictionary<String, ?>> removedSource = removed.getSource();

			Consumer<Void> start = x ->
				serviceRegistrationReferece.set(
					bundleContext.registerService(
						ManagedService.class,
						properties -> {
							if (properties == null) {
								removedSource.accept(null);
							}
							else {
								if (atomicReference.compareAndSet(
									null, properties)) {

									addedSource.accept(properties);
								}
								else {
									addedSource.accept(null);
								}
							}
						},
						new Hashtable<String, Object>() {{
							put("service.pid", pid);
						}}));

			return new OSGiResult<>(
				added, removed, start,
				x -> serviceRegistrationReferece.get().unregister());
		});
	}

	public static OSGi<Void> onClose(Consumer<Void> action) {
		return new OSGi<>(bundleContext -> new OSGiResult<>(
			Pipe.create(), Pipe.create(), NOOP, action::accept));
	}

	public static <T> MOSGi<T> services(Class<T> clazz) {
		return services(clazz, null);
	}

	public static <T> OSGi<ServiceReference<T>> serviceReferences(
		Class<T> clazz, String filterString) {

		return new OSGi<>(bundleContext -> {
			Pipe<ServiceReference<T>, ServiceReference<T>> added =
				Pipe.create();

			Consumer<ServiceReference<T>> addedSource = added.getSource();

			Pipe<ServiceReference<T>, ServiceReference<T>> removed =
				Pipe.create();

			Consumer<ServiceReference<T>> removedSource = removed.getSource();

			ServiceTracker<T, ServiceReference<T>> serviceTracker =
				new ServiceTracker<T, ServiceReference<T>>(
					bundleContext,
					buildFilter(bundleContext, filterString, clazz), null) {

					@Override
					public ServiceReference<T> addingService(
						ServiceReference<T> reference) {

						addedSource.accept(reference);

						return reference;
					}

					@Override
					public void removedService(
						ServiceReference<T> reference, ServiceReference<T> t) {

						super.removedService(reference, t);

						removedSource.accept(t);
					}
				};

			return new OSGiResult<>(
				added, removed, x -> serviceTracker.open(),
				x -> serviceTracker.close());

		});
	}

	public static <T> MOSGi<T> services(Class<T> clazz, String filterString) {
		return new MOSGi<>(bundleContext -> {
			Pipe<T, T> added = Pipe.create();

			Pipe<T, T> removed = Pipe.create();

			Consumer<T> addedSource = added.getSource();

			Consumer<T> removedSource = removed.getSource();

			ServiceTracker<T, T> serviceTracker =
				new ServiceTracker<T, T>(
					bundleContext,
					buildFilter(bundleContext, filterString, clazz), null) {

					@Override
					public T addingService(ServiceReference<T> reference) {
						ServiceObjects<T> serviceObjects =
							bundleContext.getServiceObjects(reference);

						serviceObjects.getService();

						T t = super.addingService(reference);

						addedSource.accept(t);

						return t;
					}

					@Override
					public void removedService(
						ServiceReference<T> reference, T t) {

						super.removedService(reference, t);

						removedSource.accept(t);
					}
				};

			return new OSGiResult<>(
				added, removed, x -> serviceTracker.open(),
				x -> serviceTracker.close());
		});
	}

	public static <T> OSGi<T> changeContext(
		BundleContext bundleContext, OSGi<T> program) {

		return new OSGi<>(b -> program._operation.run(bundleContext));
	}

	public static MOSGi<Bundle> bundles(int stateMask) {
		return new MOSGi<>(bundleContext -> {
			Pipe<Bundle, Bundle> added = Pipe.create();

			Consumer<Bundle> addedSource = added.getSource();

			Pipe<Bundle, Bundle> removed = Pipe.create();

			Consumer<Bundle> removedSource = removed.getSource();

			BundleTracker<Bundle> bundleTracker =
				new BundleTracker<>(
					bundleContext, stateMask,
					new BundleTrackerCustomizer<Bundle>() {

					@Override
					public Bundle addingBundle(
						Bundle bundle, BundleEvent bundleEvent) {

						addedSource.accept(bundle);

						return bundle;
					}

					@Override
					public void modifiedBundle(
						Bundle bundle, BundleEvent bundleEvent,
						Bundle bundle2) {

						removedBundle(bundle, bundleEvent, bundle2);

						addingBundle(bundle, bundleEvent);
					}

					@Override
					public void removedBundle(
						Bundle bundle, BundleEvent bundleEvent,
						Bundle bundle2) {

						removedSource.accept(bundle);
					}
				});

			return new OSGiResult<>(
				added, removed, x -> bundleTracker.open(),
				x -> bundleTracker.close());
		});
	}


	private static <T> Filter buildFilter(
		BundleContext bundleContext, String filterString, Class<T> clazz) {
		Filter filter;
		try {
			if (filterString == null) {
				filter = bundleContext.createFilter(
					"(objectClass=" + clazz.getName() + ")");
			}
			else {
				filter = bundleContext.createFilter(
					"(&(objectClass=" + clazz.getName() + ")" +
					filterString + ")");
			}
		}
		catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
		return filter;
	}

	public static <T, S extends T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, S service, Map<String, Object> properties) {

		return new OSGi<>(bundleContext -> {
			ServiceRegistration<T> serviceRegistration =
				bundleContext.registerService(
					clazz, service, new Hashtable<>(properties));

			Pipe<ServiceRegistration<T>, ServiceRegistration<T>> added =
				Pipe.create();

			Consumer<ServiceRegistration<T>> addedSource = added.getSource();

			return new OSGiResult<>(
				added, Pipe.create(),
				x -> addedSource.accept(serviceRegistration),
				x -> {
					try {
						serviceRegistration.unregister();
					}
					catch (Exception e) {
					}
				});
		});
	}

	public static <T> OSGiResult<T> runOsgi(
		BundleContext bundleContext, OSGi<T> program) {

		AtomicBoolean executed = new AtomicBoolean(false);

		OSGiResult<T> osgiResult = program._operation.run(bundleContext);

		Consumer<Void> close = x -> {
			boolean hasBeenExecuted = executed.getAndSet(true);

			if (!hasBeenExecuted) {
				osgiResult.close.accept(null);
			}
		};

		osgiResult.start.accept(null);

		return new OSGiResult<>(
			osgiResult.added, osgiResult.removed,
			osgiResult.start, close);
	}

	public static void close(OSGiResult<?> osgiResult) {
		osgiResult.close.accept(null);
	}

	public static class MOSGi<T> extends OSGi<T> {
		public MOSGi(OSGiOperation<T> operation) {
			super(operation);
		}

		public OSGi<T> once() {
			AtomicReference<T> atomicReference = new AtomicReference<>(null);

			return flatMap(t -> {
				if (atomicReference.compareAndSet(null, t)) {
					return just(t);
				}
				else {
					return nothing();
				}
			});
		}
	}

}


