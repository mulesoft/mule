/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.policy.PolicySourceParametersTransformer;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.policy.NextOperation;
import org.mule.runtime.core.policy.PolicyProvider;
import org.mule.runtime.core.policy.Policy;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.resource.spi.work.Work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This phase routes the message through the flow.
 * <p>
 * To participate of this phase, {@link MessageProcessTemplate} must implement {@link ExtensionFlowProcessingPhaseTemplate}
 *
 * This implementation will know how to process messages from extension's sources
 */
public class ExtensionFlowProcessingPhase
    extends NotificationFiringProcessingPhase<ExtensionFlowProcessingPhaseTemplate> {

  private final PolicyManager policyManager;
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  public ExtensionFlowProcessingPhase(PolicyManager policyManager) {
    this.policyManager = policyManager;
  }

  @Override
  public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate) {
    return messageProcessTemplate instanceof ExtensionFlowProcessingPhaseTemplate;
  }

  @Override
  public void runPhase(final ExtensionFlowProcessingPhaseTemplate template, final MessageProcessContext messageProcessContext,
                       final PhaseResultNotifier phaseResultNotifier) {
    Work flowExecutionWork = new Work() {

      @Override
      public void release() {

      }

      @Override
      public void run() {
        try {
          MessageSource messageSource = messageProcessContext.getMessageSource();
          ComponentIdentifier sourceIdentifier = messageProcessContext.getSourceIdentifier();
          final Event templateEvent =
              Event.builder(create(messageProcessContext.getFlowConstruct(), sourceIdentifier.getNamespace()))
                  .message((InternalMessage) template.getMessage()).build();
          PolicyProvider policyProvider = policyManager.lookupPolicyProvider();
          Optional<Policy> policy =
              policyProvider.findSourcePolicyInstance(templateEvent.getContext().getId(), sourceIdentifier);
          try {
            final MessagingExceptionHandler exceptionHandler = messageProcessContext.getFlowConstruct().getExceptionListener();
            NextOperation nextOperation = (muleEvent) -> {
              TransactionalErrorHandlingExecutionTemplate transactionTemplate =
                  createMainExecutionTemplate(messageProcessContext.getFlowConstruct().getMuleContext(),
                                              messageProcessContext.getFlowConstruct(),
                                              (messageProcessContext.getTransactionConfig() == null ? new MuleTransactionConfig()
                                                  : messageProcessContext.getTransactionConfig()),
                                              exceptionHandler);
              final Event response = transactionTemplate.execute(() -> {

                fireNotification(messageSource, muleEvent, messageProcessContext.getFlowConstruct(), MESSAGE_RECEIVED);
                return template.routeEvent(muleEvent);
              });
              return response;
            };

            Event flowExecutionResponse;
            if (policy.isPresent()) {
              nextOperation = buildFlowExecutionWithPolicyFunction(nextOperation, templateEvent, template, sourceIdentifier,
                                                                   () -> policy.get().getNextOperationEvent());
              flowExecutionResponse = policy.get().process(templateEvent, nextOperation);
            } else {
              flowExecutionResponse = nextOperation.execute(templateEvent);
            }
            fireNotification(messageSource, flowExecutionResponse, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);
            ResponseCompletionCallback responseCompletationCallback =
                createResponseCompletationCallback(phaseResultNotifier, exceptionHandler);
            // This is the case of a filtered flow. This will eventually go away.
            if (flowExecutionResponse == null) {
              flowExecutionResponse =
                  Event.builder(templateEvent).message((InternalMessage) Message.builder().nullPayload().build()).build();
            }
            Map<String, Object> responseParameters;
            if (policy.isPresent()) {
              responseParameters = policyManager.lookupSourceParametersTransformer(sourceIdentifier).get()
                  .fromMessageToSuccessResponseParameters(flowExecutionResponse.getMessage());
              template.sendResponseToClient(flowExecutionResponse, responseParameters, responseCompletationCallback);
            } else {
              responseParameters = template.getSuccessfulExecutionResponseParametersFunction()
                  .apply(flowExecutionResponse);
            }
            template.sendResponseToClient(flowExecutionResponse, responseParameters, responseCompletationCallback);
          } catch (final MessagingException e) {
            Map<String, Object> failureResponseParameters;
            Optional<PolicySourceParametersTransformer> policySourceParametersTransformer =
                policyManager.lookupSourceParametersTransformer(sourceIdentifier);
            if (policy.isPresent() && policySourceParametersTransformer.isPresent()) {
              failureResponseParameters =
                  policySourceParametersTransformer.get().fromMessageToErrorResponseParameters(e.getEvent().getMessage());

            } else {
              failureResponseParameters = template.getFailedExecutionResponseParametersFunction().apply(e.getEvent());
            }
            fireNotification(messageSource, e.getEvent(), messageProcessContext.getFlowConstruct(), MESSAGE_ERROR_RESPONSE);
            template.sendFailureResponseToClient(e, failureResponseParameters,
                                                 createSendFailureResponseCompletationCallback(phaseResultNotifier));
          } finally {
            silentlyDisposePolicy(templateEvent, policyProvider);
          }
        } catch (Exception e) {
          phaseResultNotifier.phaseFailure(e);
        }
      }
    };

    if (messageProcessContext.supportsAsynchronousProcessing()) {
      try {
        messageProcessContext.getFlowExecutionExecutor().execute(flowExecutionWork);
      } catch (Exception e) {
        phaseResultNotifier.phaseFailure(e);
      }
    } else {
      flowExecutionWork.run();
    }
  }

  private void silentlyDisposePolicy(Event templateEvent, PolicyProvider policyProvider) {
    try {
      if (policyProvider != null) {
        policyProvider.dispose(templateEvent.getContext().getId());
      }
    } catch (Exception e) {
      logger.warn(e.getMessage());
      if (logger.isDebugEnabled()) {
        logger.debug("failure disposing a policy", e);
      }
    }
  }

  private NextOperation buildFlowExecutionWithPolicyFunction(NextOperation nextOperation, Event sourceEvent,
                                                             ExtensionFlowProcessingPhaseTemplate template,
                                                             ComponentIdentifier sourceIdentifier,
                                                             Supplier<Event> lastEventSupplier) {
    return (processEvent) -> {
      try {
        Event flowExecutionResponse = nextOperation.execute(sourceEvent);
        Optional<PolicySourceParametersTransformer> policySourceParametersTransformer =
            policyManager.lookupSourceParametersTransformer(sourceIdentifier);
        if (policySourceParametersTransformer.isPresent()) {
          Map<String, Object> responseParameters =
              template.getSuccessfulExecutionResponseParametersFunction().apply(flowExecutionResponse);
          return Event.builder(processEvent.getContext())
              .message((InternalMessage) policySourceParametersTransformer.get()
                  .fromSuccessResponseParametersToMessage(responseParameters))
              .build();
        } else {
          return Event.builder(flowExecutionResponse).build();
        }
      } catch (MessagingException messagingException) {
        Map<String, Object> failureParameters =
            template.getFailedExecutionResponseParametersFunction().apply(messagingException.getEvent());
        Optional<PolicySourceParametersTransformer> policySourceParametersTransformer =
            policyManager.lookupSourceParametersTransformer(sourceIdentifier);
        if (policySourceParametersTransformer.isPresent()) {
          Message message = policySourceParametersTransformer.get().fromFailureResponseParametersToMessage(failureParameters);
          Event.Builder eventBuilder =
              Event.builder(messagingException.getEvent().getContext()).message((InternalMessage) message);
          lastEventSupplier.get().getVariableNames().forEach(variableName -> {
            eventBuilder.addVariable(variableName, lastEventSupplier.get().getVariable(variableName));
          });
          throw new MessagingException(eventBuilder.build(), messagingException.getCause());
        }
        throw messagingException;
      }
    };
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
        return executeCallback((processEvent) -> {
          Event handleException = exceptionListener.handleException(e, processEvent);
          phaseResultNotifier.phaseSuccessfully();
          return handleException;
        }, phaseResultNotifier);
      }
    };
  }

  private Event executeCallback(final NextOperation callback, PhaseResultNotifier phaseResultNotifier) {
    try {
      return callback.execute(null);
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

}
