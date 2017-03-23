/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.rx.Exceptions.UNEXPECTED_EXCEPTION_PREDICATE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;
import static org.mule.runtime.core.util.ExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.util.message.MessageUtils.toMessage;
import static org.mule.runtime.core.util.message.MessageUtils.toMessageCollection;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.policy.FailureSourcePolicyResult;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.core.policy.SourcePolicy;
import org.mule.runtime.core.policy.SuccessSourcePolicyResult;
import org.mule.runtime.core.streaming.StreamingManager;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

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

  // TODO MULE-11167 Policies should be non blocking
  public static final String ENABLE_SOURCE_POLICIES_SYSTEM_PROPERTY = "enableSourcePolicies";

  private static Logger LOGGER = LoggerFactory.getLogger(ModuleFlowProcessingPhase.class);

  private final StreamingManager streamingManager;
  private final PolicyManager policyManager;

  public ModuleFlowProcessingPhase(PolicyManager policyManager, StreamingManager streamingManager) {
    this.policyManager = policyManager;
    this.streamingManager = streamingManager;
  }

  @Override
  public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate) {
    return messageProcessTemplate instanceof ModuleFlowProcessingPhaseTemplate;
  }

  @Override
  public void runPhase(final ModuleFlowProcessingPhaseTemplate template, final MessageProcessContext messageProcessContext,
                       final PhaseResultNotifier phaseResultNotifier) {

    try {

      final MessagingExceptionHandler exceptionHandler = messageProcessContext.getFlowConstruct().getExceptionListener();
      MessageSource messageSource = messageProcessContext.getMessageSource();
      ComponentIdentifier sourceIdentifier = messageProcessContext.getSourceIdentifier();
      Consumer<MessagingException> errorConsumer =
          getErrorConsumer(messageSource, template.getFailedExecutionResponseParametersFunction(),
                           messageProcessContext, template, phaseResultNotifier);


      Event templateEvent = createEvent(template, messageProcessContext, sourceIdentifier);

      // TODO MULE-11167 Policies should be non blocking
      if (System.getProperty(ENABLE_SOURCE_POLICIES_SYSTEM_PROPERTY) == null) {
        Reference<Event> eventReference = new Reference<>();
        just(templateEvent)
            .doOnNext(request -> {
              eventReference.set(request);
              fireNotification(messageProcessContext.getMessageSource(), request,
                               messageProcessContext.getFlowConstruct(),
                               MESSAGE_RECEIVED);
            })
            .then(request -> from(template.routeEventAsync(request)))
            .doOnSuccess(getSuccessConsumer(messageSource, templateEvent, exceptionHandler, errorConsumer,
                                            messageProcessContext, phaseResultNotifier,
                                            template))
            .doOnError(MessagingException.class, errorConsumer)
            .doOnError(UNEXPECTED_EXCEPTION_PREDICATE,
                       throwable -> LOGGER.error("Unhandled exception processing request" + throwable))
            .subscribe();
      } else {
        Processor nextOperation = createFlowExecutionProcessor(messageSource, exceptionHandler, messageProcessContext, template);
        SourcePolicy policy = policyManager.createSourcePolicyInstance(sourceIdentifier, templateEvent, nextOperation, template);

        try {
          Either<FailureSourcePolicyResult, SuccessSourcePolicyResult> sourcePolicyResult = policy.process(templateEvent);

          Consumer<MessagingException> onExceptionFunction = messagingException -> {
            messagingException
                .setProcessedEvent(createErrorEvent(messagingException.getEvent(), messageSource, messagingException,
                                                    messageProcessContext.getErrorTypeLocator()));
            fireNotification(messageSource, messagingException.getEvent(), messageProcessContext.getFlowConstruct(),
                             MESSAGE_ERROR_RESPONSE);
            try {
              template.sendFailureResponseToClient(messagingException,
                                                   sourcePolicyResult.getLeft().getErrorResponseParameters(),
                                                   createSendFailureResponseCompletationCallback(phaseResultNotifier));
            } catch (MuleException e) {
              throw new MuleRuntimeException(e);
            }
          };

          Consumer<SuccessSourcePolicyResult> onSuccessFunction = successSourcePolicyResult -> {
            Event flowExecutionResponse = successSourcePolicyResult.getFlowExecutionResult();
            fireNotification(messageSource, flowExecutionResponse, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);
            ResponseCompletionCallback responseCompletationCallback =
                createResponseCompletationCallback(phaseResultNotifier, exceptionHandler);

            // TODO MULE-11141 - This is the case of a filtered flow. This will eventually go away.
            if (flowExecutionResponse == null) {
              flowExecutionResponse =
                  Event.builder(templateEvent).message(Message.builder().nullPayload().build()).build();
            }

            Map<String, Object> responseParameters = sourcePolicyResult.getRight().getResponseParameters();

            Function<Event, Map<String, Object>> errorResponseParametersFunction =
                failureEvent -> sourcePolicyResult.getRight().createErrorResponseParameters(failureEvent);

            try {
              template.sendResponseToClient(flowExecutionResponse, responseParameters, errorResponseParametersFunction,
                                            responseCompletationCallback);
            } catch (MessagingException e) {
              onExceptionFunction.accept(e);
            } catch (MuleException e) {
              onExceptionFunction.accept(new MessagingException(flowExecutionResponse, e));
            }
          };

          sourcePolicyResult
              .apply(failureSourcePolicyResult -> onExceptionFunction.accept(failureSourcePolicyResult.getMessagingException()),
                     onSuccessFunction);
        } finally {
          policyManager.disposePoliciesResources(templateEvent.getContext().getId());
        }
      }
    } catch (Exception e) {
      phaseResultNotifier.phaseFailure(e);
    }
  }

  private Event createEvent(ModuleFlowProcessingPhaseTemplate template, MessageProcessContext messageProcessContext,
                            ComponentIdentifier sourceIdentifier)
      throws MuleException {
    Message message = template.getMessage();
    Event templateEvent =
        Event.builder(create(messageProcessContext.getFlowConstruct(), sourceIdentifier.getNamespace()))
            .message(message).build();

    if (message.getPayload().getValue() instanceof SourceResultAdapter) {
      SourceResultAdapter adapter = (SourceResultAdapter) message.getPayload().getValue();
      final Result<?, ?> result = adapter.getResult();
      final Object resultValue = result.getOutput();

      if (resultValue instanceof Collection && adapter.isCollection()) {
        message = toMessage(Result.<Collection<Message>, Attributes>builder()
            .output(toMessageCollection((Collection<Result>) resultValue, result.getMediaType().orElse(ANY),
                                        adapter.getCursorProviderFactory(), templateEvent))
            .attributes(NULL_ATTRIBUTES)
            .mediaType(result.getMediaType().orElse(ANY))
            .build());
      } else {
        message = toMessage(result, result.getMediaType().orElse(ANY), adapter.getCursorProviderFactory(), templateEvent);
      }

      templateEvent = Event.builder(templateEvent).message(message).build();
    }
    return templateEvent;
  }

  private Consumer<MessagingException> getErrorConsumer(MessageSource messageSource,
                                                        Function<Event, Map<String, Object>> errorParametersFunction,
                                                        MessageProcessContext messageProcessContext,
                                                        ModuleFlowProcessingPhaseTemplate template,
                                                        PhaseResultNotifier phaseResultNotifier) {
    return messagingException -> {
      messagingException
          .setProcessedEvent(createErrorEvent(messagingException.getEvent(), messageSource, messagingException,
                                              messageProcessContext.getErrorTypeLocator()));
      fireNotification(messageSource, messagingException.getEvent(), messageProcessContext.getFlowConstruct(),
                       MESSAGE_ERROR_RESPONSE);
      try {
        template.sendFailureResponseToClient(messagingException,
                                             errorParametersFunction.apply(messagingException.getEvent()),
                                             createSendFailureResponseCompletationCallback(phaseResultNotifier));
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    };
  }

  private Consumer<Event> getSuccessConsumer(MessageSource messageSource,
                                             Event request,
                                             MessagingExceptionHandler exceptionHandler,
                                             Consumer<MessagingException> onExceptionFunction,
                                             MessageProcessContext messageProcessContext,
                                             PhaseResultNotifier phaseResultNotifier,
                                             ModuleFlowProcessingPhaseTemplate template) {
    return response -> {

      fireNotification(messageSource, response, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);
      ResponseCompletionCallback responseCompletationCallback =
          createResponseCompletationCallback(phaseResultNotifier, exceptionHandler);

      // TODO MULE-11141 - This is the case of a filtered flow. This will eventually go away.
      if (response == null) {
        response =
            Event.builder(request).message(Message.builder().nullPayload().build()).build();
      }

      Map<String, Object> responseParameters =
          template.getSuccessfulExecutionResponseParametersFunction().apply(response);

      Function<Event, Map<String, Object>> errorResponseParametersFunction =
          template.getFailedExecutionResponseParametersFunction();

      try {
        template.sendResponseToClient(response, responseParameters, errorResponseParametersFunction,
                                      responseCompletationCallback);
      } catch (MessagingException e) {
        onExceptionFunction.accept(e);
      } catch (MuleException e) {
        onExceptionFunction.accept(new MessagingException(response, e));
      }
    };
  }

  private Processor createFlowExecutionProcessor(MessageSource messageSource, MessagingExceptionHandler exceptionHandler,
                                                 MessageProcessContext messageProcessContext,
                                                 ModuleFlowProcessingPhaseTemplate template) {
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
