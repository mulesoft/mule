/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.client.strategy.ExtensionsClientProcessorsStrategy;
import org.mule.runtime.module.extension.internal.runtime.client.strategy.ExtensionsClientProcessorsStrategyFactory;
import org.mule.runtime.module.extension.internal.runtime.operation.OperationMessageProcessor;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;


/**
 * This is the default implementation for a {@link ExtensionsClient}, it uses the {@link ExtensionManager} in the
 * {@link MuleContext} to search for the extension that wants to execute the operation from.
 * <p>
 * The concrete execution of the operation is handled by an {@link OperationMessageProcessor} instance.
 * <p>
 * This implementation can only execute extensions that were built using the SDK, Smart Connectors operations can't be executed.
 *
 * @since 4.0
 */
public final class DefaultExtensionsClient implements ExtensionsClient, Initialisable {

  @Inject
  private ExtensionsClientProcessorsStrategyFactory extensionsClientProcessorsStrategyFactory;

  private final CoreEvent event;

  private ExtensionsClientProcessorsStrategy extensionsClientProcessorsStrategy;

  /**
   * This constructor enables the {@link DefaultExtensionsClient} to be aware of the current execution {@link CoreEvent} and
   * enables to perform the dynamic operation execution with the same event that the SDK operation using the
   * {@link ExtensionsClient} receives.
   *
   * @param event the current execution event.
   * @param extensionsClientProcessorsStrategyFactory the factory used to get the appropriate operation message processor strategy
   */
  public DefaultExtensionsClient(CoreEvent event,
                                 ExtensionsClientProcessorsStrategyFactory extensionsClientProcessorsStrategyFactory) {
    this.event = event;
    this.extensionsClientProcessorsStrategyFactory = extensionsClientProcessorsStrategyFactory;
  }

  /**
   * Creating a client from this constructor will enable the execution of operations with an initializer event.
   */
  public DefaultExtensionsClient() {
    this.event = null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A> CompletableFuture<Result<T, A>> executeAsync(String extension, String operation, OperationParameters parameters) {
    OperationMessageProcessor processor =
        extensionsClientProcessorsStrategy.getOperationMessageProcessor(extension, operation, parameters);
    final CoreEvent eventFromParams = extensionsClientProcessorsStrategy.getEvent(parameters);
    return just(eventFromParams)
        .transform(processor)
        .map(event -> Result.<T, A>builder(event.getMessage()).build())
        .onErrorMap(t -> {
          Throwable unwrapped = unwrap(t);
          if (unwrapped instanceof MessagingException) {
            return unwrapped;
          } else {
            return new MessagingException(eventFromParams, unwrapped, processor);
          }
        })
        .doAfterTerminate(() -> extensionsClientProcessorsStrategy.disposeProcessor(processor))
        .toFuture();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, A> Result<T, A> execute(String extension, String operation, OperationParameters params)
      throws MuleException {
    OperationMessageProcessor processor =
        extensionsClientProcessorsStrategy.getOperationMessageProcessor(extension, operation, params);
    final CoreEvent eventFromParams = extensionsClientProcessorsStrategy.getEvent(params);
    try {
      CoreEvent process = processor.process(eventFromParams);
      return Result.<T, A>builder(process.getMessage()).build();
    } catch (Exception e) {
      Throwable unwrapped = unwrap(e);
      if (unwrapped instanceof MessagingException) {
        throw (MessagingException) unwrapped;
      } else {
        throw new MessagingException(eventFromParams, unwrapped, processor);
      }
    } finally {
      extensionsClientProcessorsStrategy.disposeProcessor(processor);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    this.extensionsClientProcessorsStrategy = extensionsClientProcessorsStrategyFactory.create(event);
  }
}
