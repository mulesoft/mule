/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Default implementatio of {@link FlowListener}.
 * <p>
 * It uses an {@link CoreEvent}'s response {@link Publisher} to suscribe to the event termination and execute the necessary logic.
 *
 * @since 4.0
 */
public class DefaultFlowListener implements FlowListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFlowListener.class);

  private final ExtensionModel extensionModel;
  private final OperationModel operationModel;

  private Consumer<Message> successConsumer;
  private Consumer<Exception> errorConsumer;
  private Runnable onComplete;

  /**
   * Creates a new instance
   *
   * @param event the event on which the operation is being executed.
   */
  public DefaultFlowListener(ExtensionModel extensionModel, OperationModel operationModel, CoreEvent event) {
    this.extensionModel = extensionModel;
    this.operationModel = operationModel;
    from(((BaseEventContext) event.getContext()).getResponsePublisher())
        .doOnSuccessOrError((responseEvent, t) -> onTerminate(responseEvent, t))
        .subscribe();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSuccess(Consumer<Message> handler) {
    assertNotNull(handler);
    successConsumer = handler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onError(Consumer<Exception> handler) {
    assertNotNull(handler);
    this.errorConsumer = handler;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onComplete(Runnable handler) {
    assertNotNull(handler);
    onComplete = handler;
  }

  private void onTerminate(CoreEvent event, Throwable error) {
    try {
      if (event != null && successConsumer != null) {
        try {
          successConsumer.accept(event.getMessage());
        } catch (Exception e) {
          LOGGER.warn("Operation " + operationModel.getName() + " from extension " + extensionModel.getName()
              + " threw exception while executing the onSuccess FlowListener", e);
        }
      } else if (error != null && errorConsumer != null) {
        Exception exception = error instanceof Exception ? (Exception) error : new MessagingException(event, error);
        try {
          errorConsumer.accept(exception);
        } catch (Exception e) {
          LOGGER.warn("Operation " + operationModel.getName() + " from extension " + extensionModel.getName()
              + " threw exception while executing the onError FlowListener", e);
        }
      }
    } finally {
      if (onComplete != null) {
        try {
          onComplete.run();
        } catch (Exception e) {
          LOGGER.warn("Operation " + operationModel.getName() + " from extension " + extensionModel.getName()
              + " threw exception while executing the onComplete FlowListener", e);
        }
      }
    }
  }

  private void assertNotNull(Object handler) {
    checkArgument(handler != null, "Cannot set null handler");
  }
}
