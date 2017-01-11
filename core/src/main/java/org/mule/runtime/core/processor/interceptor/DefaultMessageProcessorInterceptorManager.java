/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.interceptor;

import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;

import org.mule.runtime.api.interception.InterceptionHandler;
import org.mule.runtime.core.api.interception.MessageProcessorInterceptorManager;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */
public class DefaultMessageProcessorInterceptorManager implements MessageProcessorInterceptorManager {

  private List<InterceptionHandler> interceptionHandlers = new ArrayList<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isInterceptionEnabled() {
    return !interceptionHandlers.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addInterceptionHandler(InterceptionHandler interceptionHandler) {
    checkNotNull(interceptionHandler, "interceptionHandler cannot be null");

    this.interceptionHandlers.add(interceptionHandler);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<InterceptionHandler> retrieveInterceptionHandlerChain() {
    return unmodifiableList(interceptionHandlers);
  }
}
