/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.transaction.MuleTransactionConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This phase routes the message through the flow.
 * <p>
 * To participate of this phase, {@link org.mule.runtime.core.execution.MessageProcessTemplate} must implement
 * {@link org.mule.runtime.core.execution.FlowProcessingPhaseTemplate}
 */
public class AsyncResponseFlowProcessingPhase
    extends NotificationFiringProcessingPhase<AsyncResponseFlowProcessingPhaseTemplate> {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate) {
    return messageProcessTemplate instanceof AsyncResponseFlowProcessingPhaseTemplate;
  }

  @Override
  public void runPhase(final AsyncResponseFlowProcessingPhaseTemplate template, final MessageProcessContext messageProcessContext,
                       final PhaseResultNotifier phaseResultNotifier) {
    Runnable flowExecutionWork = () -> {
      try {
        MessageSource messageSource = messageProcessContext.getMessageSource();
        try {
          final MessagingExceptionHandler exceptionHandler = messageProcessContext.getFlowConstruct().getExceptionListener();
          TransactionalErrorHandlingExecutionTemplate transactionTemplate =
              createMainExecutionTemplate(messageProcessContext.getFlowConstruct().getMuleContext(),
                                          messageProcessContext.getFlowConstruct(),
                                          (messageProcessContext.getTransactionConfig() == null ? new MuleTransactionConfig()
                                              : messageProcessContext.getTransactionConfig()),
                                          exceptionHandler);
          final Event response = transactionTemplate.execute(() -> {
            Event muleEvent = template.getEvent();
            fireNotification(messageSource, muleEvent, messageProcessContext.getFlowConstruct(), MESSAGE_RECEIVED);
            return template.routeEvent(muleEvent);
          });
          fireNotification(messageSource, response, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);
          template.sendResponseToClient(response, createResponseCompletationCallback(phaseResultNotifier, exceptionHandler));
        } catch (final MessagingException e1) {
          fireNotification(messageSource, e1.getEvent(), messageProcessContext.getFlowConstruct(), MESSAGE_ERROR_RESPONSE);
          template.sendFailureResponseToClient(e1, createSendFailureResponseCompletationCallback(phaseResultNotifier));
        }
      } catch (Exception e2) {
        phaseResultNotifier.phaseFailure(e2);
      }
    };

    if (messageProcessContext.supportsAsynchronousProcessing()) {
      messageProcessContext.getFlowExecutionExecutor().execute(flowExecutionWork);
    } else {
      flowExecutionWork.run();
    }
  }

  private ResponseCompletionCallback createSendFailureResponseCompletationCallback(final PhaseResultNotifier phaseResultNotifier) {
    return new ResponseCompletionCallback() {

      @Override
      public void responseSentSuccessfully() {
        phaseResultNotifier.phaseSuccessfully();
      }

      @Override
      public Event responseSentWithFailure(MessagingException e, Event event) {
        phaseResultNotifier.phaseFailure(e);
        return event;
      }
    };
  }

  private ResponseCompletionCallback createResponseCompletationCallback(final PhaseResultNotifier phaseResultNotifier,
                                                                        final MessagingExceptionHandler exceptionListener) {
    return new ResponseCompletionCallback() {

      @Override
      public void responseSentSuccessfully() {
        phaseResultNotifier.phaseSuccessfully();
      }

      @Override
      public Event responseSentWithFailure(final MessagingException e, final Event event) {
        return executeCallback(() -> {
          final Event exceptionStrategyResult = exceptionListener.handleException(e, event);
          phaseResultNotifier.phaseSuccessfully();
          return exceptionStrategyResult;
        }, phaseResultNotifier);
      }
    };
  }

  private Event executeCallback(final Callback callback, PhaseResultNotifier phaseResultNotifier) {
    try {
      return callback.execute();
    } catch (Exception callbackException) {
      phaseResultNotifier.phaseFailure(callbackException);
      throw new MuleRuntimeException(callbackException);
    }
  }

  @Override
  public int compareTo(MessageProcessPhase messageProcessPhase) {
    if (messageProcessPhase instanceof ValidationPhase) {
      return 1;
    }
    return 0;
  }

  private interface Callback {

    Event execute() throws Exception;

  }

}
