/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

public class AbstractCompletableMethodOperationExecutorTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("MULE-18124")
  @Description("Verify that when a non-blocking operation method throws an exception, the callback#error method is called only once.")
  public void nonBlockingThrowsException() throws NoSuchMethodException, SecurityException {
    final NullPointerException expected = new NullPointerException("Expected");

    final AbstractCompletableMethodOperationExecutor<ComponentModel> executor =
        new AbstractCompletableMethodOperationExecutor<ComponentModel>(mock(ComponentModel.class),
                                                                       this.getClass()
                                                                           .getDeclaredMethod("nonBlockingThrowsException"),
                                                                       null) {

          @Override
          protected void doExecute(ExecutionContext<ComponentModel> executionContext, ExecutorCallback callback) {
            final ExecutionContextAdapter context = (ExecutionContextAdapter) executionContext;
            context.setVariable(COMPLETION_CALLBACK_CONTEXT_PARAM,
                                new ExecutorCompletionCallbackAdapter(new PreservingThreadContextExecutorCallback(callback)));

            throw expected;
          }
        };

    final Map<String, Object> ctxVariables = new HashMap<>();
    final ExecutionContextAdapter ctx = mock(ExecutionContextAdapter.class);
    when(ctx.getVariable(any(String.class)))
        .thenAnswer(inv -> ctxVariables.get(inv.getArgument(0)));
    when(ctx.setVariable(any(String.class), any()))
        .thenAnswer(inv -> ctxVariables.put(inv.getArgument(0), inv.getArgument(1)));

    final ExecutorCallback callback = mock(ExecutorCallback.class);
    executor.execute(ctx, callback);

    verify(callback, times(1)).error(expected);
  }
}
