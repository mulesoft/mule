/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Arrays.asList;
import static java.util.Arrays.deepEquals;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.findImplementedInterfaces;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.container.api.MetadataInvocationHandler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.IllegalDependencyInjectionException;
import org.mule.runtime.core.internal.config.preferred.PreferredObjectSelector;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Proxies a {@link Service} instance to automatically {@link Inject} parameters for invocations of implementation methods.
 *
 * @since 4.0
 */
public class InjectParamsFromContextServiceProxy extends MetadataInvocationHandler<Service> {

  public static final String MANY_CANDIDATES_ERROR_MSG_TEMPLATE =
      "More than one invocation candidate for for method '%s' in service '%s'";
  public static final String NO_OBJECT_FOUND_FOR_PARAM =
      "No object found in the registry for parameter '%s' of method '%s' in service '%s'";

  private final Registry registry;

  /**
   * Creates a new proxy for the provided service instance.
   *
   * @param service service instance to wrap. Non null.
   * @param context the {@link MuleContext} to use for resolving injectable parameters. Non null.
   */
  public InjectParamsFromContextServiceProxy(Service service, Registry registry) {
    super(service);
    checkArgument(registry != null, "context cannot be null");
    this.registry = registry;
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
        Object arg;
        if (parameter.isAnnotationPresent(Named.class)) {
          arg = registry.lookupByName(parameter.getAnnotation(Named.class).value())
              .orElseThrow(() -> new IllegalDependencyInjectionException(format(NO_OBJECT_FOUND_FOR_PARAM,
                                                                                parameter.getName(), injectable.getName(),
                                                                                getProxiedObject().getName())));
        } else {
          final Collection<?> lookupObjects = registry.lookupAllByType(parameter.getType());
          arg = new PreferredObjectSelector().select(lookupObjects.iterator());
        }
        augmentedArgs.add(arg);
      }

      return doInvoke(proxy, injectable, augmentedArgs.toArray());
    }
  }

  private Method resolveInjectableMethod(Method method) {
    Method candidate = null;

    for (Method serviceImplMethod : getImplementationDeclaredMethods()) {
      if (isPublic(serviceImplMethod.getModifiers())
          && serviceImplMethod.getName().equals(method.getName())
          && serviceImplMethod.getAnnotationsByType(Inject.class).length > 0
          && equivalentParams(method.getParameters(), serviceImplMethod.getParameters())) {
        if (candidate != null
            && !(candidate.getName().equals(serviceImplMethod.getName())
                && deepEquals(candidate.getParameterTypes(), serviceImplMethod.getParameterTypes()))) {
          throw new IllegalDependencyInjectionException(format(MANY_CANDIDATES_ERROR_MSG_TEMPLATE, method.getName(),
                                                               getProxiedObject().getName()));
        }
        candidate = serviceImplMethod;
      }
    }
    return candidate;
  }

  private boolean equivalentParams(Parameter[] invocationParams, Parameter[] serviceImplParams) {
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
          && registry.lookupAllByType(serviceImplParams[j].getType()).isEmpty()) {
        return false;
      }
    }

    return true;
  }

  /**
   * Creates a proxy for the provided service instance.
   *
   * @param service service to wrap. Non null.
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
