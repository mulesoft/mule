/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.core.api.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.core.api.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.api.message.ErrorBuilder.builder;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.util.ExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessageCollection;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromCallable;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.when;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.ErrorTypeRepository;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.execution.MessageProcessContext;
import org.mule.runtime.core.api.execution.MessageProcessTemplate;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.policy.SourcePolicy;
import org.mule.runtime.core.internal.policy.SourcePolicyFailureResult;
import org.mule.runtime.core.internal.policy.SourcePolicySuccessResult;
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

  private static Logger LOGGER = LoggerFactory.getLogger(ModuleFlowProcessingPhase.class);

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
      final MessageSource messageSource = messageProcessContext.getMessageSource();
      final FlowConstruct flowConstruct = (FlowConstruct) muleContext.getConfigurationComponentLocator().find(Location.builder()
          .globalName(messageSource.getRootContainerName()).build()).get();
      final ComponentLocation sourceLocation = messageSource.getLocation();
      final Consumer<Either<MessagingException, Event>> terminateConsumer = getTerminateConsumer(messageSource, template);
      final MonoProcessor<Void> responseCompletion = MonoProcessor.create();
      final Event templateEvent = createEvent(template, sourceLocation, responseCompletion, flowConstruct);
      final SourcePolicy policy = policyManager.createSourcePolicyInstance(sourceLocation, templateEvent,
                                                                           new FlowProcessor(template, templateEvent), template);
      final PhaseContext phaseContext = new PhaseContext(template, messageProcessContext, phaseResultNotifier, terminateConsumer);

      just(templateEvent)
          .doOnNext(onMessageReceived(messageProcessContext, flowConstruct))
          // Process policy and in turn flow emitting Either<SourcePolicyFailureResult,SourcePolicySuccessResult>> when complete.
          .then(request -> from(policy.process(request)))
          // Perform processing of result by sending success or error response and handle errors that occur.
          // Returns Publisher<Void> to signal when this is complete or if it failed.
          .then(policyResult -> policyResult.reduce(policyFailure(phaseContext, flowConstruct),
                                                    policySuccess(phaseContext, flowConstruct)))
          .doOnSuccess(aVoid -> phaseResultNotifier.phaseSuccessfully())
          .doOnError(onFailure(phaseResultNotifier, terminateConsumer))
          // Complete EventContext via responseCompletion Mono once everything is done.
          .doAfterTerminate((event, throwable) -> responseCompletion.onComplete())
          .subscribe();
    } catch (Exception e) {
      phaseResultNotifier.phaseFailure(e);
    }
  }

  /*
   * Consumer invoked for each new execution of this processing phase.
   */
  private Consumer<Event> onMessageReceived(MessageProcessContext messageProcessContext, FlowConstruct flowConstruct) {
    return request -> fireNotification(messageProcessContext.getMessageSource(), request,
                                       flowConstruct, MESSAGE_RECEIVED);
  }

  /*
   * Process success by attempting to send a response to client handling the case where response sending fails or the resolution
   * of response parameters fails.
   */
  private Function<SourcePolicySuccessResult, Mono<Void>> policySuccess(final PhaseContext ctx, FlowConstruct flowConstruct) {
    return successResult -> {
      fireNotification(ctx.messageProcessContext.getMessageSource(), successResult.getResult(),
                       flowConstruct, MESSAGE_RESPONSE);
      try {
        return from(ctx.template
            .sendResponseToClient(successResult.getResult(), successResult.getResponseParameters().get()))
                .doOnSuccess(v -> onTerminate(ctx.terminateConsumer, right(successResult.getResult())))
                .onErrorResume(e -> policySuccessError(new SourceErrorException(successResult.getResult(),
                                                                                sourceResponseSendErrorType, e),
                                                       successResult, ctx, flowConstruct));
      } catch (Exception e) {
        return policySuccessError(new SourceErrorException(successResult.getResult(), sourceResponseGenerateErrorType, e),
                                  successResult, ctx, flowConstruct);
      }
    };
  }

  /*
   * Process failure success by attempting to send an error response to client handling the case where error response sending
   * fails or the resolution of error response parameters fails.
   */
  private Function<SourcePolicyFailureResult, Mono<Void>> policyFailure(final PhaseContext ctx, FlowConstruct flowConstruct) {
    return failureResult -> {
      fireNotification(ctx.messageProcessContext.getMessageSource(), failureResult.getMessagingException().getEvent(),
                       flowConstruct, MESSAGE_ERROR_RESPONSE);
      return sendErrorResponse(failureResult.getMessagingException(), event -> failureResult.getErrorResponseParameters().get(),
                               ctx);
    };
  }

  /*
   * Handle errors caused when attempting to process a success response by invoking flow error handler and disregarding the result
   * and sending an error response.
   */
  private Mono<Void> policySuccessError(SourceErrorException see, SourcePolicySuccessResult successResult, PhaseContext ctx,
                                        FlowConstruct flowConstruct) {
    MessagingException messagingException = see.toMessagingException();
    return when(just(messagingException).flatMapMany(flowConstruct.getExceptionListener()).last()
        .onErrorResume(e -> empty()),
                sendErrorResponse(messagingException, successResult.createErrorResponseParameters(), ctx)
                    .doOnSuccess(v -> onTerminate(ctx.terminateConsumer, right(see.getEvent())))).then();
  }

  /*
   * Send an error response. This may be due to an error being propagated from the Flow or fue to a failure sending a success
   * response. Error caused by failures in the flow error handler do not result in an error message being sent.
   */
  private Mono<Void> sendErrorResponse(MessagingException messagingException,
                                       Function<Event, Map<String, Object>> errorParameters,
                                       final PhaseContext ctx) {
    Event event = messagingException.getEvent();
    if (messagingException.inErrorHandler()) {
      return error(new SourceErrorException(event, sourceErrorResponseGenerateErrorType, messagingException.getCause(),
                                            messagingException));
    } else {
      try {
        return from(ctx.template
            .sendFailureResponseToClient(messagingException, errorParameters.apply(event)))
                .onErrorMap(e -> new SourceErrorException(builder(messagingException.getEvent())
                    .error(builder(e).errorType(sourceErrorResponseSendErrorType).build()).build(),
                                                          sourceErrorResponseSendErrorType, e))
                .doOnSuccess(v -> onTerminate(ctx.terminateConsumer, left(messagingException)));
      } catch (Exception e) {
        return error(new SourceErrorException(event, sourceErrorResponseGenerateErrorType, e, messagingException));
      }
    }
  }

  /*
   * Consumer invoked when processing of fails due to an error sending an error response, of because the error originated from
   * within an error handler.
   */
  private Consumer<Throwable> onFailure(PhaseResultNotifier phaseResultNotifier,
                                        Consumer<Either<MessagingException, Event>> terminateConsumer) {
    return throwable -> {
      onTerminate(terminateConsumer, left(throwable));
      throwable = throwable instanceof SourceErrorException ? throwable.getCause() : throwable;
      Exception failureException = throwable instanceof Exception ? (Exception) throwable : new DefaultMuleException(throwable);
      phaseResultNotifier.phaseFailure(failureException);
    };
  }

  private Consumer<Either<MessagingException, Event>> getTerminateConsumer(MessageSource messageSource,
                                                                           ModuleFlowProcessingPhaseTemplate template) {
    return eventOrException -> template.sendAfterTerminateResponseToClient(eventOrException.mapLeft(messagingException -> {
      messagingException.setProcessedEvent(createErrorEvent(messagingException.getEvent(), messageSource, messagingException,
                                                            muleContext.getErrorTypeLocator()));
      return messagingException;
    }));
  }

  private Event createEvent(ModuleFlowProcessingPhaseTemplate template, ComponentLocation sourceLocation,
                            Publisher<Void> responseCompletion, FlowConstruct flowConstruct)
      throws MuleException {
    Message message = template.getMessage();
    Event templateEvent =
        builder(create(flowConstruct, sourceLocation, null, responseCompletion)).message(message)
            .flow(flowConstruct)
            .build();

    if (message.getPayload().getValue() instanceof SourceResultAdapter) {
      SourceResultAdapter adapter = (SourceResultAdapter) message.getPayload().getValue();
      final Result<?, ?> result = adapter.getResult();
      final Object resultValue = result.getOutput();

      if (resultValue instanceof Collection && adapter.isCollection()) {
        message = toMessage(Result.<Collection<Message>, TypedValue>builder()
            .output(toMessageCollection((Collection<Result>) resultValue, adapter.getCursorProviderFactory(),
                                        templateEvent))
            .mediaType(result.getMediaType().orElse(ANY))
            .build());
      } else {
        message = toMessage(result, result.getMediaType().orElse(ANY), adapter.getCursorProviderFactory(), templateEvent);
      }

      templateEvent = builder(templateEvent).message(message).build();
    }
    return templateEvent;
  }


  private Event emptyEvent(Event request) {
    return builder(request).message(of(null)).build();
  }

  /**
   * This method will not throw any {@link Exception}.
   *
   * @param terminateConsumer the action to perform on the transformed result.
   * @param result the outcome of trying to send the response of the source through the source. In the case of error, only
   *        {@link MessagingException} or {@link SourceErrorException} are valid values on the {@code left} side of this
   *        parameter.
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

  private class FlowProcessor implements Processor {

    private final ModuleFlowProcessingPhaseTemplate template;
    private final Event templateEvent;

    public FlowProcessor(ModuleFlowProcessingPhaseTemplate template, Event templateEvent) {
      this.template = template;
      this.templateEvent = templateEvent;
    }

    @Override
    public Event process(Event event) throws MuleException {
      return processToApply(event, this);
    }

    @Override
    public Publisher<Event> apply(Publisher<Event> publisher) {
      return from(publisher).then(request -> from(template.routeEventAsync(request))
          .switchIfEmpty(fromCallable(() -> emptyEvent(templateEvent))));
    }
  }

  /*
   * Container for passing relevant context between private methods to avoid long method signatures everywhere.
   */
  private static final class PhaseContext {

    final ModuleFlowProcessingPhaseTemplate template;
    final MessageProcessContext messageProcessContext;
    final PhaseResultNotifier phaseResultNotifier;
    final Consumer<Either<MessagingException, Event>> terminateConsumer;

    PhaseContext(ModuleFlowProcessingPhaseTemplate template,
                 MessageProcessContext messageProcessContext,
                 PhaseResultNotifier phaseResultNotifier,
                 Consumer<Either<MessagingException, Event>> terminateConsumer) {
      this.template = template;
      this.messageProcessContext = messageProcessContext;
      this.phaseResultNotifier = phaseResultNotifier;
      this.terminateConsumer = terminateConsumer;
    }
  }

}
