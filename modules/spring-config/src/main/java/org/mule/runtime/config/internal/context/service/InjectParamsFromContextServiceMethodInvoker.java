/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.service;

import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceUtils.calculateAugmentedArgForParamerter;
import static org.mule.runtime.config.internal.context.service.InjectParamsFromContextServiceUtils.resolveInjectableMethodFor;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.service.internal.manager.DefaultMethodInvoker;
import org.mule.runtime.module.service.internal.manager.MethodInvoker;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.LoadingCache;

import jakarta.inject.Inject;

/**
 * A {@link MethodInvoker} to automatically reroute {@link Service} method invocations to {@link Inject} annotated overloads,
 * similar to {@link InjectParamsFromContextServiceProxy}
 *
 * @since 4.2
 */
public class InjectParamsFromContextServiceMethodInvoker extends DefaultMethodInvoker {

  public static final String NO_OBJECT_FOUND_FOR_PARAM =
      "No object found in the registry for parameter '%s' of method '%s' in service '%s'";

  private final LoadingCache<Pair<Object, Method>, Method> injectableMethodCache;
  private final LoadingCache<Class<?>, Collection<?>> lookupAllByTypeCache;
  private final LoadingCache<String, Optional<?>> lookupByNameCache;

  /**
   * Creates a new instance
   *
   * @param registry the {@link Registry} to use for resolving injectable parameters. Non null.
   */
  public InjectParamsFromContextServiceMethodInvoker(Registry registry) {
    requireNonNull(registry, "registry cannot be null");

    lookupAllByTypeCache = newBuilder().build(registry::lookupAllByType);
    lookupByNameCache = newBuilder().build(registry::lookupByName);
    injectableMethodCache = newBuilder().build(p -> resolveInjectableMethod(p.getFirst(), p.getSecond()));
  }

  @Override
  public Object invoke(Object target, Method method, Object[] args) throws Throwable {
    Method injectable = injectableMethodCache.get(new Pair<>(target, method));

    if (injectable == method) {
      return super.invoke(target, method, args);
    }

    final List<Object> augmentedArgs;
    try {
      augmentedArgs = calculateAugmentedArgs(target, method, args, injectable);
    } catch (NullPointerException e) {
      // registry is not initialised yet, call original method
      return super.invoke(target, method, args);
    }

    return super.invoke(target, injectable, augmentedArgs.toArray());
  }

  private List<Object> calculateAugmentedArgs(Object target, Method method, Object[] args, Method injectable) {
    final List<Object> augmentedArgs = args == null ? new ArrayList<>() : new ArrayList<>(asList(args));
    for (int i = method.getParameters().length; i < injectable.getParameters().length; ++i) {
      final Parameter parameter = injectable.getParameters()[i];
      augmentedArgs.add(calculateAugmentedArgForParamerter(parameter,
                                                           lookupByNameCache, lookupAllByTypeCache,
                                                           injectable, target.toString()));
    }
    return augmentedArgs;
  }

  private Method resolveInjectableMethod(Object target, Method method) {
    Method candidate = null;

    for (Method serviceImplMethod : getImplementationDeclaredMethods(target)) {
      candidate = resolveInjectableMethodFor(method, serviceImplMethod,
                                             lookupAllByTypeCache,
                                             candidate, target.toString());
    }
    return candidate != null ? candidate : method;
  }

  private Method[] getImplementationDeclaredMethods(Object target) {
    List<Method> methods = new LinkedList<>();
    Class<?> clazz = target.getClass();
    while (clazz != Object.class) {
      methods.addAll(asList(clazz.getDeclaredMethods()));
      clazz = clazz.getSuperclass();
    }

    return methods.toArray(new Method[methods.size()]);
  }

}
