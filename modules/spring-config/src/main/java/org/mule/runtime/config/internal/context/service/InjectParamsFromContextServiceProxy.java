/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.service;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceUtils.calculateAugmentedArgForParamerter;
import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceUtils.resolveInjectableMethodFor;
import static org.mule.runtime.core.api.util.ClassUtils.findImplementedInterfaces;

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.container.internal.MetadataInvocationHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.LoadingCache;

import jakarta.inject.Inject;

/**
 * Proxies a {@link Service} instance to automatically {@link Inject} parameters for invocations of implementation methods.
 *
 * @since 4.0
 */
public class InjectParamsFromContextServiceProxy extends MetadataInvocationHandler<Service> {

  private final LoadingCache<Class<?>, Collection<?>> lookupAllByTypeCache;
  private final LoadingCache<String, Optional<?>> lookupByNameCache;

  /**
   * Creates a new proxy for the provided service instance.
   *
   * @param service  service instance to wrap. Non null.
   * @param registry the {@link Registry} to use for resolving injectable parameters. Non null.
   */
  public InjectParamsFromContextServiceProxy(Service service, Registry registry) {
    super(service);
    requireNonNull(registry, "registry cannot be null");

    lookupAllByTypeCache = newBuilder().build(registry::lookupAllByType);
    lookupByNameCache = newBuilder().build(registry::lookupByName);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Method injectable = resolveInjectableMethod(method);

    if (injectable == null) {
      return doInvoke(proxy, method, args);
    } else {
      final List<Object> augmentedArgs = args == null ? new ArrayList<>() : new ArrayList<>(asList(args));

      for (int i = method.getParameters().length; i < injectable.getParameters().length; ++i) {
        final Parameter parameter = injectable.getParameters()[i];
        augmentedArgs.add(calculateAugmentedArgForParamerter(parameter,
                                                             lookupByNameCache, lookupAllByTypeCache,
                                                             injectable, getProxiedObject().getName()));
      }

      return doInvoke(proxy, injectable, augmentedArgs.toArray());
    }
  }

  private Method resolveInjectableMethod(Method method) {
    Method candidate = null;

    for (Method serviceImplMethod : getImplementationDeclaredMethods()) {
      candidate = resolveInjectableMethodFor(method, serviceImplMethod,
                                             lookupAllByTypeCache,
                                             candidate, getProxiedObject().getName());
    }
    return candidate;
  }

  /**
   * Creates a proxy for the provided service instance.
   *
   * @param service  service to wrap. Non null.
   * @param registry the {@link Registry} to use for resolving injectable parameters. Non null.
   * @return a new proxy instance.
   */
  public static Service createInjectProviderParamsServiceProxy(Service service, Registry registry) {
    checkArgument(service != null, "service cannot be null");
    checkArgument(registry != null, "registry cannot be null");
    InvocationHandler handler = new InjectParamsFromContextServiceProxy(service, registry);

    return (Service) newProxyInstance(service.getClass().getClassLoader(), findImplementedInterfaces(service.getClass()),
                                      handler);
  }
}
