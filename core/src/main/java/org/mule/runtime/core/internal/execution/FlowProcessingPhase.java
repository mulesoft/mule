/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.api.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.SchedulerBusyException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.TransactionalExecutionTemplate;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.exception.ResponseDispatchException;
import org.mule.runtime.core.privileged.execution.FlowProcessingPhaseTemplate;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;
import org.mule.runtime.core.privileged.execution.RequestResponseFlowProcessingPhaseTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * This phase routes the message through the flow.
 *
 * To participate of this phase, {@link MessageProcessTemplate} must implement {@link FlowProcessingPhaseTemplate}
 */
public class FlowProcessingPhase extends NotificationFiringProcessingPhase<FlowProcessingPhaseTemplate> {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private Registry registry;

  /**
   * Creates a new FlowProcessingPhase with the provided {@link Registry}.
   * 
   * @param registry the registry to perform flow lookups.
   */
  public FlowProcessingPhase(Registry registry) {
    this.registry = registry;
  }

  @Override
  public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate) {
    return messageProcessTemplate instanceof FlowProcessingPhaseTemplate;
  }

  @Override
  public void runPhase(final FlowProcessingPhaseTemplate flowProcessingPhaseTemplate,
                       final MessageProcessContext messageProcessContext, final PhaseResultNotifier phaseResultNotifier) {
    Runnable flowExecutionWork = () -> {
      try {
        FlowConstruct flowConstruct =
            registry.<FlowConstruct>lookupByName(messageProcessContext.getMessageSource().getRootContainerName()).get();
        try {
          final AtomicReference exceptionThrownDuringFlowProcessing = new AtomicReference();
          TransactionalExecutionTemplate<CoreEvent> transactionTemplate =
              createTransactionalExecutionTemplate(muleContext, messageProcessContext.getTransactionConfig()
                  .orElse(new MuleTransactionConfig()));
          CoreEvent response = transactionTemplate.execute(() -> {
            try {
              Object message = flowProcessingPhaseTemplate.getOriginalMessage();
              if (message == null) {
                return null;
              }
              CoreEvent muleEvent = flowProcessingPhaseTemplate.getEvent();
              muleEvent = flowProcessingPhaseTemplate.beforeRouteEvent(muleEvent);
              muleEvent = flowProcessingPhaseTemplate.routeEvent(muleEvent);
              muleEvent = flowProcessingPhaseTemplate.afterRouteEvent(muleEvent);
              sendResponseIfNeccessary(messageProcessContext.getMessageSource(), flowConstruct,
                                       muleEvent, flowProcessingPhaseTemplate);
              return muleEvent;
            } catch (Exception e) {
              exceptionThrownDuringFlowProcessing.set(e);
              throw e;
            }
          });
          if (exceptionThrownDuringFlowProcessing.get() != null
              && !(exceptionThrownDuringFlowProcessing.get() instanceof ResponseDispatchException)) {
            sendResponseIfNeccessary(messageProcessContext.getMessageSource(), flowConstruct,
                                     response, flowProcessingPhaseTemplate);
          }
          flowProcessingPhaseTemplate.afterSuccessfulProcessingFlow(response);
        } catch (ResponseDispatchException e) {
          flowProcessingPhaseTemplate.afterFailureProcessingFlow(e);
        } catch (MessagingException e) {
          sendFailureResponseIfNeccessary(messageProcessContext.getMessageSource(), flowConstruct, e,
                                          flowProcessingPhaseTemplate);
          flowProcessingPhaseTemplate.afterFailureProcessingFlow(e);
        }
        phaseResultNotifier.phaseSuccessfully();
      } catch (Exception e) {
        MuleException me = new DefaultMuleException(e);
        try {
          flowProcessingPhaseTemplate.afterFailureProcessingFlow(me);
        } catch (MuleException e1) {
          logger.warn("Failure during exception processing in flow template: " + e.getMessage());
          if (logger.isDebugEnabled()) {
            logger.debug("Failure during exception processing in flow template: ", e);
          }
        }
        phaseResultNotifier.phaseFailure(e);
      }
    };
    if (messageProcessContext.supportsAsynchronousProcessing()) {
      try {
        messageProcessContext.getFlowExecutionExecutor().execute(flowExecutionWork);
      } catch (SchedulerBusyException e) {
        phaseResultNotifier.phaseFailure(e);
      }
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

  private void sendResponseIfNeccessary(MessageSource messageSource, FlowConstruct flow, CoreEvent muleEvent,
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

  public void setRegistry(Registry registry) {
    this.registry = registry;
  }
}
