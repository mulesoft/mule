/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.execution.ExecutionCallback;

public interface ExecutionInterceptor<T> {

  /**
   * Executes the callback
   *
   * @param callback callback to execute
   * @param executionContext information about the current execution context
   * @return the result of the callback
   * @throws Exception
   */
  T execute(ExecutionCallback<T> callback, ExecutionContext executionContext) throws Exception;

}
