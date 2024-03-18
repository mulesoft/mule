/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.FlowInterceptor;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class FlowInterceptorWrapper implements ComponentInterceptorAdapter {

  private static final String BEFORE_METHOD_NAME = "before";
  private static final String AFTER_METHOD_NAME = "after";
  private static final String AROUND_METHOD_NAME = "around";

  private final FlowInterceptor flowInterceptor;

  public FlowInterceptorWrapper(FlowInterceptor flowInterceptor) {
    this.flowInterceptor = flowInterceptor;
  }

  @Override
  public boolean implementsBeforeOrAfter() {
    try {
      return !(flowInterceptor.getClass()
          .getMethod(BEFORE_METHOD_NAME, String.class, InterceptionEvent.class).isDefault()
          && flowInterceptor.getClass()
              .getMethod(AFTER_METHOD_NAME, String.class, InterceptionEvent.class, Optional.class).isDefault());
    } catch (NoSuchMethodException | SecurityException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public boolean implementsAround() {
    try {
      return !flowInterceptor.getClass()
          .getMethod(AROUND_METHOD_NAME, String.class, InterceptionEvent.class, InterceptionAction.class)
          .isDefault();
    } catch (NoSuchMethodException | SecurityException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public void before(ComponentLocation location, Map<String, ProcessorParameterValue> resolvedParams,
                     DefaultInterceptionEvent interceptionEvent) {
    flowInterceptor.before(location.getRootContainerName(), interceptionEvent);
  }

  @Override
  public CompletableFuture<InterceptionEvent> around(ComponentLocation location,
                                                     Map<String, ProcessorParameterValue> resolvedParams,
                                                     DefaultInterceptionEvent interceptionEvent,
                                                     ReactiveInterceptionAction reactiveInterceptionAction) {
    return flowInterceptor.around(location.getRootContainerName(), interceptionEvent, reactiveInterceptionAction);
  }

  @Override
  public void after(ComponentLocation location, DefaultInterceptionEvent interceptionEvent, Optional<Throwable> thrown) {
    flowInterceptor.after(location.getRootContainerName(), interceptionEvent, thrown);
  }

  @Override
  public ClassLoader getClassLoader() {
    return flowInterceptor.getClass().getClassLoader();
  }
}
