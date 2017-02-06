/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.INTERCEPTING_CALLBACK_PARAM;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * A specialization of {@link OperationMessageProcessor} which also implements {@link InterceptingMessageProcessor}.
 * <p>
 * It should only be used with operations which return {@link InterceptingCallback} instances.
 *
 * @since 4.0
 */
public class InterceptingOperationMessageProcessor extends OperationMessageProcessor
    implements InterceptingMessageProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(InterceptingOperationMessageProcessor.class);

  private Processor next;

  public InterceptingOperationMessageProcessor(ExtensionModel extensionModel, OperationModel operationModel,
                                               ConfigurationProvider configurationProvider, String target,
                                               ResolverSet resolverSet,
                                               CursorStreamProviderFactory cursorStreamProviderFactory,
                                               ExtensionManager extensionManager,
                                               PolicyManager policyManager) {
    super(extensionModel, operationModel, configurationProvider, target, resolverSet, cursorStreamProviderFactory,
          extensionManager, policyManager);
  }

  @Override
  protected Mono<Event> doProcess(org.mule.runtime.core.api.Event event, ExecutionContextAdapter operationContext) {
    Event resultEvent = super.doProcess(event, operationContext).block();
    InterceptingCallback<?> interceptingCallback = getInterceptorCallback(operationContext);
    Mono<Event> result;
    try {
      if (interceptingCallback.shouldProcessNext()) {
        LOGGER.debug("Intercepting operation '{}' will proceed to execute intercepted chain",
                     operationContext.getComponentModel().getName());

        try {
          resultEvent = processNext(resultEvent, operationContext);
          onSuccess(operationContext, resultEvent, interceptingCallback);
          result = just(resultEvent);
        } catch (Exception e) {
          result = error(onException(interceptingCallback, resultEvent, operationContext, e));
        }

      } else {
        result = just(resultEvent);
        LOGGER.debug("Intercepting operation '{}' skipped processing of intercepted chain",
                     operationContext.getComponentModel().getName());
      }
    } finally {
      operationContext.removeVariable(INTERCEPTING_CALLBACK_PARAM);
      try {
        onComplete(interceptingCallback, event, operationContext);
      } catch (MuleException e) {
        result = error(e);
      }
    }

    return result;
  }

  private MuleException onException(InterceptingCallback<?> interceptingCallback, Event event,
                                    ExecutionContextAdapter operationContext, Exception exception) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Intercepting operation '%s' got an exception while processing intercepted chain",
                          operationContext.getComponentModel().getName()),
                   exception);
    }
    try {
      interceptingCallback.onException(exception);
    } catch (Exception e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER
            .debug(format("Intercepting operation '%s' got an error while processing exception on callback. Original exception will be thrown back to the flow",
                          operationContext.getComponentModel().getName()),
                   e);
      }
    }

    if (exception instanceof MuleException) {
      return (MuleException) exception;
    }

    return new MessagingException(event, exception, this);
  }

  private void onSuccess(ExecutionContextAdapter operationContext, Event resultEvent,
                         InterceptingCallback<?> interceptingCallback)
      throws MessagingException {
    LOGGER.debug("Intercepting operation '{}' success", operationContext.getComponentModel().getName());
    try {
      interceptingCallback.onSuccess(resultEvent.getMessage());
    } catch (Exception e) {
      throw new MessagingException(createStaticMessage(format("Intercepting operation '%s' executed intercepted chain but failed to process the obtained response",
                                                              operationContext.getComponentModel().getName())),
                                   resultEvent, e, this);
    }
  }

  private void onComplete(InterceptingCallback<?> interceptingCallback, Event event, ExecutionContextAdapter operationContext)
      throws MuleException {
    LOGGER.debug("Intercepting operation '{}' completed", operationContext.getComponentModel().getName());
    try {
      interceptingCallback.onComplete();
    } catch (Exception e) {
      throw new MessagingException(createStaticMessage(format("Intercepting operation '%s' failed to notify completion",
                                                              operationContext.getComponentModel().getName())),
                                   event, e, this);
    }
  }

  private InterceptingCallback<?> getInterceptorCallback(ExecutionContextAdapter<OperationModel> operationContext) {
    InterceptingCallback<?> interceptingCallback = operationContext.getVariable(INTERCEPTING_CALLBACK_PARAM);
    if (interceptingCallback == null) {
      throw new IllegalStateException("Could not find callback for intercepting operation "
          + operationContext.getComponentModel().getName());
    }
    return interceptingCallback;
  }

  private Event processNext(Event interceptedEvent, ExecutionContextAdapter operationContext) throws MuleException {
    if (next == null) {
      return interceptedEvent;
    } else if (interceptedEvent == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("MuleEvent is null. Next MessageProcessor '{}' will not be invoked.", next.getClass().getName());
      }
      return null;
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Invoking next MessageProcessor: '{}'", next.getClass().getName());
      }

      Event resultEvent = next.process(interceptedEvent);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Intercepting operation '%s' executed intercepted chain and got the following event back: ",
                            operationContext.getComponentModel().getName(), resultEvent));
      }

      return resultEvent;
    }
  }

  @Override
  protected ExecutionMediator createExecutionMediator() {
    return new InterceptingExecutionMediator(super.createExecutionMediator());
  }

  @Override
  public void setListener(Processor listener) {
    next = listener;
  }

}
