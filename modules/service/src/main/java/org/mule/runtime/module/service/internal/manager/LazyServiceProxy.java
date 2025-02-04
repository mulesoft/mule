/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.manager;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.getInvocationHandler;
import static java.lang.reflect.Proxy.isProxyClass;
import static java.lang.reflect.Proxy.newProxyInstance;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.module.artifact.api.classloader.DisposableClassLoader;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.api.manager.ServiceProxyInvocationHandler;
import org.mule.runtime.module.service.api.manager.ServiceRegistry;
import org.mule.runtime.module.service.internal.discoverer.LazyServiceAssembly;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

/**
 * A {@link Service} proxy which allows to defer the actual load and creation of the service until the first invokation of one of
 * its contract methods. Lifecycle will also be applied lazily.
 * <p>
 * Use in tandem with the {@link LazyServiceAssembly} for a truly lazy effect.
 *
 * @since 4.2
 */
public class LazyServiceProxy implements ServiceProxyInvocationHandler {

  private static final Logger LOGGER = getLogger(LazyServiceProxy.class);
  private static final Logger SPLASH_LOGGER = getLogger("org.mule.runtime.core.internal.logging");

  private final ServiceAssembly assembly;
  private final DefaultServiceRegistry serviceRegistry;
  private final LazyValue<Service> service;

  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean stopped = new AtomicBoolean(false);
  private MethodInvoker methodInvoker = new DefaultMethodInvoker();

  /**
   * Creates a new proxy based on the given {@code assembly} and {@code serviceRegistry}
   *
   * @param assembly        the {@link ServiceAssembly}
   * @param serviceRegistry the {@link DefaultServiceRegistry}
   * @return a new {@link Service} proxy
   */
  public static Service from(ServiceAssembly assembly, ServiceRegistry serviceRegistry, Injector containerInjector) {
    return proxy(assembly, new LazyServiceProxy(assembly, (DefaultServiceRegistry) serviceRegistry, containerInjector));
  }

  private static Service proxy(ServiceAssembly assembly, InvocationHandler handler) {
    final Class<? extends Service> contract = resolveContract(assembly);
    return (Service) newProxyInstance(contract.getClassLoader(),
                                      new Class[] {contract, Startable.class, Stoppable.class},
                                      handler);
  }

  private static Class<? extends Service> resolveContract(ServiceAssembly assembly) {
    return assembly.getServiceContract();
  }

  protected LazyServiceProxy(ServiceAssembly assembly, DefaultServiceRegistry serviceRegistry, Injector containerInjector) {
    this.assembly = assembly;
    this.serviceRegistry = serviceRegistry;
    this.service = new LazyValue<>(createService(containerInjector));
  }

  protected LazyServiceProxy(ServiceAssembly assembly,
                             DefaultServiceRegistry serviceRegistry,
                             LazyValue<Service> service) {
    this.assembly = assembly;
    this.serviceRegistry = serviceRegistry;
    this.service = service;
  }

  /**
   * Creates a new proxy {@link Service} equivalent to this one, but to be used in the context of a deployed application.
   *
   * @param methodInvoker The {@link MethodInvoker} to use
   * @return a new application specific proxy
   */
  public Service forApplication(MethodInvoker methodInvoker) {
    return proxy(assembly, new LazyServiceProxyApplicationDecorator(assembly, serviceRegistry, service, methodInvoker));
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    final Class<?> methodClass = method.getDeclaringClass();
    if (methodClass == Object.class && !method.getName().equals("toString")) {
      return method.invoke(this, args);
    } else if (methodClass == NamedObject.class) {
      return assembly.getName();
    } else if (methodClass == Service.class) {
      return assembly.getServiceContract().getSimpleName();
    } else if (methodClass == Startable.class) {
      return handleStart();
    } else if (methodClass == Stoppable.class) {
      return handleStop();
    } else {
      return methodInvoker.invoke(getService(), method, args);
    }
  }

  protected void setMethodInvoker(MethodInvoker methodInvoker) {
    this.methodInvoker = methodInvoker;
  }

  private CheckedSupplier<Service> createService(Injector containerInjector) {
    return () -> {
      Service serviceInstance = withServiceClassLoader(() -> instantiateService(containerInjector));
      if (started.compareAndSet(false, true)) {
        doStart(serviceInstance);
        stopped.set(false);
      }
      return serviceInstance;
    };
  }

  private Service instantiateService(Injector containerInjector) throws ServiceResolutionError, MuleException {
    ServiceProvider provider = assembly.getServiceProvider();
    serviceRegistry.inject(provider);
    Service serviceInstance = provider.getServiceDefinition().getService();
    if (containerInjector != null) {
      containerInjector.inject(serviceInstance);
    }
    return serviceInstance;
  }

  protected synchronized Object handleStart() {
    if (service.isComputed() && started.compareAndSet(false, true)) {
      doStart(getService());
      stopped.set(false);
    }
    return null;
  }

  protected Object handleStop() {
    if (stopped.compareAndSet(false, true) && service.isComputed()) {
      doStop();
      started.set(false);
    }

    return null;
  }

  private synchronized void doStart(Service service) {
    withServiceClassLoader(() -> {
      startIfNeeded(service);
      String splash = service.getSplashMessage();
      if (isNotEmpty(splash)) {
        SPLASH_LOGGER.info(new ServiceSplashScreen(service.toString(), splash).toString());
      }
    });
  }

  private synchronized void doStop() {
    withServiceClassLoader(() -> {
      try {
        stopIfNeeded(getService());
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

  @Override
  public boolean equals(Object obj) {
    if (obj != null && isProxyClass(obj.getClass())) {
      return this == getInvocationHandler(obj);
    }

    return false;
  }

  @Override
  public Service getService() {
    return service.get();
  }
}
