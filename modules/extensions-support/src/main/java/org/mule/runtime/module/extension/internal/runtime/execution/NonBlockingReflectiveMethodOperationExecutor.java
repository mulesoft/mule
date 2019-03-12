/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.lang.reflect.Method;

/**
 * Implementation of {@link NonBlockingReflectiveMethodOperationExecutor} which works by using reflection to invoke a method from a class.
 *
 * @since 4.2.0
 */
public class NonBlockingReflectiveMethodOperationExecutor<M extends ComponentModel>
    extends AbstractReflectiveMethodOperationExecutor<M> {


  public NonBlockingReflectiveMethodOperationExecutor(M operationModel, Method operationMethod, Object operationInstance) {
    super(operationModel, operationMethod, operationInstance);
  }

  protected void doExecute(ExecutionContext<M> executionContext, ExecutorCallback callback) {
    final ExecutionContextAdapter<M> context = (ExecutionContextAdapter<M>) executionContext;
    final CompletionCallbackFacade facade = new CompletionCallbackFacade(callback);

    context.setVariable(COMPLETION_CALLBACK_CONTEXT_PARAM, facade);

    executor.execute(executionContext);
  }

}
