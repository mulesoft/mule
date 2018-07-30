/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.manager;

import static java.lang.String.format;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;

/**
 * Keeps track of {@link Service} implementations and is capable of injecting them into {@link ServiceProvider} instances
 * with fields annotated with {@link Inject}. Method injection as {@link Qualifier} and {@link Named} annotations are not
 * supported.
 *
 * @since 4.2
 */
public class ServiceRegistry {

  private final Map<Class<? extends Service>, Service> services = new HashMap<>();

  /**
   * Injects the tracked {@link Service services} into the given {@code serviceProvider}
   *
   * @param serviceProvider the injection target
   * @throws ServiceResolutionError if a dependency could not be injected
   */
  public void inject(ServiceProvider serviceProvider) throws ServiceResolutionError {
    for (Field field : getAllFields(serviceProvider.getClass(), withAnnotation(Inject.class))) {
      final Object dependency = lookup(field.getType());
      if (dependency == null) {
        throw new ServiceResolutionError(format("Cannot find a service to inject into field '%s' of service provider '%s'",
                                                field.getName(), serviceProvider.getClass().getName()));
      }

      try {
        field.setAccessible(true);
        field.set(serviceProvider, dependency);
      } catch (Exception e) {
        throw new ServiceResolutionError(format("Could not inject dependency on field %s of type %s", field.getName(),
                                                dependency.getClass().getName()),
                                         e);
      }
    }
  }

  /**
   * Tracks the given {@code service}
   *
   * @param service  the {@link Service} to be tracked
   * @param assembly the {@code service}'s {@link ServiceAssembly}
   */
  public void register(Service service, ServiceAssembly assembly) {
    services.put(assembly.getServiceContract(), service);
  }

  private Object lookup(Class<?> type) {
    return services.entrySet().stream()
        .filter(entry -> type.isAssignableFrom(entry.getKey()))
        .findFirst()
        .map(Entry::getValue)
        .orElse(null);
  }
}
