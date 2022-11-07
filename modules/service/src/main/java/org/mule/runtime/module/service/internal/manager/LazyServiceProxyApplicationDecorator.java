/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.manager;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.internal.util.MethodInvoker;
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
