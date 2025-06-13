/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.source;

import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.util.FunctionalUtils.withNullEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.evaluate;

import static java.util.Collections.emptyMap;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.extension.api.client.source.SourceCallbackParameterizer;
import org.mule.runtime.extension.api.client.source.SourceResultHandler;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionsFlowProcessingTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;

/**
 * Default implementation of {@link SourceResultHandler}
 *
 * @since 4.5.0
 */
final class DefaultSourceResultHandler<T, A> implements SourceResultHandler<T, A> {

  private static final Logger LOGGER = getLogger(DefaultSourceResultHandler.class);

  private final SourceClient sourceClient;
  private final Result<T, A> result;
  private final ExtensionsFlowProcessingTemplate template;
  private final ClassLoader extensionClassLoader;

  DefaultSourceResultHandler(SourceClient sourceClient,
                             Result<T, A> result,
                             ExtensionsFlowProcessingTemplate template) {
    this.sourceClient = sourceClient;
    this.result = result;
    this.template = template;

    extensionClassLoader = sourceClient.getExtensionClassLoader();
  }

  @Override
  public Result<T, A> getResult() {
    return result;
  }

  private void afterPhaseExecution(Either<MessagingException, CoreEvent> either, ClassLoader extensionClassLoader) {
    withContextClassLoader(extensionClassLoader, () -> template.afterPhaseExecution(either));
  }

  @Override
  public CompletableFuture<Void> completeWithSuccess(Consumer<SourceCallbackParameterizer> successCallbackParameters) {
    return withNullEvent(event -> {
      final CompletableFuture<Void> future = new CompletableFuture<>();
      future.whenComplete((v, t) -> {
        if (t != null) {
          LOGGER.atWarn()
              .setCause(t)
              .log("Failed to send success response to client: {}", t.getMessage());
          afterPhaseExecution(left(sourceClient.asMessagingException(t, event)), extensionClassLoader);
        } else {
          afterPhaseExecution(right(event), extensionClassLoader);
        }
      });

      try {
        Map<String, Object> params = resolveCallbackParameters(sourceClient.getSourceModel().getSuccessCallback(),
                                                               successCallbackParameters,
                                                               event);

        withContextClassLoader(extensionClassLoader,
                               () -> template.sendResponseToClient(event, params, new FutureCompletionCallback(future)));
      } catch (Throwable t) {
        future.completeExceptionally(t);
      }
      return future;
    });
  }

  @Override
  public CompletableFuture<Void> completeWithError(Throwable exception,
                                                   Consumer<SourceCallbackParameterizer> errorCallbackParameters) {
    final ClassLoader extensionClassLoader = sourceClient.getExtensionClassLoader();
    return withNullEvent(event -> {
      final MessagingException messagingException = sourceClient.asMessagingException(exception, event);
      final CompletableFuture<Void> future = new CompletableFuture<>();

      future.whenComplete((v, t) -> {
        if (t != null) {
          LOGGER.atWarn()
              .setCause(t)
              .log("Failed to send error response to client: {}", t.getMessage());
          afterPhaseExecution(left(sourceClient.asMessagingException(t, event)), extensionClassLoader);
        } else {
          afterPhaseExecution(left(messagingException), extensionClassLoader);
        }
      });

      try {
        Map<String, Object> params = resolveCallbackParameters(sourceClient.getSourceModel().getErrorCallback(),
                                                               errorCallbackParameters,
                                                               messagingException.getEvent());

        withContextClassLoader(extensionClassLoader, () -> template
            .sendFailureResponseToClient(messagingException, params, new FutureCompletionCallback(future)));
      } catch (Throwable t) {
        future.completeExceptionally(t);
      }
      return future;
    });
  }

  private Map<String, Object> resolveCallbackParameters(Optional<? extends ParameterizedModel> callbackModel,
                                                        Consumer<SourceCallbackParameterizer> parameterizerConsumer,
                                                        CoreEvent event) {
    DefaultSourceCallbackParameterizer parameterizer = new DefaultSourceCallbackParameterizer();
    parameterizerConsumer.accept(parameterizer);

    return callbackModel.map(model -> evaluate(sourceClient.toResolverSet(parameterizer, model),
                                               sourceClient.resolveConfigurationInstance(event),
                                               event))
        .orElse(emptyMap());
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
