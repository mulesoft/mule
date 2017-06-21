/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.execution.ExecutionCallback;

public class ExecuteCallbackInterceptor<T> implements ExecutionInterceptor<T> {

  @Override
  public T execute(ExecutionCallback<T> callback, ExecutionContext executionContext) throws Exception {
    return callback.process();
  }
}
