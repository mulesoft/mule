/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
