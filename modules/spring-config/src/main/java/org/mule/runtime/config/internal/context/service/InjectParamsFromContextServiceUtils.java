/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.service;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Arrays.deepEquals;

import org.mule.runtime.core.api.registry.IllegalDependencyInjectionException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.LoadingCache;

import jakarta.inject.Inject;
import jakarta.inject.Named;

class InjectParamsFromContextServiceUtils {

  public static final String MANY_CANDIDATES_ERROR_MSG_TEMPLATE =
      "More than one invocation candidate for method '%s' in service '%s'";
  public static final String NO_OBJECT_FOUND_FOR_PARAM =
      "No object found in the registry for parameter '%s' of method '%s' in service '%s'";

  private InjectParamsFromContextServiceUtils() {
    // Use statically
  }

  static Object calculateAugmentedArgForParamerter(final Parameter parameter,
                                                   LoadingCache<String, Optional<?>> lookupByNameCache,
                                                   LoadingCache<Class<?>, Collection<?>> lookupAllByTypeCache,
                                                   Method injectable, String serviceName) {
    Named named = parameter.getAnnotation(Named.class);
    if (named != null) {
      return lookupByNameCache.get(named.value())
          .orElseThrow(() -> new IllegalDependencyInjectionException(format(NO_OBJECT_FOUND_FOR_PARAM,
                                                                            parameter.getName(), injectable.getName(),
                                                                            serviceName)));
    }

    // Still need to support javax.inject for the time being...
    javax.inject.Named javaxNamed = parameter.getAnnotation(javax.inject.Named.class);
    if (javaxNamed != null) {
      return lookupByNameCache.get(javaxNamed.value())
          .orElseThrow(() -> new IllegalDependencyInjectionException(format(NO_OBJECT_FOUND_FOR_PARAM,
                                                                            parameter.getName(), injectable.getName(),
                                                                            serviceName)));
    }

    return lookupAllByTypeCache.get(parameter.getType()).iterator().next();
  }

  static Method resolveInjectableMethodFor(Method method, Method serviceImplMethod,
                                           LoadingCache<Class<?>, Collection<?>> lookupAllByTypeCache,
                                           Method candidate, String serviceName) {
    if (isPublic(serviceImplMethod.getModifiers())
        && serviceImplMethod.getName().equals(method.getName())
        && (serviceImplMethod.getAnnotationsByType(Inject.class).length > 0
            // Still need to support javax.inject for the time being...
            || serviceImplMethod.getAnnotationsByType(javax.inject.Inject.class).length > 0)
        && equivalentParams(method.getParameters(), serviceImplMethod.getParameters(),
                            lookupAllByTypeCache)) {
      if (candidate != null
          && !(candidate.getName().equals(serviceImplMethod.getName())
              && deepEquals(candidate.getParameterTypes(), serviceImplMethod.getParameterTypes()))) {
        throw new IllegalDependencyInjectionException(format(MANY_CANDIDATES_ERROR_MSG_TEMPLATE, method.getName(),
                                                             serviceName));
      }
      return serviceImplMethod;
    }
    return candidate;
  }

  private static boolean equivalentParams(Parameter[] invocationParams, Parameter[] serviceImplParams,
                                          LoadingCache<Class<?>, Collection<?>> lookupAllByTypeCache) {
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
      if (!(serviceImplParams[j].isAnnotationPresent(Named.class)
          // Still need to support javax.inject for the time being...
          || serviceImplParams[j].isAnnotationPresent(javax.inject.Named.class))
          && lookupAllByTypeCache.get(serviceImplParams[j].getType()).isEmpty()) {
        return false;
      }
    }

    return true;
  }

}
