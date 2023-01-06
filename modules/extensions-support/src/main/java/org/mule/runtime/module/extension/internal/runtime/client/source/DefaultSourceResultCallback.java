/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.source;

import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.core.internal.util.FunctionalUtils.withNullEvent;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.core.internal.execution.MessageProcessContext;
import org.mule.runtime.extension.api.client.source.SourceParameterizer;
import org.mule.runtime.extension.api.client.source.SourceResultCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionsFlowProcessingTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class DefaultSourceResultCallback<T, A> implements SourceResultCallback<T, A> {

  private final Result<T, A> result;
  private final ExtensionsFlowProcessingTemplate template;
  private final MessageProcessContext messageProcessContext;

  DefaultSourceResultCallback(Result<T, A> result,
                              ExtensionsFlowProcessingTemplate template,
                              MessageProcessContext messageProcessContext) {
    this.result = result;
    this.template = template;
    this.messageProcessContext = messageProcessContext;
  }

  @Override
  public Result<T, A> getResult() {
    return result;
  }

  @Override
  public CompletableFuture<Void> completeWithSuccess(Consumer<SourceParameterizer> successCallbackParameters) {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    try {
      withNullEvent(event -> {
        try {
          template.sendResponseToClient(event, null, new FutureCompletionCallback(future));
          template.afterPhaseExecution(right(event));
        } catch (Throwable t) {

          // this t? or a messaging exception? or whatever makes it to the error callback?
          future.completeExceptionally(t);
        }
          return null;
      });
    } catch (Throwable t) {
      future.completeExceptionally(t);
    }
    return future;
  }

  @Override
  public CompletableFuture<Void> completeWithError() {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    try {
      withNullEvent(event -> {
//        template.sendFailureResponseToClient(event, null, new FutureCompletionCallback(future));
        return null;
      });
    } catch (Throwable t) {
      future.completeExceptionally(t);
    }
    return future;
  }

  private class FutureCompletionCallback implements CompletableCallback<Void> {

    private final CompletableFuture<Void> future;

    private FutureCompletionCallback(CompletableFuture<Void> future) {
      this.future = future;
    }

    @Override
    public void complete(Void value) {
      future.complete(value);
    }

    @Override
    public void error(Throwable e) {
      future.completeExceptionally(e);
    }
  }
}
