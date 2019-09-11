/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Arrays.asList;
import static java.util.Arrays.deepEquals;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.registry.IllegalDependencyInjectionException;
import org.mule.runtime.core.internal.config.preferred.PreferredObjectSelector;
import org.mule.runtime.core.internal.util.DefaultMethodInvoker;
import org.mule.runtime.core.internal.util.MethodInvoker;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * A {@link MethodInvoker} to automatically reroute {@link Service} method invocations to {@link Inject} annotated overloads,
 * similar to {@link InjectParamsFromContextServiceProxy}
 *
 * @since 4.2
 */
public class InjectParamsFromContextServiceMethodInvoker extends DefaultMethodInvoker {

  public static final String MANY_CANDIDATES_ERROR_MSG_TEMPLATE =
      "More than one invocation candidate for method '%s' in service '%s'";
  public static final String NO_OBJECT_FOUND_FOR_PARAM =
      "No object found in the registry for parameter '%s' of method '%s' in service '%s'";

  private final Registry registry;

  private com.github.benmanes.caffeine.cache.LoadingCache<Class<?>, Collection<?>> lookupAllByTypeCache;
  private com.github.benmanes.caffeine.cache.LoadingCache<String, Optional<?>> lookupByNameCache;

  /**
   * Creates a new instance
   *
   * @param registry    the {@link Registry} to use for resolving injectable parameters. Non null.
   */
  public InjectParamsFromContextServiceMethodInvoker(Registry registry) {
    checkArgument(registry != null, "registry cannot be null");

    this.registry = registry;
    lookupAllByTypeCache = Caffeine.newBuilder().build(registry::lookupAllByType);
    lookupByNameCache = Caffeine.newBuilder().build(registry::lookupByName);
  }

  @Override
  public Object invoke(Object target, Method method, Object[] args) throws Throwable {
    Method injectable = resolveInjectableMethod(target, method);

    if (injectable == null) {
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
      Object arg;
      Named named = parameter.getAnnotation(Named.class);
      if (named != null) {
        arg = lookupByNameCache.get(parameter.getAnnotation(Named.class).value())
            .orElseThrow(() -> new IllegalDependencyInjectionException(format(NO_OBJECT_FOUND_FOR_PARAM,
                                                                              parameter.getName(), injectable.getName(),
                                                                              target.toString())));
      } else {
        final Collection<?> lookupObjects = lookupAllByTypeCache.get(parameter.getType());
        arg = new PreferredObjectSelector().select(lookupObjects.iterator());
      }
      augmentedArgs.add(arg);
    }
    return augmentedArgs;
  }

  private Method resolveInjectableMethod(Object target, Method method) {
    Method candidate = null;

    for (Method serviceImplMethod : getImplementationDeclaredMethods(target)) {
      if (isPublic(serviceImplMethod.getModifiers())
          && serviceImplMethod.getName().equals(method.getName())
          && serviceImplMethod.getAnnotationsByType(Inject.class).length > 0
          && equivalentParams(method.getParameters(), serviceImplMethod.getParameters())) {
        if (candidate != null
            && !(candidate.getName().equals(serviceImplMethod.getName())
                && deepEquals(candidate.getParameterTypes(), serviceImplMethod.getParameterTypes()))) {
          throw new IllegalDependencyInjectionException(format(MANY_CANDIDATES_ERROR_MSG_TEMPLATE, method.getName(),
                                                               target.toString()));
        }
        candidate = serviceImplMethod;
      }
    }
    return candidate;
  }

  private Method[] getImplementationDeclaredMethods(Object object) {
    List<Method> methods = new LinkedList<>();
    Class<?> clazz = object.getClass();
    while (clazz != Object.class) {
      methods.addAll(asList(clazz.getDeclaredMethods()));
      clazz = clazz.getSuperclass();
    }

    return methods.toArray(new Method[methods.size()]);
  }

  private boolean equivalentParams(Parameter[] invocationParams, Parameter[] serviceImplParams) {
    if (serviceImplParams.length < invocationParams.length) {
      return false;
    }

    int i = 0;
    for (Parameter invocationParam : invocationParams) {
      if (!serviceImplParams[i].getType().equals(invocationParam.getType())) {
        return false;
      }
      ++i;
    }

    // Check that the remaining parameters are injectable
    for (int j = i; j < serviceImplParams.length; ++j) {
      if (!serviceImplParams[j].isAnnotationPresent(Named.class)
          && lookupAllByTypeCache.get(serviceImplParams[j].getType()).isEmpty()) {
        return false;
      }
    }

    return true;
  }
}
