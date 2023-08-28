/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.manager;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;

/**
 * A subclass of {@link LazyServiceProxy} ment to be used in the context of a deployed application.
 *
 * This subclass uses an alternate {@link MethodInvoker} while also ignores all lifecycle invocations.
 *
 * @since 4.2
 */
public class LazyServiceProxyApplicationDecorator extends LazyServiceProxy {


  public LazyServiceProxyApplicationDecorator(ServiceAssembly assembly,
                                              DefaultServiceRegistry serviceRegistry,
                                              LazyValue<Service> service,
                                              MethodInvoker methodInvoker) {
    super(assembly, serviceRegistry, service);
    setMethodInvoker(methodInvoker);
  }

  @Override
  protected Object handleStart() {
    return null;
  }

  @Override
  protected Object handleStop() {
    return null;
  }
}
