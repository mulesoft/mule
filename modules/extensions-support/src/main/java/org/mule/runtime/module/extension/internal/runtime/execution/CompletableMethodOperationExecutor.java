/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import java.lang.reflect.Method;

/**
 * Implementation of {@link CompletableMethodOperationExecutor} which works by using a {@link GeneratedMethodComponentExecutor}
 *
 * @since 4.3.0
 */
public class CompletableMethodOperationExecutor<M extends ComponentModel> extends AbstractCompletableMethodOperationExecutor<M> {


  public CompletableMethodOperationExecutor(M operationModel, Method operationMethod, Object operationInstance) {
    super(operationModel, operationMethod, operationInstance);
  }

  protected void doExecute(ExecutionContext<M> executionContext, ExecutorCallback callback) {
    callback.complete(executor.execute(executionContext));
  }
}
