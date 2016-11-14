/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.transaction.MuleTransactionConfig;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This phase routes the message through the flow.
 *
 * To participate of this phase, {@link MessageProcessTemplate} must implement {@link FlowProcessingPhaseTemplate}
 */
public class FlowProcessingPhase extends NotificationFiringProcessingPhase<FlowProcessingPhaseTemplate> {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate) {
    return messageProcessTemplate instanceof FlowProcessingPhaseTemplate;
  }

  @Override
  public void runPhase(final FlowProcessingPhaseTemplate flowProcessingPhaseTemplate,
                       final MessageProcessContext messageProcessContext, final PhaseResultNotifier phaseResultNotifier) {
    Runnable flowExecutionWork = () -> {
      try {
        try {
          final AtomicReference exceptionThrownDuringFlowProcessing = new AtomicReference();
          TransactionalErrorHandlingExecutionTemplate transactionTemplate =
              createMainExecutionTemplate(messageProcessContext.getFlowConstruct().getMuleContext(),
                                          messageProcessContext.getFlowConstruct(),
                                          (messageProcessContext.getTransactionConfig() == null ? new MuleTransactionConfig()
                                              : messageProcessContext.getTransactionConfig()),
                                          messageProcessContext.getFlowConstruct().getExceptionListener());
          Event response = transactionTemplate.execute(() -> {
            try {
              Object message = flowProcessingPhaseTemplate.getOriginalMessage();
              if (message == null) {
                return null;
              }
              Event muleEvent = flowProcessingPhaseTemplate.getEvent();
              muleEvent = flowProcessingPhaseTemplate.beforeRouteEvent(muleEvent);
              muleEvent = flowProcessingPhaseTemplate.routeEvent(muleEvent);
              muleEvent = flowProcessingPhaseTemplate.afterRouteEvent(muleEvent);
              sendResponseIfNeccessary(messageProcessContext.getMessageSource(), messageProcessContext.getFlowConstruct(),
                                       muleEvent, flowProcessingPhaseTemplate);
              return muleEvent;
            } catch (Exception e2) {
              exceptionThrownDuringFlowProcessing.set(e2);
              throw e2;
            }
          });
          if (exceptionThrownDuringFlowProcessing.get() != null
              && !(exceptionThrownDuringFlowProcessing.get() instanceof ResponseDispatchException)) {
            sendResponseIfNeccessary(messageProcessContext.getMessageSource(), messageProcessContext.getFlowConstruct(),
                                     response, flowProcessingPhaseTemplate);
          }
          flowProcessingPhaseTemplate.afterSuccessfulProcessingFlow(response);
        } catch (ResponseDispatchException e3) {
          flowProcessingPhaseTemplate.afterFailureProcessingFlow(e3);
        } catch (MessagingException e4) {
          sendFailureResponseIfNeccessary(messageProcessContext.getMessageSource(), messageProcessContext.getFlowConstruct(), e4,
                                          flowProcessingPhaseTemplate);
          flowProcessingPhaseTemplate.afterFailureProcessingFlow(e4);
        }
        phaseResultNotifier.phaseSuccessfully();
      } catch (Exception e5) {
        MuleException me = new DefaultMuleException(e5);
        try {
          flowProcessingPhaseTemplate.afterFailureProcessingFlow(me);
        } catch (MuleException e1) {
          logger.warn("Failure during exception processing in flow template: " + e5.getMessage());
          if (logger.isDebugEnabled()) {
            logger.debug("Failure during exception processing in flow template: ", e5);
          }
        }
        phaseResultNotifier.phaseFailure(e5);
      }
    };
    if (messageProcessContext.supportsAsynchronousProcessing()) {
      messageProcessContext.getFlowExecutionExecutor().execute(flowExecutionWork);
    } else {
      flowExecutionWork.run();
    }
  }

  private void sendFailureResponseIfNeccessary(MessageSource messageSource, FlowConstruct flow,
                                               MessagingException messagingException,
                                               FlowProcessingPhaseTemplate flowProcessingPhaseTemplate)
      throws MuleException {
    if (flowProcessingPhaseTemplate instanceof RequestResponseFlowProcessingPhaseTemplate) {
      fireNotification(messageSource, messagingException.getEvent(), flow, MESSAGE_ERROR_RESPONSE);
      ((RequestResponseFlowProcessingPhaseTemplate) flowProcessingPhaseTemplate).sendFailureResponseToClient(messagingException);
    }
  }

  private void sendResponseIfNeccessary(MessageSource messageSource, FlowConstruct flow, Event muleEvent,
                                        FlowProcessingPhaseTemplate flowProcessingPhaseTemplate)
      throws MuleException {
    if (flowProcessingPhaseTemplate instanceof RequestResponseFlowProcessingPhaseTemplate) {
      fireNotification(messageSource, muleEvent, flow, MESSAGE_RESPONSE);
      ((RequestResponseFlowProcessingPhaseTemplate) flowProcessingPhaseTemplate).sendResponseToClient(muleEvent);
    }
  }

  @Override
  public int compareTo(MessageProcessPhase messageProcessPhase) {
    if (messageProcessPhase instanceof ValidationPhase) {
      return 1;
    }
    return 0;
  }
}
