/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.manager;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.util.ReflectionUtilsPredicates.withAnnotation;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.api.manager.ServiceRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;

/**
 * Keeps track of {@link Service} implementations and is capable of injecting them into {@link ServiceProvider} instances with
 * fields annotated with {@link Inject}. Optionality is supported by using {@link Optional} type when declared a field with
 * {@link Inject}. Method injection as {@link Qualifier} and {@link Named} annotations are not supported.
 *
 * @since 4.2
 */
public class DefaultServiceRegistry implements ServiceRegistry {

  private final Map<Class<? extends Service>, Service> services = new HashMap<>();

  /**
   * Injects the tracked {@link Service services} into the given {@code serviceProvider}
   *
   * @param serviceProvider the injection target
   * @throws ServiceResolutionError if a dependency could not be injected
   */
  public void inject(ServiceProvider serviceProvider) throws ServiceResolutionError {
    for (Field field : getAllFields(serviceProvider.getClass(), withAnnotation(Inject.class))) {
      Class<?> dependencyType = field.getType();

      boolean asOptional = false;
      if (dependencyType.equals(Optional.class)) {
        Type type = ((ParameterizedType) (field.getGenericType())).getActualTypeArguments()[0];
        if (type instanceof ParameterizedType) {
          dependencyType = (Class<?>) ((ParameterizedType) type).getRawType();
        } else {
          dependencyType = (Class<?>) type;
        }
        asOptional = true;
      }

      try {
        field.setAccessible(true);
        Object dependency = resolveObjectToInject(dependencyType, asOptional);
        if (dependency != null) {
          field.set(serviceProvider, dependency);
        } else if (!asOptional) {
          throw new ServiceResolutionError(format("Cannot find a service to inject into field '%s#%s' of service provider '%s'",
                                                  field.getDeclaringClass().getName(),
                                                  field.getName(),
                                                  serviceProvider.getServiceDefinition().getServiceClass().getName()));
        }
      } catch (Exception e) {
        throw new ServiceResolutionError(format("Could not inject dependency on field '%s#%s' of type '%s'",
                                                field.getDeclaringClass().getName(),
                                                field.getName(),
                                                dependencyType.getName()),
                                         e);
      }
    }
    for (Method method : getAllMethods(serviceProvider.getClass(), withAnnotation(Inject.class))) {
      if (method.getParameters().length == 1) {
        try {
          Class<?> dependencyType = method.getParameterTypes()[0];

          boolean asOptional = false;
          if (dependencyType.equals(Optional.class)) {
            Type type = ((ParameterizedType) (method.getGenericParameterTypes()[0])).getActualTypeArguments()[0];
            if (type instanceof ParameterizedType) {
              dependencyType = (Class<?>) ((ParameterizedType) type).getRawType();
            } else {
              dependencyType = (Class<?>) type;
            }
            asOptional = true;
          }

          Object dependency = resolveObjectToInject(dependencyType, asOptional);
          if (dependency != null) {
            method.invoke(serviceProvider, dependency);
          } else if (!asOptional) {
            throw new ServiceResolutionError(format("Cannot find a service to inject into field '%s#%s' of service provider '%s'",
                                                    method.getDeclaringClass().getName(),
                                                    method.getName(),
                                                    serviceProvider.getServiceDefinition().getServiceClass().getName()));
          }
        } catch (Exception e) {
          throw new RuntimeException(format("Could not inject dependency on method %s of type %s", method.getName(),
                                            serviceProvider.getClass().getName()),
                                     e);
        }
      }

    }
  }

  private Object resolveObjectToInject(Class<?> dependencyType, boolean asOptional) {
    Object dependency = services.entrySet().stream()
        .filter(entry -> dependencyType.isAssignableFrom(entry.getKey()))
        .findFirst()
        .map(Entry::getValue)
        .orElse(null);
    return asOptional ? ofNullable(dependency) : dependency;
  }

  @Override
  public <S extends Service> void register(S service, Class<? extends S> serviceContract) {
    services.put(serviceContract, service);
  }

  public <S extends Service> void register(Class<? extends S> serviceContract, S service) {
    services.put(serviceContract, service);
  }

  @Override
  public <S extends Service> void unregister(Class<? extends S> serviceContract) {
    services.remove(serviceContract);
  }

  @Override
  public <S extends Service> Optional<S> getService(Class<? extends S> serviceInterface) {
    return services
        .values()
        .stream()
        .filter(serviceInterface::isInstance)
        .map(s -> (S) s)
        .findAny();
  }

  @Override
  public Collection<Service> getAllServices() {
    return new HashSet<>(services.values());
  }
}
