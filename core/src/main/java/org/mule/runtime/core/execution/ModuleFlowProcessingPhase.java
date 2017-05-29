/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static java.lang.System.getProperty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;
import static org.mule.runtime.core.util.ExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.util.FunctionalUtils.safely;
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
import org.mule.runtime.core.util.func.CheckedConsumer;
import org.mule.runtime.extension.api.runtime.operation.Result;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.MonoProcessor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

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
      Consumer<Either<Event, MessagingException>> terminateConsumer =
          getTerminateConsumer(messageSource, template, phaseResultNotifier);

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
            .doOnSuccess(getSuccessConsumer(messageSource, templateEvent, messageProcessContext, phaseResultNotifier, template,
                                            terminateConsumer))
            .doOnError(MessagingException.class, me -> {
              if (me.getCause() instanceof SourceErrorException
                  && sourceResponseErrorTypeMatcher.match(((SourceErrorException) me.getCause()).getErrorType())) {
                try {
                  handleSourceError(exceptionHandler, errorConsumer, (SourceErrorException) me.getCause());
                  onTerminate(terminateConsumer, me.getEvent(), null);
                } catch (Exception e) {
                  onTerminate(terminateConsumer, null, e);
                }
              } else {
                try {
                  errorConsumer.accept(me);
                  onTerminate(terminateConsumer, null, me);
                } catch (Exception e) {
                  onTerminate(terminateConsumer, null, e);
                }
              }
            })
            .doAfterTerminate((event, throwable) -> responseCompletion.onComplete())
            .subscribe();
      } else {
        Processor nextOperation = createFlowExecutionProcessor(messageSource, exceptionHandler, messageProcessContext, template);
        SourcePolicy policy = policyManager.createSourcePolicyInstance(sourceLocation, templateEvent, nextOperation, template);

        try {
          Consumer<FailureSourcePolicyResult> onFailureFunction = failureSourcePolicyResult -> {
            final MessagingException messagingException = failureSourcePolicyResult.getMessagingException();
            try {
              final ErrorType errorType = messagingException.getEvent().getError().orElseGet(() -> ErrorBuilder
                  .builder(messagingException.getCause()).errorType(sourceResponseGenerateErrorType).build())
                  .getErrorType();

              errorConsumer.accept(messagingException);

              if (sourceResponseErrorTypeMatcher.match(errorType)) {
                onTerminate(terminateConsumer, messagingException.getEvent(), null);
              } else {
                throw new SourceErrorException(messagingException.getEvent(), errorType, messagingException);
              }
            } catch (SourceErrorException see) {
              onTerminate(terminateConsumer, null, sourceErrorToMessagingException(see));
            }
          };

          Consumer<SuccessSourcePolicyResult> onSuccessFunction = successSourcePolicyResult -> {
            Event flowExecutionResponse = successSourcePolicyResult.getFlowExecutionResult();
            fireNotification(messageSource, flowExecutionResponse, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);

            // TODO MULE-11141 - This is the case of a filtered flow. This will eventually go away.
            if (flowExecutionResponse == null) {
              flowExecutionResponse = builder(templateEvent).message(of(null)).build();
            }

            Map<String, Object> responseParameters = successSourcePolicyResult.getResponseParameters();

            try {
              template.sendResponseToClient(flowExecutionResponse, responseParameters,
                                            failureEvent -> successSourcePolicyResult.createErrorResponseParameters(failureEvent),
                                            createResponseCompletationCallback(phaseResultNotifier));
            } catch (SourceErrorException see) {
              errorConsumer.accept(sourceErrorToMessagingException(see));
              onTerminate(terminateConsumer, null, see);
            } finally {
              onTerminate(terminateConsumer, flowExecutionResponse, null);
            }
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

  private void handleSourceError(final MessagingExceptionHandler exceptionHandler, Consumer<MessagingException> errorConsumer,
                                 SourceErrorException see) {
    MessagingException messagingException = sourceErrorToMessagingException(see);
    exceptionHandler.handleException(messagingException, messagingException.getEvent());
    errorConsumer.accept(messagingException);
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

  private CheckedConsumer<MessagingException> getErrorConsumer(MessageSource messageSource,
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
        phaseResultNotifier.phaseFailure(e);
        throw new SourceErrorException(errorEvent, sourceErrorResponseGenerateErrorType, e, messagingException);
      }

      template.sendFailureResponseToClient(messagingException, parameters,
                                           createSendFailureResponseCompletationCallback(phaseResultNotifier,
                                                                                         sourceErrorResponseSendErrorType));
    };
  }

  private Consumer<Either<Event, MessagingException>> getTerminateConsumer(MessageSource messageSource,
                                                                           ModuleFlowProcessingPhaseTemplate template,
                                                                           PhaseResultNotifier phaseResultNotifier) {
    return eventOrException -> {

      ResponseCompletionCallback completionCallback = createSendFailureResponseCompletationCallback(phaseResultNotifier, null);
      eventOrException.apply(event -> safely(() -> template.sendAfterTerminateResponseToClient(left(event))),
                             messagingException -> safely(() -> {
                               messagingException
                                   .setProcessedEvent(createErrorEvent(messagingException.getEvent(), messageSource,
                                                                       messagingException, muleContext.getErrorTypeLocator()));
                               template.sendAfterTerminateResponseToClient(right(messagingException));
                             }));
    };
  }

  private Consumer<Event> getSuccessConsumer(MessageSource messageSource, Event request,
                                             MessageProcessContext messageProcessContext,
                                             PhaseResultNotifier phaseResultNotifier,
                                             ModuleFlowProcessingPhaseTemplate template,
                                             Consumer<Either<Event, MessagingException>> terminateConsumer) {
    return response -> {

      fireNotification(messageSource, response, messageProcessContext.getFlowConstruct(), MESSAGE_RESPONSE);

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

      template.sendResponseToClient(response, responseParameters, template.getFailedExecutionResponseParametersFunction(),
                                    createResponseCompletationCallback(phaseResultNotifier));

      onTerminate(terminateConsumer, response, null);
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

  private void onTerminate(Consumer<Either<Event, MessagingException>> terminateConsumer, Event event, Throwable throwable) {
    if (throwable != null) {
      if (throwable instanceof MessagingException) {
        terminateConsumer.accept(right((MessagingException) throwable));
      } else if (throwable instanceof SourceErrorException) {
        terminateConsumer.accept(right(sourceErrorToMessagingException((SourceErrorException) throwable)));
      } else {
        terminateConsumer.accept(right(new MessagingException(event, throwable)));
      }
    } else {
      terminateConsumer.accept(left(event));
    }
  }

  private MessagingException sourceErrorToMessagingException(SourceErrorException see) {
    Event eventWithError = Event.builder(see.getEvent())
        .error(ErrorBuilder.builder(see.getCause())
            .errorType(see.getErrorType())
            .build())
        .build();
    final MessagingException me = new MessagingException(eventWithError, see.getCause());
    return me;
  }

  @Override
  public int compareTo(MessageProcessPhase messageProcessPhase) {
    if (messageProcessPhase instanceof ValidationPhase) {
      return 1;
    }
    return 0;
  }

  public static final class SourceErrorException extends MuleRuntimeException {

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

    public Optional<MessagingException> getOriginalCause() {
      return ofNullable(originalCause);
    }

  }

}
