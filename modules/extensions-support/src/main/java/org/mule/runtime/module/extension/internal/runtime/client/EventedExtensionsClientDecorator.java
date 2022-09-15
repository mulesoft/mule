/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameterizer;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.client.operation.EventedOperationsParameterDecorator;
import org.mule.runtime.module.extension.internal.runtime.client.operation.InternalOperationParameterizer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Decorates an {@link ExtensionsClient} instance so that all its operations are executed in the context of a given
 * {@link #event}, unless another context event has been explicitly defined.
 *
 * @since 4.5.0
 */
public class EventedExtensionsClientDecorator implements ExtensionsClient {

  private final ExtensionsClient extensionsClient;
  private final CoreEvent event;

  public EventedExtensionsClientDecorator(ExtensionsClient extensionsClient, CoreEvent event) {
    this.extensionsClient = extensionsClient;
    this.event = event;
  }

  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extension, String operation,
                                                             OperationParameters parameters) {
    return extensionsClient.executeAsync(extension, operation, new EventedOperationsParameterDecorator(parameters, event));
  }

  @Override
  public <T, A> Result<T, A> execute(String extension, String operation, OperationParameters parameters) throws MuleException {
    return extensionsClient.execute(extension, operation, new EventedOperationsParameterDecorator(parameters, event));
  }

  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extension,
                                                             String operation,
                                                             Consumer<OperationParameterizer> parameters) {
    return extensionsClient.executeAsync(extension, operation, parameterizer -> {
      parameters.accept(parameterizer);
      if (parameterizer instanceof InternalOperationParameterizer) {
        if (!((InternalOperationParameterizer) parameterizer).getContextEvent().isPresent()) {
          parameterizer.inTheContextOf(event);
        }
      }
    });
  }
}
