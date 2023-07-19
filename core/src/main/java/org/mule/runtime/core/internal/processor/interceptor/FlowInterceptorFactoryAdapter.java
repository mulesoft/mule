/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.FlowInterceptorFactory;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

public class FlowInterceptorFactoryAdapter implements ComponentInterceptorFactoryAdapter {

  private final FlowInterceptorFactory interceptorFactory;

  public FlowInterceptorFactoryAdapter(FlowInterceptorFactory interceptorFactory) {
    this.interceptorFactory = interceptorFactory;
  }

  @Override
  public boolean isInterceptable(ReactiveProcessor component) {
    return true;
  }

  @Override
  public boolean intercept(ComponentLocation componentLocation) {
    return componentLocation.getComponentIdentifier().getType() == FLOW
        && interceptorFactory.intercept(componentLocation.getRootContainerName());
  }

  @Override
  public ComponentInterceptorAdapter get() {
    return new FlowInterceptorWrapper(interceptorFactory.get());
  }
}
