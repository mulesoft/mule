/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.manager;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.logging.LogUtil.log;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.core.internal.util.DefaultMethodInvoker;
import org.mule.runtime.core.internal.util.HasMethodInvoker;
import org.mule.runtime.core.internal.util.MethodInvoker;
import org.mule.runtime.module.artifact.api.classloader.DisposableClassLoader;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.internal.discoverer.LazyServiceAssembly;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

/**
 * A {@link Service} proxy which allows to defer the actual load and creation of the service until the first invokation of
 * one of its contract methods. Lifecycle will also be applied lazily.
 * <p>
 * Use in tandem with the {@link LazyServiceAssembly} for a truly lazy effect.
 *
 * @since 4.2
 */
public class LazyServiceProxy implements InvocationHandler {

  private static final Logger LOGGER = getLogger(LazyServiceProxy.class);

  private final ServiceAssembly assembly;
  private final ServiceRegistry serviceRegistry;
  private final LazyValue<Service> service;

  private boolean started = false;
  private boolean stopped = false;
  private MethodInvoker methodInvoker = new DefaultMethodInvoker();

  /**
   * Creates a new proxy based on the given {@code assembly} and {@code serviceRegistry}
   *
   * @param assembly the {@link ServiceAssembly}
   * @param serviceRegistry the {@link ServiceRegistry}
   * @return a new {@link Service} proxy
   */
  public static Service from(ServiceAssembly assembly, ServiceRegistry serviceRegistry) {
    final Class<? extends Service> contract = assembly.getServiceContract();
    return (Service) newProxyInstance(contract.getClassLoader(),
                                      new Class[] {contract, Startable.class, Stoppable.class, HasMethodInvoker.class},
                                      new LazyServiceProxy(assembly, serviceRegistry));
  }

  private LazyServiceProxy(ServiceAssembly assembly, ServiceRegistry serviceRegistry) {
    this.assembly = assembly;
    this.serviceRegistry = serviceRegistry;
    service = new LazyValue<>(createService());
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    final Class<?> methodClass = method.getDeclaringClass();
    if (methodClass == HasMethodInvoker.class) {
      setMethodInvoker((MethodInvoker) args[0]);
      return null;
    } else if (methodClass == Object.class && !method.getName().equals("toString")) {
      return method.invoke(this, args);
    } else if (methodClass == NamedObject.class) {
      return assembly.getName();
    } else if (methodClass == Startable.class) {
      return handleStart();
    } else if (methodClass == Stoppable.class) {
      return handleStop();
    } else {
      return methodInvoker.invoke(service.get(), method, args);
    }
  }

  public void setMethodInvoker(MethodInvoker methodInvoker) {
    this.methodInvoker = methodInvoker;
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
    ServiceProvider provider = assembly.getServiceProvider();
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
        LOGGER.warn(format("Service '%s' was not stopped properly: %s", assembly.getName(), e.getMessage()), e);
      }

      if (assembly.getClassLoader() instanceof DisposableClassLoader) {
        try {
          ((DisposableClassLoader) assembly.getClassLoader()).dispose();
        } catch (Exception e) {
          LOGGER
              .warn(format("Service '%s' class loader was not stopped properly: %s", assembly.getName(), e.getMessage()),
                    e);
        }

      }
    });
  }

  private void withServiceClassLoader(CheckedRunnable task) {
    withContextClassLoader(assembly.getClassLoader(), task);
  }

  private <T> T withServiceClassLoader(Callable<T> callable) {
    return withContextClassLoader(assembly.getClassLoader(), callable);
  }
}
