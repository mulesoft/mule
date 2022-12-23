/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.source;

import org.mule.runtime.core.internal.execution.FlowProcessTemplate;
import org.mule.runtime.core.internal.execution.MessageProcessContext;
import org.mule.runtime.extension.api.client.source.SourceResultCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.concurrent.CompletableFuture;

final class DefaultSourceResultCallback<T, A> implements SourceResultCallback<T, A> {

  private final Result<T, A> result;
  private final FlowProcessTemplate template;
  private final MessageProcessContext messageProcessContext;

  DefaultSourceResultCallback(Result<T, A> result,
                              FlowProcessTemplate template,
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
  public CompletableFuture<Void> completeWithSuccess() {
    return null;
  }

  @Override
  public CompletableFuture<Void> completeWithError() {
    return null;
  }
}
