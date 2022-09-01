/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameterizer;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.internal.client.InternalOperationParameters;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.client.InternalOperationParameterizer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * An argument resolver that yields instances of {@link ExtensionsClient}.
 *
 * @since 4.0
 */
public class ExtensionsClientArgumentResolver implements ArgumentResolver<ExtensionsClient> {

  private final ExtensionsClient extensionsClient;

  public ExtensionsClientArgumentResolver(ExtensionsClient extensionsClient) {
    this.extensionsClient = extensionsClient;
  }

  @Override
  public ExtensionsClient resolve(ExecutionContext executionContext) {
    return new EventedExtensionsClientDecorator((ExecutionContextAdapter) executionContext);
  }

  private class EventedExtensionsClientDecorator implements ExtensionsClient {

    private final ExecutionContextAdapter executionContext;

    private EventedExtensionsClientDecorator(ExecutionContextAdapter executionContext) {
      this.executionContext = executionContext;
    }

    @Override
    public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extension, String operation, OperationParameters parameters) {
      setEvent(parameters);
      return extensionsClient.executeAsync(extension, operation, parameters);
    }

    @Override
    public <T, A> Result<T, A> execute(String extension, String operation, OperationParameters parameters) throws MuleException {
      setEvent(parameters);
      return extensionsClient.execute(extension, operation, parameters);
    }

    @Override
    public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extension,
                                                               String operation,
                                                               Consumer<OperationParameterizer> parameters) {
      return extensionsClient.executeAsync(extension, operation, parameterizer -> {
        parameters.accept(parameterizer);
        if (parameterizer instanceof InternalOperationParameterizer) {
          if (!((InternalOperationParameterizer) parameterizer).getContextEvent().isPresent()) {
            parameterizer.inTheContextOf(executionContext.getEvent());
          }
        }
      });
    }

    private void setEvent(OperationParameters parameters) {
      if (parameters instanceof InternalOperationParameters) {
        ((InternalOperationParameters) parameters).setContextEvent(executionContext.getEvent());
      }
    }
  }
}
