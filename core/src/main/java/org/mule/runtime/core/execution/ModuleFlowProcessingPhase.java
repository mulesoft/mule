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
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.core.policy.SourcePolicy;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.resource.spi.work.Work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This phase routes the message through the flow.
 * <p>
 * To participate of this phase, {@link MessageProcessTemplate} must implement {@link ModuleFlowProcessingPhaseTemplate}
 *
 * This implementation will know how to process messages from extension's sources
 */
public class ModuleFlowProcessingPhase
    extends NotificationFiringProcessingPhase<ModuleFlowProcessingPhaseTemplate> {

  private final PolicyManager policyManager;
  protected static transient Logger logger = LoggerFactory.getLogger(ModuleFlowProcessingPhase.class);

  public ModuleFlowProcessingPhase(PolicyManager policyManager) {
    this.policyManager = policyManager;
  }

  @Override
  public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate) {
    return messageProcessTemplate instanceof ModuleFlowProcessingPhaseTemplate;
  }

  @Override
  public void runPhase(final ModuleFlowProcessingPhaseTemplate template, final MessageProcessContext messageProcessContext,
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

          Optional<SourcePolicy> policy =
              policyManager.findSourcePolicyInstance(templateEvent.getContext().getId(), sourceIdentifier);

          try {
            final MessagingExceptionHandler exceptionHandler = messageProcessContext.getFlowConstruct().getExceptionListener();
            Processor nextOperation = createFlowExecutionProcessor(messageSource, exceptionHandler);
            Event flowExecutionResponse;
            if (policy.isPresent()) {
              flowExecutionResponse = policy.get().process(templateEvent, nextOperation, template);
            } else {
              flowExecutionResponse = nextOperation.process(templateEvent);
            }
            fireNotification(messageSource, flowExecutionResponse, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);
            ResponseCompletionCallback responseCompletationCallback =
                createResponseCompletationCallback(phaseResultNotifier, exceptionHandler);

            // This is the case of a filtered flow. This will eventually go away.
            if (flowExecutionResponse == null) {
              flowExecutionResponse =
                  Event.builder(templateEvent).message((InternalMessage) Message.builder().nullPayload().build()).build();
            }

            Map<String, Object> responseParameters =
                generateSuccessfulResponseParameters(sourceIdentifier, policy, flowExecutionResponse, template);

            Optional<SourcePolicyParametersTransformer> policySourceParametersTransformer =
                policyManager.lookupSourceParametersTransformer(sourceIdentifier);
            Function<Event, Map<String, Object>> errorResponseParametersFunction =
                generateErrorResponseParametersFunction(policy, policySourceParametersTransformer,
                                                        template);

            template.sendResponseToClient(flowExecutionResponse, responseParameters, errorResponseParametersFunction,
                                          responseCompletationCallback);

          } catch (final MessagingException e) {


            fireNotification(messageSource, e.getEvent(), messageProcessContext.getFlowConstruct(), MESSAGE_ERROR_RESPONSE);

            template.sendFailureResponseToClient(e,
                                                 generateErrorResponseParametersFunction(policy, policyManager
                                                     .lookupSourceParametersTransformer(sourceIdentifier), template)
                                                         .apply(e.getEvent()),
                                                 createSendFailureResponseCompletationCallback(phaseResultNotifier));
          } finally {
            policyManager.disposePoliciesResources(templateEvent.getContext().getId());
          }
        } catch (Exception e) {
          phaseResultNotifier.phaseFailure(e);
        }
      }

      private Processor createFlowExecutionProcessor(MessageSource messageSource, MessagingExceptionHandler exceptionHandler) {
        return muleEvent -> {
          try {
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
          } catch (MuleException e) {
            throw e;
          } catch (Exception e) {
            throw new DefaultMuleException(e);
          }
        };
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

  private Function<Event, Map<String, Object>> generateErrorResponseParametersFunction(Optional<SourcePolicy> policy,
                                                                                       Optional<SourcePolicyParametersTransformer> policySourceParametersTransformer,
                                                                                       ModuleFlowProcessingPhaseTemplate template) {
    return (failureResponseEvent) -> {
      Map<String, Object> failureResponseParameters;
      if (policy.isPresent() && policySourceParametersTransformer.isPresent()) {
        failureResponseParameters =
            policySourceParametersTransformer.get().fromMessageToErrorResponseParameters(failureResponseEvent.getMessage());

      } else {
        failureResponseParameters = template.getFailedExecutionResponseParametersFunction().apply(failureResponseEvent);
      }
      return failureResponseParameters;
    };
  }

  private Map<String, Object> generateSuccessfulResponseParameters(ComponentIdentifier sourceIdentifier,
                                                                   Optional<SourcePolicy> policy, Event flowExecutionResponse,
                                                                   ModuleFlowProcessingPhaseTemplate template)
      throws MuleException {
    Map<String, Object> responseParameters;
    if (policy.isPresent()) {
      responseParameters = policyManager.lookupSourceParametersTransformer(sourceIdentifier).get()
          .fromMessageToSuccessResponseParameters(flowExecutionResponse.getMessage());
    } else {
      responseParameters = template.getSuccessfulExecutionResponseParametersFunction()
          .apply(flowExecutionResponse);
    }
    return responseParameters;
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
          Event handleException = exceptionListener.handleException(e, event);
          phaseResultNotifier.phaseSuccessfully();
          return handleException;
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

  private interface Callback {

    Event execute() throws Exception;

  }

  @Override
  public int compareTo(MessageProcessPhase messageProcessPhase) {
    if (messageProcessPhase instanceof ValidationPhase) {
      return 1;
    }
    return 0;
  }

}
