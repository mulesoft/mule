/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.execution;

import static org.mule.runtime.core.api.event.CoreEvent.builder;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.Map;

public class MuleOperationExecutor implements CompletableComponentExecutor<OperationModel> {

  private final Operation operation;

  public MuleOperationExecutor(Operation operation) {
    this.operation = operation;
  }

  @Override
  public void execute(ExecutionContext<OperationModel> executionContext, ExecutorCallback callback) {
    ExecutionContextAdapter<OperationModel> ctx = (ExecutionContextAdapter<OperationModel>) executionContext;
    final CoreEvent inputEvent = ctx.getEvent();

    CoreEvent executionEvent = builder(inputEvent)
        .parameters(buildOperationParameters(ctx))
        .build();

    operation.execute(executionEvent).whenComplete((event, exception) -> {
      if (exception != null) {
        callback.error(exception);
      } else {
        callback.complete(builder(inputEvent)
            .message(event.getMessage())
            .build());
      }
    });
  }

  private Map<String, TypedValue<?>> buildOperationParameters(ExecutionContextAdapter<OperationModel> ctx) {
//    SdkInternalContext sdkCtx = SdkInternalContext.from(event);
//    Map<String, Object> params = sdkCtx.getOperationExecutionParams(chainLocation, event.getContext().getId()).getParameters();
    return CaseInsensitiveHashMap.emptyCaseInsensitiveMap();
  }
}
