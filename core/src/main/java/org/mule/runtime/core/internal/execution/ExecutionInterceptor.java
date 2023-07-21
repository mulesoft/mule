/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.execution.ExecutionCallback;

public interface ExecutionInterceptor<T> {

  /**
   * Executes the callback
   *
   * @param callback         callback to execute
   * @param executionContext information about the current execution context
   * @return the result of the callback
   * @throws Exception
   */
  T execute(ExecutionCallback<T> callback, ExecutionContext executionContext) throws Exception;

}
