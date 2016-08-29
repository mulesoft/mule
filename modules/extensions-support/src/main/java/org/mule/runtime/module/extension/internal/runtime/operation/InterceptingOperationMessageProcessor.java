/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.INTERCEPTING_CALLBACK_PARAM;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.util.NotificationUtils;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.runtime.ExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.InterceptingExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialization of {@link OperationMessageProcessor} which also implements {@link InterceptingMessageProcessor}.
 * <p>
 * It should only be used with operations which return {@link InterceptingCallback} instances.
 *
 * @since 4.0
 */
public class InterceptingOperationMessageProcessor extends OperationMessageProcessor
    implements InterceptingMessageProcessor, MessageProcessorContainer {

  private static final Logger LOGGER = LoggerFactory.getLogger(InterceptingOperationMessageProcessor.class);

  private MessageProcessor next;

  public InterceptingOperationMessageProcessor(RuntimeExtensionModel extensionModel, RuntimeOperationModel operationModel,
                                               String configurationProviderName, String target, ResolverSet resolverSet,
                                               ExtensionManagerAdapter extensionManager) {
    super(extensionModel, operationModel, configurationProviderName, target, resolverSet, extensionManager);
  }

  @Override
  protected MuleEvent doProcess(org.mule.runtime.core.api.MuleEvent event, OperationContextAdapter operationContext)
      throws MuleException {
    MuleEvent resultEvent = (MuleEvent) super.doProcess(event, operationContext);
    InterceptingCallback<?> interceptingCallback = getInterceptorCallback(operationContext);

    try {
      if (interceptingCallback.shouldProcessNext()) {
        LOGGER.debug("Intercepting operation '{}' will proceed to execute intercepted chain",
                     operationContext.getOperationModel().getName());

        try {
          resultEvent = processNext(resultEvent, operationContext);
        } catch (Exception e) {
          throw onException(interceptingCallback, resultEvent, operationContext, e);
        }

        onSuccess(operationContext, resultEvent, interceptingCallback);
      } else {
        LOGGER.debug("Intercepting operation '{}' skipped processing of intercepted chain",
                     operationContext.getOperationModel().getName());
      }
    } finally {
      operationContext.removeVariable(INTERCEPTING_CALLBACK_PARAM);
      onComplete(interceptingCallback, event, operationContext);
    }

    return resultEvent;

  }

  private MuleException onException(InterceptingCallback<?> interceptingCallback, MuleEvent event,
                                    OperationContextAdapter operationContext, Exception exception) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Intercepting operation '%s' got an exception while processing intercepted chain",
                          operationContext.getOperationModel().getName()),
                   exception);
    }
    try {
      interceptingCallback.onException(exception);
    } catch (Exception e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER
            .debug(format("Intercepting operation '%s' got an error while processing exception on callback. Original exception will be thrown back to the flow",
                          operationContext.getOperationModel().getName()),
                   e);
      }
    }

    if (exception instanceof MuleException) {
      return (MuleException) exception;
    }

    return new MessagingException(event, exception, this);
  }

  private void onSuccess(OperationContextAdapter operationContext, MuleEvent resultEvent,
                         InterceptingCallback<?> interceptingCallback)
      throws MessagingException {
    LOGGER.debug("Intercepting operation '{}' success", operationContext.getOperationModel().getName());
    try {
      interceptingCallback.onSuccess(resultEvent.getMessage());
    } catch (Exception e) {
      throw new MessagingException(createStaticMessage(format("Intercepting operation '%s' executed intercepted chain but failed to process the obtained response",
                                                              operationContext.getOperationModel().getName())),
                                   resultEvent, e, this);
    }
  }

  private void onComplete(InterceptingCallback<?> interceptingCallback, MuleEvent event, OperationContextAdapter operationContext)
      throws MuleException {
    LOGGER.debug("Intercepting operation '{}' completed", operationContext.getOperationModel().getName());
    try {
      interceptingCallback.onComplete();
    } catch (Exception e) {
      throw new MessagingException(createStaticMessage(format("Intercepting operation '%s' failed to notify completion",
                                                              operationContext.getOperationModel().getName())),
                                   event, e, this);
    }
  }

  private InterceptingCallback<?> getInterceptorCallback(OperationContextAdapter operationContext) {
    InterceptingCallback<?> interceptingCallback = operationContext.getVariable(INTERCEPTING_CALLBACK_PARAM);
    if (interceptingCallback == null) {
      throw new IllegalStateException("Could not find callback for intercepting operation "
          + operationContext.getOperationModel().getName());
    }
    return interceptingCallback;
  }

  private MuleEvent processNext(MuleEvent interceptedEvent, OperationContextAdapter operationContext) throws MuleException {
    if (next == null) {
      return interceptedEvent;
    } else if (interceptedEvent == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("MuleEvent is null. Next MessageProcessor '{}' will not be invoked.", next.getClass().getName());
      }
      return null;
    } else if (VoidMuleEvent.getInstance().equals(interceptedEvent)) {
      return interceptedEvent;
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Invoking next MessageProcessor: '{}'", next.getClass().getName());
      }

      MuleEvent resultEvent = next.process(interceptedEvent);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Intercepting operation '%s' executed intercepted chain and got the following event back: ",
                            operationContext.getOperationModel().getName(), resultEvent));
      }

      return resultEvent;
    }
  }

  @Override
  protected ExecutionMediator createExecutionMediator() {
    return new InterceptingExecutionMediator(super.createExecutionMediator());
  }

  @Override
  public void setListener(MessageProcessor listener) {
    next = listener;
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    if (next instanceof InternalMessageProcessor) {
      return;
    }
    if (next instanceof MessageProcessorChain) {
      NotificationUtils.addMessageProcessorPathElements(((MessageProcessorChain) next).getMessageProcessors(),
                                                        pathElement.getParent());
    } else if (next != null) {
      NotificationUtils.addMessageProcessorPathElements(asList(next), pathElement.getParent());
    }
  }
}
