/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.execution.interception;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.runtime.core.api.interception.ProcessorInterceptionManager;
import org.mule.runtime.core.api.interception.ProcessorInterceptorCallback;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link ProcessorInterceptionManager}.
 *
 * @since 4.0
 */
public class DefaultProcessorInterceptionManager implements ProcessorInterceptionManager {

  private Map<ComponentIdentifier, ProcessorInterceptorCallback> processorInterceptorCallbacks = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerInterceptionCallback(ComponentIdentifier componentIdentifier,
                                           ProcessorInterceptorCallback processorInterceptorCallback) {
    checkNotNull(componentIdentifier, "componentIdentifier not null");
    checkNotNull(processorInterceptorCallback, "processorInterceptorCallback not null");

    if (this.processorInterceptorCallbacks.containsKey(componentIdentifier)) {
      throw new IllegalStateException("There is already registered an " + ProcessorInterceptorCallback.class.getName()
          + " for componentIdentifier: " + componentIdentifier);
    }
    this.processorInterceptorCallbacks.put(componentIdentifier, processorInterceptorCallback);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ProcessorInterceptorCallback> retrieveInterceptorCallback(ComponentIdentifier componentIdentifier) {
    checkNotNull(componentIdentifier, "componentIdentifier not null");

    if (!this.processorInterceptorCallbacks.containsKey(componentIdentifier)) {
      return empty();
    }
    return of(this.processorInterceptorCallbacks.get(componentIdentifier));
  }

}
