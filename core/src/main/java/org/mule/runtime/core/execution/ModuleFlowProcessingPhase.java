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
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.api.util.ExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.core.util.message.MessageUtils.toMessage;
import static org.mule.runtime.core.util.message.MessageUtils.toMessageCollection;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromCallable;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
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
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
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
      Function<MessagingException, Publisher<Void>> errorHandler =
          onError(messageSource, template.getFailedExecutionResponseParametersFunction(), messageProcessContext, template,
                  phaseResultNotifier);

      Consumer<Either<MessagingException, Event>> terminateConsumer = getTerminateConsumer(messageSource, template);

      MonoProcessor<Void> responseCompletion = MonoProcessor.create();

      Event templateEvent = createEvent(template, messageProcessContext, sourceLocation, responseCompletion);

      // TODO MULE-11167 Policies should be non blocking
      if (!enableSourcePolicies) {
        just(templateEvent)
            .doOnNext(request -> fireNotification(messageProcessContext.getMessageSource(), request,
                                                  messageProcessContext.getFlowConstruct(),
                                                  MESSAGE_RECEIVED))
            // switchIfEmpty in case the flow was filtered
            .then(request -> from(template.routeEventAsync(request)).switchIfEmpty(fromCallable(() -> emptyEvent(templateEvent))))
            .then(onSuccess(messageSource, templateEvent, messageProcessContext, phaseResultNotifier, template,
                            terminateConsumer))
            .onErrorMap(MessagingException.class, me -> me.getCause() instanceof SourceErrorException ? me.getCause() : me)
            .onErrorResume(SourceErrorException.class,
                           see -> onSourceException(exceptionHandler, errorHandler, terminateConsumer, see))
            .onErrorResume(MessagingException.class, me -> from(errorHandler.apply(me))
                .doOnSuccess(v -> onTerminate(terminateConsumer, left(me)))
                .doOnError(e -> onTerminate(terminateConsumer, left(e))))
            .doAfterTerminate((event, throwable) -> responseCompletion.onComplete())
            .subscribe();
      } else {
        Processor nextOperation =
            createFlowExecutionProcessor(messageSource, exceptionHandler, messageProcessContext, template);
        SourcePolicy policy = policyManager.createSourcePolicyInstance(sourceLocation, templateEvent, nextOperation, template);

        try {
          Consumer<FailureSourcePolicyResult> onFailureFunction = failureSourcePolicyResult -> {
            final MessagingException messagingException = failureSourcePolicyResult.getMessagingException();
            try {
              final ErrorType errorType = messagingException.getEvent().getError().orElseGet(() -> ErrorBuilder
                  .builder(messagingException.getCause()).errorType(sourceResponseGenerateErrorType).build())
                  .getErrorType();

              from(errorHandler.apply(messagingException))
                  .doOnSuccess(v -> {
                    if (sourceResponseErrorTypeMatcher.match(errorType)) {
                      onTerminate(terminateConsumer, right(messagingException.getEvent()));
                    } else {
                      onTerminate(terminateConsumer,
                                  left(new SourceErrorException(messagingException.getEvent(), errorType, messagingException)
                                      .toMessagingException()));
                    }
                  })
                  .doOnError(SourceErrorException.class, see -> onTerminate(terminateConsumer, left(see.toMessagingException())))
                  .subscribe();
            } catch (SourceErrorException see) {
              onTerminate(terminateConsumer, left(see.toMessagingException()));
            }
          };

          Consumer<SuccessSourcePolicyResult> onSuccessFunction = successSourcePolicyResult -> {
            Event flowExecutionResponse = successSourcePolicyResult.getFlowExecutionResult();
            fireNotification(messageSource, flowExecutionResponse, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);

            // TODO MULE-11141 - This is the case of a filtered flow. This will eventually go away.
            if (flowExecutionResponse == null) {
              flowExecutionResponse = emptyEvent(templateEvent);
            }

            Map<String, Object> responseParameters = successSourcePolicyResult.getResponseParameters();

            Event finalResponse = flowExecutionResponse;
            from(template.sendResponseToClient(flowExecutionResponse, responseParameters,
                                               failureEvent -> successSourcePolicyResult
                                                   .createErrorResponseParameters(failureEvent),
                                               createResponseCompletationCallback(phaseResultNotifier)))
                                                   .onErrorResume(SourceErrorException.class,
                                                                  see -> from(errorHandler.apply(see.toMessagingException()))
                                                                      .doOnSuccess(v -> onTerminate(terminateConsumer,
                                                                                                    left(see))))
                                                   .doAfterTerminate((v, e) -> onTerminate(terminateConsumer,
                                                                                           right(finalResponse)))
                                                   .subscribe();
          };

          policy.process(templateEvent).apply(onFailureFunction, onSuccessFunction);
        } finally {
          policyManager.disposePoliciesResources(templateEvent.getContext().getId());
        }
      }
    } catch (Exception e) {
      phaseResultNotifier.phaseFailure(e);
    }
  }

  private Mono<Void> onSourceException(MessagingExceptionHandler exceptionHandler,
                                       Function<MessagingException, Publisher<Void>> errorHandler,
                                       Consumer<Either<MessagingException, Event>> terminateConsumer,
                                       SourceErrorException see) {
    if (sourceResponseErrorTypeMatcher.match(see.getErrorType())) {
      return from(handleSourceError(exceptionHandler, errorHandler, see))
          .doOnSuccess(v -> onTerminate(terminateConsumer, right(see.getEvent())))
          .doOnError(e -> onTerminate(terminateConsumer, left(e)));
    }
    return error(see);
  }

  private Publisher<Void> handleSourceError(final MessagingExceptionHandler exceptionHandler,
                                            Function<MessagingException, Publisher<Void>> errorConsumer,
                                            SourceErrorException see) {
    MessagingException messagingException = see.toMessagingException();
    exceptionHandler.handleException(messagingException, messagingException.getEvent());
    return errorConsumer.apply(messagingException);
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
            .output(toMessageCollection((Collection<Result>) resultValue, adapter.getCursorProviderFactory(),
                                        templateEvent))
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

  private CheckedFunction<MessagingException, Publisher<Void>> onError(MessageSource messageSource,
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

      if (messagingException.inErrorHandler()) {
        phaseResultNotifier.phaseFailure((Exception) messagingException.getCause());
        SourceErrorException exception =
            new SourceErrorException(messagingException.getEvent(), sourceErrorResponseGenerateErrorType,
                                     messagingException.getCause(), messagingException);
        return error(exception);
      } else {
        Map<String, Object> parameters;
        try {
          parameters = errorParametersFunction.apply(messagingException.getEvent());
        } catch (Exception e) {
          phaseResultNotifier.phaseFailure(e);
          return error(new SourceErrorException(errorEvent, sourceErrorResponseGenerateErrorType, e, messagingException));
        }

        return template.sendFailureResponseToClient(messagingException, parameters,
                                                    createSendFailureResponseCompletationCallback(phaseResultNotifier,
                                                                                                  sourceErrorResponseSendErrorType));
      }
    };
  }

  private Consumer<Either<MessagingException, Event>> getTerminateConsumer(MessageSource messageSource,
                                                                           ModuleFlowProcessingPhaseTemplate template) {
    return eventOrException -> {
      template.sendAfterTerminateResponseToClient(eventOrException.mapLeft(messagingException -> {
        messagingException.setProcessedEvent(createErrorEvent(messagingException.getEvent(), messageSource, messagingException,
                                                              muleContext.getErrorTypeLocator()));
        return messagingException;
      }));
    };
  }

  private Function<Event, Mono<Void>> onSuccess(MessageSource messageSource, Event request,
                                                MessageProcessContext messageProcessContext,
                                                PhaseResultNotifier phaseResultNotifier,
                                                ModuleFlowProcessingPhaseTemplate template,
                                                Consumer<Either<MessagingException, Event>> terminateConsumer) {
    return response -> {

      fireNotification(messageSource, response, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);

      // TODO MULE-11141 - This is the case of a filtered flow. This will eventually go away.
      if (response == null) {
        response = emptyEvent(request);
      }

      Map<String, Object> responseParameters;
      try {
        responseParameters = template.getSuccessfulExecutionResponseParametersFunction().apply(response);
      } catch (Exception e) {
        return error(new SourceErrorException(response, sourceResponseGenerateErrorType, e));
      }

      Event finalResponse = response;

      return from(template.sendResponseToClient(response, responseParameters,
                                                template.getFailedExecutionResponseParametersFunction(),
                                                createResponseCompletationCallback(phaseResultNotifier)))
                                                    .doOnSuccess(v -> onTerminate(terminateConsumer, right(finalResponse)));
    };
  }

  private Event emptyEvent(Event request) {
    return builder(request).message(of(null)).build();
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
        return transactionTemplate.execute(() -> {
          fireNotification(messageSource, muleEvent, messageProcessContext.getFlowConstruct(), MESSAGE_RECEIVED);
          return template.routeEvent(muleEvent);
        });
      } catch (MuleException e) {
        throw e;
      } catch (Exception e) {
        throw new DefaultMuleException(e);
      }
    };
  }

  private ResponseCompletionCallback createSendFailureResponseCompletationCallback(final PhaseResultNotifier phaseResultNotifier,
                                                                                   ErrorType failureErrorType) {
    return new ResponseCompletionCallback() {

      @Override
      public void responseSentSuccessfully() {
        phaseResultNotifier.phaseSuccessfully();
      }

      @Override
      public Event responseSentWithFailure(MessagingException e, Event event) {
        if (failureErrorType != null) {
          Event errorEvent = Event.builder(event)
              .error(ErrorBuilder.builder(e.getCause()).errorType(failureErrorType).build()).build();

          phaseResultNotifier.phaseFailure((Exception) e.getCause());
          throw new SourceErrorException(errorEvent, failureErrorType, e.getCause());
        } else {
          LOGGER.error("Unhandled exception processing request", e);
          return event;
        }
      }
    };
  }

  private ResponseCompletionCallback createResponseCompletationCallback(final PhaseResultNotifier phaseResultNotifier) {
    return new ResponseCompletionCallback() {

      @Override
      public void responseSentSuccessfully() {
        phaseResultNotifier.phaseSuccessfully();
      }

      @Override
      public Event responseSentWithFailure(final MessagingException e, final Event event) {
        if (e.getCause() instanceof SourceErrorException
            && sourceResponseSendErrorType.equals(((SourceErrorException) e.getCause()).getErrorType())) {
          throw (SourceErrorException) e.getCause();
        } else {
          throw new SourceErrorException(event, sourceResponseSendErrorType, e.getCause());
        }
      }
    };
  }

  /**
   * This method will not throw any {@link Exception}.
   *
   * @param terminateConsumer the action to perform on the transformed result.
   * @param result            the outcome of trying to send the response of the source through the source. In the case of error, only
   *                          {@link MessagingException} or {@link SourceErrorException} are valid values on the {@code left} side of this
   *                          parameter.
   */
  private void onTerminate(Consumer<Either<MessagingException, Event>> terminateConsumer, Either<Throwable, Event> result) {
    safely(() -> terminateConsumer.accept(result.mapLeft(throwable -> {
      if (throwable instanceof MessagingException) {
        return (MessagingException) throwable;
      } else if (throwable instanceof SourceErrorException) {
        return ((SourceErrorException) throwable).toMessagingException();
      } else {
        return null;
      }
    })));
  }

  @Override
  public int compareTo(MessageProcessPhase messageProcessPhase) {
    if (messageProcessPhase instanceof ValidationPhase) {
      return 1;
    }
    return 0;
  }

}
