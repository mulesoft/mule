/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import static java.lang.String.format;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Implements {@link ServiceProviderResolutionHelper} using reflection.
 */
public class ReflectionServiceProviderResolutionHelper implements ServiceProviderResolutionHelper {

  @Override
  public void injectInstance(ServiceProvider serviceProvider, Collection<ServiceDefinition> resolvedServices)
      throws ServiceResolutionError {
    for (Field field : getAllFields(serviceProvider.getClass(), withAnnotation(Inject.class))) {
      Class<?> dependencyType = field.getType();
      final Object dependency = lookupService(resolvedServices, dependencyType);
      if (dependency == null) {
        throw new ServiceResolutionError(format("Cannot find a service to inject into field '%s' of service provider '%s'",
                                                field.getName(), serviceProvider.getClass().getName()));
      }
      try {
        field.setAccessible(true);
        field.set(serviceProvider, dependency);
      } catch (Exception e) {
        throw new ServiceResolutionError(format("Could not inject dependency on field %s of type %s", field.getName(),
                                                dependencyType.getClass().getName()),
                                         e);
      }
    }
  }

  private Object lookupService(Collection<ServiceDefinition> muleServices, Class<?> dependencyType) {
    for (ServiceDefinition muleService : muleServices) {
      if (muleService.getServiceClass().equals(dependencyType)) {
        return muleService.getService();
      }
    }

    return null;
  }


  @Override
  public List<Class<? extends Service>> findServiceDependencies(ServiceProvider serviceProvider) {
    final List<Class<? extends Service>> result = getAllFields(serviceProvider.getClass(), withAnnotation(Inject.class)).stream()
        .map(f -> (Class<? extends Service>) f.getType()).collect(Collectors.toCollection(LinkedList::new));

    result.forEach(clazz -> {
      if (!Service.class.isAssignableFrom(clazz)) {
        throw new IllegalArgumentException("Service providers can depend on Service instances only, but found "
            + clazz.getName());
      }
    });

    return result;
  }
}
