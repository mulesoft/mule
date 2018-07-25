/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.manager;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.module.artifact.api.classloader.DisposableClassLoader;
import org.mule.runtime.module.service.api.discoverer.ServiceLocator;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

public class LazyServiceProxy implements InvocationHandler {

  private static final Logger LOGGER = getLogger(LazyServiceProxy.class);

  private final ServiceLocator serviceLocator;
  private final ServiceRegistry serviceRegistry;
  private final LazyValue<Service> service;

  private boolean started = false;
  private boolean stopped = false;

  public LazyServiceProxy(ServiceLocator serviceLocator, ServiceRegistry serviceRegistry) {
    this.serviceLocator = serviceLocator;
    this.serviceRegistry = serviceRegistry;
    service = new LazyValue<>(createService());
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getDeclaringClass() == Startable.class) {
      return handleStart();
    } else if (method.getDeclaringClass() == Stoppable.class) {
      return handleStop();
    } else {
      return method.invoke(service.get(), args);
    }
  }

  private CheckedSupplier<Service> createService() {
    return () -> {
      Service service = withServiceClassLoader(() -> instantiateService());

      if (started) {
        doStart(service);
      }

      return service;
    };
  }

  private Service instantiateService() throws ServiceResolutionError {
    ServiceProvider provider = serviceLocator.getServiceProvider();
    serviceRegistry.inject(provider);
    Service service = provider.getServiceDefinition().getService();

    return service;
  }

  private Object handleStart() {
    stopped = false;
    if (!started) {
      started = true;
      if (service.isComputed()) {
        doStart(service.get());
      }
    }
    return null;
  }

  private Object handleStop() {
    started = false;
    if (!stopped) {
      stopped = true;
      if (service.isComputed()) {
        doStop();
      }
    }
    return null;
  }

  private void doStart(Service service) {
    withServiceClassLoader(() -> {
      startIfNeeded(service);
      String splash = service.getSplashMessage();
      if (isNotEmpty(splash)) {
        log(new ServiceSplashScreen(service.toString(), splash).toString());
      }
    });
  }

  private void doStop() {
    withServiceClassLoader(() -> {
      try {
        stopIfNeeded(service.get());
      } catch (Exception e) {
        LOGGER.warn(format("Service '%s' was not stopped properly: %s", serviceLocator.getName(), e.getMessage()), e);
      }

      if (serviceLocator.getClassLoader() instanceof DisposableClassLoader) {
        try {
          ((DisposableClassLoader) serviceLocator.getClassLoader()).dispose();
        } catch (Exception e) {
          LOGGER
              .warn(format("Service '%s' class loader was not stopped properly: %s", serviceLocator.getName(), e.getMessage()),
                    e);
        }

      }
    });
  }

  private void withServiceClassLoader(CheckedRunnable task) {
    withContextClassLoader(serviceLocator.getClassLoader(), task);
  }

  private <T> T withServiceClassLoader(Callable<T> callable) {
    return withContextClassLoader(serviceLocator.getClassLoader(), callable);
  }
}
