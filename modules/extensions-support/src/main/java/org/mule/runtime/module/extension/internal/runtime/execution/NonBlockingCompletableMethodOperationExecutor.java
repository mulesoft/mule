/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.lang.reflect.Method;

/**
 * Implementation of {@link NonBlockingCompletableMethodOperationExecutor} which works by using a
 * {@link GeneratedMethodComponentExecutor}
 *
 * @since 4.3.0
 */
public class NonBlockingCompletableMethodOperationExecutor<M extends ComponentModel>
    extends AbstractCompletableMethodOperationExecutor<M> {

  public NonBlockingCompletableMethodOperationExecutor(M operationModel, Method operationMethod, Object operationInstance) {
    super(operationModel, operationMethod, operationInstance);
  }

  @Override
  protected void doExecute(ExecutionContext<M> executionContext, ExecutorCallback callback) {
    final ExecutionContextAdapter<M> context = (ExecutionContextAdapter<M>) executionContext;
    context.setVariable(COMPLETION_CALLBACK_CONTEXT_PARAM,
                        new ExecutorCompletionCallbackAdapter(new PreservingThreadContextExecutorCallback(callback)));

    executor.execute(executionContext);
  }

}
