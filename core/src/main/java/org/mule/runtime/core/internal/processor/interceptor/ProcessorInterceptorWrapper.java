/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class ProcessorInterceptorWrapper implements ComponentInterceptorAdapter {

  private static final String BEFORE_METHOD_NAME = "before";
  private static final String AFTER_METHOD_NAME = "after";
  private static final String AROUND_METHOD_NAME = "around";

  private final ProcessorInterceptor processorInterceptor;

  public ProcessorInterceptorWrapper(ProcessorInterceptor processorInterceptor) {
    this.processorInterceptor = processorInterceptor;
  }

  @Override
  public boolean implementsBeforeOrAfter() {
    try {
      return !(processorInterceptor.getClass()
          .getMethod(BEFORE_METHOD_NAME, ComponentLocation.class, Map.class, InterceptionEvent.class)
          .isDefault()
          && processorInterceptor.getClass()
              .getMethod(AFTER_METHOD_NAME, ComponentLocation.class, InterceptionEvent.class, Optional.class).isDefault());
    } catch (NoSuchMethodException | SecurityException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public boolean implementsAround() {
    try {
      return !processorInterceptor.getClass()
          .getMethod(AROUND_METHOD_NAME, ComponentLocation.class, Map.class, InterceptionEvent.class, InterceptionAction.class)
          .isDefault();
    } catch (NoSuchMethodException | SecurityException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public void before(ComponentLocation location, Map<String, ProcessorParameterValue> resolvedParams,
                     DefaultInterceptionEvent interceptionEvent) {
    processorInterceptor.before(location, resolvedParams, interceptionEvent);
  }

  @Override
  public CompletableFuture<InterceptionEvent> around(ComponentLocation location,
                                                     Map<String, ProcessorParameterValue> resolvedParams,
                                                     DefaultInterceptionEvent interceptionEvent,
                                                     ReactiveInterceptionAction reactiveInterceptionAction) {
    return processorInterceptor.around(location, resolvedParams, interceptionEvent, reactiveInterceptionAction);
  }

  @Override
  public void after(ComponentLocation location, DefaultInterceptionEvent interceptionEvent, Optional<Throwable> thrown) {
    processorInterceptor.after(location, interceptionEvent, thrown);
  }

  @Override
  public boolean isErrorMappingRequired(ComponentLocation location) {
    return processorInterceptor.isErrorMappingRequired(location);
  }

  @Override
  public ClassLoader getClassLoader() {
    return processorInterceptor.getClass().getClassLoader();
  }
}
