/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

public class ProcessorInterceptorFactoryAdapter implements ComponentInterceptorFactoryAdapter {

  private final ProcessorInterceptorFactory interceptorFactory;

  public ProcessorInterceptorFactoryAdapter(ProcessorInterceptorFactory interceptorFactory) {
    this.interceptorFactory = interceptorFactory;
  }

  @Override
  public boolean isInterceptable(ReactiveProcessor component) {
    return component instanceof Component
        && ((Component) component).getLocation() != null;
  }

  @Override
  public boolean intercept(ComponentLocation componentLocation) {
    return interceptorFactory.intercept(componentLocation);
  }

  @Override
  public ComponentInterceptorAdapter get() {
    return new ProcessorInterceptorWrapper(interceptorFactory.get());
  }

}
