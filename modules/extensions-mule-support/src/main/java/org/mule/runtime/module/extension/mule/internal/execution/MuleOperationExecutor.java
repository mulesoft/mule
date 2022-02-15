/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.execution;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext.from;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.Map;

import javax.inject.Inject;

public class MuleOperationExecutor implements CompletableComponentExecutor<ComponentModel>, Initialisable {

  private final ComponentModel operationModel;

  private Operation operation;

  @Inject
  private MuleContext muleContext;

  public MuleOperationExecutor(ComponentModel operationModel) {
    this.operationModel = operationModel;
  }

  @Override
  public void initialise() throws InitialisationException {
    operation = (Operation) new DefaultRegistry(muleContext).lookupByName(operationModel.getName())
        .orElseThrow(() -> new InitialisationException(createStaticMessage(
                                                                           format("Operation '%s' not found in registry",
                                                                                  operationModel.getName())),
                                                       this));
  }

  @Override
  public void execute(ExecutionContext<ComponentModel> executionContext, ExecutorCallback callback) {
    ExecutionContextAdapter<ComponentModel> ctx = (ExecutionContextAdapter<ComponentModel>) executionContext;
    final CoreEvent inputEvent = ctx.getEvent();

    CoreEvent executionEvent = builder(inputEvent)
        .parameters(buildOperationParameters(inputEvent, ctx))
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

  private Map<String, ?> buildOperationParameters(CoreEvent inputEvent, ExecutionContextAdapter<ComponentModel> ctx) {
    return from(inputEvent)
        .getOperationExecutionParams(ctx.getComponent().getLocation(), inputEvent.getContext().getId())
        .getParameters();
  }
}
