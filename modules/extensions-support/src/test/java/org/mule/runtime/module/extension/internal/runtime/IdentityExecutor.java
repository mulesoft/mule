/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedExecutionContext;

import java.util.Map;

/**
 * A {@link CompletableComponentExecutor} which just completes immediately with the same input event.
 */
class IdentityExecutor<T extends ComponentModel> implements CompletableComponentExecutor<T> {

  /**
   * Factory method which can be used as a method reference for a {@link CompletableComponentExecutorFactory}.
   *
   * @param componentModel unused.
   * @param parameters     unused.
   * @return a new {@link IdentityExecutor} instance.
   */
  public static <Y extends ComponentModel> IdentityExecutor<Y> create(Y componentModel, Map<String, Object> parameters) {
    return new IdentityExecutor<>();
  }

  @Override
  public void execute(ExecutionContext executionContext, ExecutorCallback executorCallback) {
    executorCallback.complete(((EventedExecutionContext<?>) executionContext).getEvent());
  }
}
