/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
