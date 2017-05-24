/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static java.lang.System.getProperty;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.rx.Exceptions.UNEXPECTED_EXCEPTION_PREDICATE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;
import static org.mule.runtime.core.util.ExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.util.message.MessageUtils.toMessage;
import static org.mule.runtime.core.util.message.MessageUtils.toMessageCollection;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.ErrorTypeMatcher;
import org.mule.runtime.core.exception.ErrorTypeRepository;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.message.ErrorBuilder;
import org.mule.runtime.core.policy.FailureSourcePolicyResult;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.core.policy.SourcePolicy;
import org.mule.runtime.core.policy.SuccessSourcePolicyResult;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.MonoProcessor;

/**
 * This phase routes the message through the flow.
 * <p>
 * To participate of this phase, {@link MessageProcessTemplate} must implement {@link ModuleFlowProcessingPhaseTemplate}
 * <p>
 * This implementation will know how to process messages from extension's sources
 */
public class ModuleFlowProcessingPhase
    extends NotificationFiringProcessingPhase<ModuleFlowProcessingPhaseTemplate> implements Initialisable {

  // TODO MULE-11167 Policies should be non blocking
  public static final String ENABLE_SOURCE_POLICIES_SYSTEM_PROPERTY = "enableSourcePolicies";

  private static Logger LOGGER = LoggerFactory.getLogger(ModuleFlowProcessingPhase.class);

  private boolean enableSourcePolicies;

  private ErrorTypeMatcher sourceResponseErrorTypeMatcher;
  private ErrorType sourceResponseGenerateErrorType;
  private ErrorType sourceResponseSendErrorType;
  private ErrorType sourceErrorResponseGenerateErrorType;
  private ErrorType sourceErrorResponseSendErrorType;

  private final PolicyManager policyManager;

  public ModuleFlowProcessingPhase(PolicyManager policyManager) {
    this.policyManager = policyManager;
  }

  @Override
  public void initialise() throws InitialisationException {
    enableSourcePolicies = getProperty(ENABLE_SOURCE_POLICIES_SYSTEM_PROPERTY) != null;

    final ErrorTypeRepository errorTypeRepository = muleContext.getErrorTypeRepository();

    sourceResponseErrorTypeMatcher = new SingleErrorTypeMatcher(errorTypeRepository.getSourceResponseErrorType());

    sourceResponseGenerateErrorType = errorTypeRepository.getErrorType(SOURCE_RESPONSE_GENERATE).get();
    sourceResponseSendErrorType = errorTypeRepository.getErrorType(SOURCE_RESPONSE_SEND).get();
    sourceErrorResponseGenerateErrorType = errorTypeRepository.getErrorType(SOURCE_ERROR_RESPONSE_GENERATE).get();
    sourceErrorResponseSendErrorType = errorTypeRepository.getErrorType(SOURCE_ERROR_RESPONSE_SEND).get();
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
      ComponentLocation sourceLocation = messageSource.getLocation();
      Consumer<MessagingException> errorConsumer =
          getErrorConsumer(messageSource, template.getFailedExecutionResponseParametersFunction(),
                           messageProcessContext, template, phaseResultNotifier);

      MonoProcessor<Void> responseCompletion = MonoProcessor.create();

      Event templateEvent = createEvent(template, messageProcessContext, sourceLocation, responseCompletion);

      // TODO MULE-11167 Policies should be non blocking
      if (!enableSourcePolicies) {
        just(templateEvent)
            .doOnNext(request -> {
              fireNotification(messageProcessContext.getMessageSource(), request,
                               messageProcessContext.getFlowConstruct(),
                               MESSAGE_RECEIVED);
            })
            .then(request -> from(template.routeEventAsync(request)))
            .doOnSuccess(getSuccessConsumer(messageSource, templateEvent, exceptionHandler, messageProcessContext,
                                            phaseResultNotifier, template))
            .doOnError(SourceErrorException.class, see -> {
              if (sourceResponseErrorTypeMatcher.match(see.getErrorType())) {
                Event event = Event.builder(see.getEvent())
                    .error(ErrorBuilder.builder(see)
                        .errorType(see.getErrorType())
                        .build())
                    .build();
                MessagingException messagingException = new MessagingException(event, see);
                exceptionHandler.handleException(messagingException, event);
                errorConsumer.accept(messagingException);
              }
            })
            .doOnError(MessagingException.class, me -> {
              if (me.getCause() instanceof SourceErrorException
                  && sourceResponseErrorTypeMatcher.match(((SourceErrorException) me.getCause()).getErrorType())) {
                Event event = Event.builder(((SourceErrorException) me.getCause()).getEvent())
                    .error(ErrorBuilder.builder(me.getCause())
                        .errorType(((SourceErrorException) me.getCause()).getErrorType())
                        .build())
                    .build();
                MessagingException messagingException = new MessagingException(event, me.getCause());
                exceptionHandler.handleException(messagingException, event);
                errorConsumer.accept(messagingException);
              } else {
                errorConsumer.accept(me);
              }
            })
            .doOnError(UNEXPECTED_EXCEPTION_PREDICATE,
                       throwable -> LOGGER.error("Unhandled exception processing request", throwable))
            .doAfterTerminate((event, throwable) -> {
              // TODO Bazinga!
              // try {
              // onTerminate(terminateConsumer, event, throwable);
              // } finally {
              responseCompletion.onComplete();
              // }
            })
            .subscribe();
      } else {
        Processor nextOperation = createFlowExecutionProcessor(messageSource, exceptionHandler, messageProcessContext, template);
        SourcePolicy policy = policyManager.createSourcePolicyInstance(sourceLocation, templateEvent, nextOperation, template);

        try {
          Either<FailureSourcePolicyResult, SuccessSourcePolicyResult> sourcePolicyResult = policy.process(templateEvent);

          Consumer<MessagingException> onExceptionFunction = messagingException -> {
            messagingException
                .setProcessedEvent(createErrorEvent(messagingException.getEvent(), messageSource, messagingException,
                                                    muleContext.getErrorTypeLocator()));
            fireNotification(messageSource, messagingException.getEvent(), messageProcessContext.getFlowConstruct(),
                             MESSAGE_ERROR_RESPONSE);
            try {
              template.sendFailureResponseToClient(messagingException,
                                                   sourcePolicyResult.getLeft().getErrorResponseParameters(),
                                                   createSendFailureResponseCompletationCallback(phaseResultNotifier));
            } catch (Exception e) {
              throw new SourceErrorException(messagingException.getEvent(),
                                             sourceErrorResponseSendErrorType,
                                             e, messagingException);
            }
          };

          Consumer<SuccessSourcePolicyResult> onSuccessFunction = successSourcePolicyResult -> {
            Event flowExecutionResponse = successSourcePolicyResult.getFlowExecutionResult();
            fireNotification(messageSource, flowExecutionResponse, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);
            ResponseCompletionCallback responseCompletationCallback =
                createResponseCompletationCallback(phaseResultNotifier, exceptionHandler);

            // TODO MULE-11141 - This is the case of a filtered flow. This will eventually go away.
            if (flowExecutionResponse == null) {
              flowExecutionResponse = builder(templateEvent).message(of(null)).build();
            }

            Map<String, Object> responseParameters = sourcePolicyResult.getRight().getResponseParameters();

            Function<Event, Map<String, Object>> errorResponseParametersFunction =
                failureEvent -> sourcePolicyResult.getRight().createErrorResponseParameters(failureEvent);

            try {
              template.sendResponseToClient(flowExecutionResponse, responseParameters, errorResponseParametersFunction,
                                            responseCompletationCallback);
            } catch (Exception e) {
              onExceptionFunction.accept(new MessagingException(Event.builder(flowExecutionResponse)
                  .error(ErrorBuilder.builder()
                      .errorType(muleContext.getErrorTypeRepository().getErrorType(SOURCE_RESPONSE_SEND).get())
                      .build())
                  .build(), e));
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
                            ComponentLocation sourceLocation, Publisher<Void> responseCompletion)
      throws MuleException {
    Message message = template.getMessage();
    Event templateEvent =
        builder(create(messageProcessContext.getFlowConstruct(), sourceLocation, null, responseCompletion)).message(message)
            .flow(messageProcessContext.getFlowConstruct())
            .build();

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

      templateEvent = builder(templateEvent).message(message).build();
    }
    return templateEvent;
  }

  private Consumer<MessagingException> getErrorConsumer(MessageSource messageSource,
                                                        Function<Event, Map<String, Object>> errorParametersFunction,
                                                        MessageProcessContext messageProcessContext,
                                                        ModuleFlowProcessingPhaseTemplate template,
                                                        PhaseResultNotifier phaseResultNotifier) {
    return messagingException -> {
      Event errorEvent = createErrorEvent(messagingException.getEvent(), messageSource, messagingException,
                                          muleContext.getErrorTypeLocator());
      messagingException.setProcessedEvent(errorEvent);
      fireNotification(messageSource, messagingException.getEvent(), messageProcessContext.getFlowConstruct(),
                       MESSAGE_ERROR_RESPONSE);

      Map<String, Object> parameters;
      try {
        parameters = errorParametersFunction.apply(messagingException.getEvent());
      } catch (Exception e) {
        throw new SourceErrorException(errorEvent, sourceErrorResponseGenerateErrorType, e, messagingException);
      }

      try {
        template.sendFailureResponseToClient(messagingException, parameters,
                                             createSendFailureResponseCompletationCallback(phaseResultNotifier));
      } catch (Exception e) {
        throw new SourceErrorException(errorEvent, sourceErrorResponseSendErrorType, e, messagingException);
      }
    };
  }

  private Consumer<Event> getSuccessConsumer(MessageSource messageSource,
                                             Event request,
                                             MessagingExceptionHandler exceptionHandler,
                                             MessageProcessContext messageProcessContext,
                                             PhaseResultNotifier phaseResultNotifier,
                                             ModuleFlowProcessingPhaseTemplate template) {
    return response -> {

      fireNotification(messageSource, response, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);
      ResponseCompletionCallback responseCompletationCallback =
          createResponseCompletationCallback(phaseResultNotifier, exceptionHandler);

      // TODO MULE-11141 - This is the case of a filtered flow. This will eventually go away.
      if (response == null) {
        response = builder(request).message(of(null)).build();
      }

      Map<String, Object> responseParameters;
      try {
        responseParameters = template.getSuccessfulExecutionResponseParametersFunction().apply(response);
      } catch (Exception e) {
        throw new SourceErrorException(response, sourceResponseGenerateErrorType, e);
      }

      try {
        Function<Event, Map<String, Object>> errorResponseParametersFunction =
            template.getFailedExecutionResponseParametersFunction();

        template.sendResponseToClient(response, responseParameters, errorResponseParametersFunction,
                                      responseCompletationCallback);
      } catch (Exception e) {
        throw new SourceErrorException(response, sourceResponseSendErrorType, e);
      }
    };
  }

  private Processor createFlowExecutionProcessor(MessageSource messageSource, MessagingExceptionHandler exceptionHandler,
                                                 MessageProcessContext messageProcessContext,
                                                 ModuleFlowProcessingPhaseTemplate template) {
    return muleEvent -> {
      try {
        TransactionalErrorHandlingExecutionTemplate transactionTemplate =
            createMainExecutionTemplate(muleContext,
                                        messageProcessContext.getFlowConstruct(),
                                        messageProcessContext.getTransactionConfig().orElse(new MuleTransactionConfig()),
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

  static final class SourceErrorException extends MuleRuntimeException {

    private Event event;
    private ErrorType errorType;
    private MessagingException originalCause;

    public SourceErrorException(Event event, ErrorType errorType, Throwable cause) {
      super(cause);
      this.event = event;
      this.errorType = errorType;
      this.originalCause = null;
    }

    public SourceErrorException(Event event, ErrorType errorType, Throwable cause, MessagingException originalCause) {
      super(cause);
      this.event = event;
      this.errorType = errorType;
      this.originalCause = originalCause;
    }

    public Event getEvent() {
      return event;
    }

    public ErrorType getErrorType() {
      return errorType;
    }

    public MessagingException getOriginalCause() {
      return originalCause;
    }

  }

}
