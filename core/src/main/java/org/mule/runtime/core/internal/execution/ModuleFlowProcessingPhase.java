/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.internal.message.ErrorBuilder.builder;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.core.internal.util.InternalExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessageCollection;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromCallable;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.when;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.policy.SourcePolicy;
import org.mule.runtime.core.internal.policy.SourcePolicyFailureResult;
import org.mule.runtime.core.internal.policy.SourcePolicySuccessResult;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;
import org.mule.runtime.extension.api.runtime.operation.Result;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;

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
  private ConfigurationComponentLocator componentLocator;

  private final PolicyManager policyManager;

  public ModuleFlowProcessingPhase(PolicyManager policyManager) {
    this.policyManager = policyManager;
  }

  @Override
  public void initialise() throws InitialisationException {
    final ErrorTypeRepository errorTypeRepository = muleContext.getErrorTypeRepository();
    componentLocator = muleContext.getConfigurationComponentLocator();

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
      final FlowConstruct flowConstruct = (FlowConstruct) componentLocator.find(messageSource.getRootContainerLocation()).get();
      final ComponentLocation sourceLocation = messageSource.getLocation();
      final Consumer<Either<MessagingException, CoreEvent>> terminateConsumer = getTerminateConsumer(messageSource, template);
      final CompletableFuture<Void> responseCompletion = new CompletableFuture<>();
      final CoreEvent templateEvent = createEvent(template, sourceLocation, responseCompletion, flowConstruct);

      try {
        FlowProcessor flowExecutionProcessor = new FlowProcessor(template, flowConstruct.getExceptionListener(), templateEvent);
        flowExecutionProcessor.setAnnotations(flowConstruct.getAnnotations());
        final SourcePolicy policy =
            policyManager.createSourcePolicyInstance(messageSource, templateEvent, flowExecutionProcessor, template);
        final PhaseContext phaseContext =
            new PhaseContext(template, messageProcessContext, phaseResultNotifier, terminateConsumer);

        just(templateEvent)
            .doOnNext(onMessageReceived(messageProcessContext, flowConstruct))
            // Process policy and in turn flow emitting Either<SourcePolicyFailureResult,SourcePolicySuccessResult>> when
            // complete.
            .flatMap(request -> from(policy.process(request)))
            // Perform processing of result by sending success or error response and handle errors that occur.
            // Returns Publisher<Void> to signal when this is complete or if it failed.
            .flatMap(policyResult -> policyResult.reduce(policyFailure(phaseContext, flowConstruct),
                                                         policySuccess(phaseContext, flowConstruct)))
            .doOnSuccess(aVoid -> phaseResultNotifier.phaseSuccessfully())
            .doOnError(onFailure(phaseResultNotifier, terminateConsumer))
            // Complete EventContext via responseCompletion Mono once everything is done.
            .doAfterTerminate(() -> responseCompletion.complete(null))
            .subscribe();
      } catch (Exception e) {
        from(template.sendFailureResponseToClient(new MessagingExceptionResolver(messageProcessContext.getMessageSource())
            .resolve(new MessagingException(templateEvent, e), muleContext),
                                                  template.getFailedExecutionResponseParametersFunction().apply(templateEvent)))
                                                      .doOnTerminate(() -> phaseResultNotifier.phaseFailure(e)).subscribe();
      }
    } catch (Exception t) {
      phaseResultNotifier.phaseFailure(t);
    }
  }

  /*
   * Consumer invoked for each new execution of this processing phase.
   */
  private Consumer<CoreEvent> onMessageReceived(MessageProcessContext messageProcessContext, FlowConstruct flowConstruct) {
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
                               ctx)
                                   .doOnSuccess(v -> onTerminate(ctx.terminateConsumer,
                                                                 left(failureResult.getMessagingException())));
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
                    .doOnSuccess(v -> onTerminate(ctx.terminateConsumer, left(messagingException)))).then();
  }

  /*
   * Send an error response. This may be due to an error being propagated from the Flow or fue to a failure sending a success
   * response. Error caused by failures in the flow error handler do not result in an error message being sent.
   */
  private Mono<Void> sendErrorResponse(MessagingException messagingException,
                                       Function<CoreEvent, Map<String, Object>> errorParameters,
                                       final PhaseContext ctx) {
    CoreEvent event = messagingException.getEvent();
    if (messagingException.inErrorHandler()) {
      return error(new SourceErrorException(event, sourceErrorResponseGenerateErrorType, messagingException.getCause(),
                                            messagingException));
    } else {
      try {
        return from(ctx.template
            .sendFailureResponseToClient(messagingException, errorParameters.apply(event)))
                .onErrorMap(e -> new SourceErrorException(builder(messagingException.getEvent())
                    .error(builder(e).errorType(sourceErrorResponseSendErrorType).build()).build(),
                                                          sourceErrorResponseSendErrorType, e));
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
                                        Consumer<Either<MessagingException, CoreEvent>> terminateConsumer) {
    return throwable -> {
      onTerminate(terminateConsumer, left(throwable));
      throwable = throwable instanceof SourceErrorException ? throwable.getCause() : throwable;
      Exception failureException = throwable instanceof Exception ? (Exception) throwable : new DefaultMuleException(throwable);
      phaseResultNotifier.phaseFailure(failureException);
    };
  }

  private Consumer<Either<MessagingException, CoreEvent>> getTerminateConsumer(MessageSource messageSource,
                                                                               ModuleFlowProcessingPhaseTemplate template) {
    return eventOrException -> template.afterPhaseExecution(eventOrException.mapLeft(messagingException -> {
      messagingException.setProcessedEvent(createErrorEvent(messagingException.getEvent(), messageSource, messagingException,
                                                            ((PrivilegedMuleContext) muleContext).getErrorTypeLocator()));
      return messagingException;
    }));
  }

  private CoreEvent createEvent(ModuleFlowProcessingPhaseTemplate template, ComponentLocation sourceLocation,
                                CompletableFuture responseCompletion, FlowConstruct flowConstruct) {
    Message message = template.getMessage();
    CoreEvent templateEvent = InternalEvent
        .builder(create(flowConstruct.getUniqueIdString(), flowConstruct.getServerId(), sourceLocation, null,
                        Optional.of(responseCompletion),
                        NullExceptionHandler.getInstance()))
        .message(message)
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
        message = toMessage(result, adapter.getMediaType(), adapter.getCursorProviderFactory(), templateEvent);
      }

      templateEvent = builder(templateEvent).message(message).build();
    }
    return templateEvent;
  }


  private CoreEvent emptyEvent(CoreEvent request) {
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
  private void onTerminate(Consumer<Either<MessagingException, CoreEvent>> terminateConsumer,
                           Either<Throwable, CoreEvent> result) {
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

  private class FlowProcessor extends AbstractComponent implements Processor {

    private final ModuleFlowProcessingPhaseTemplate template;
    private final CoreEvent templateEvent;
    private final FlowExceptionHandler messagingExceptionHandler;

    public FlowProcessor(ModuleFlowProcessingPhaseTemplate template, FlowExceptionHandler messagingExceptionHandler,
                         CoreEvent templateEvent) {
      this.template = template;
      this.templateEvent = templateEvent;
      this.messagingExceptionHandler = messagingExceptionHandler;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return processToApply(event, this);
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return from(publisher)
          .flatMapMany(event -> processWithChildContext(event,
                                                        p -> from(p).flatMapMany(e -> template.routeEventAsync(e))
                                                            .switchIfEmpty(fromCallable(() -> emptyEvent(templateEvent))),
                                                        Optional.empty(), messagingExceptionHandler));
    }
  }

  /*
   * Container for passing relevant context between private methods to avoid long method signatures everywhere.
   */
  private static final class PhaseContext {

    final ModuleFlowProcessingPhaseTemplate template;
    final MessageProcessContext messageProcessContext;
    final PhaseResultNotifier phaseResultNotifier;
    final Consumer<Either<MessagingException, CoreEvent>> terminateConsumer;

    PhaseContext(ModuleFlowProcessingPhaseTemplate template,
                 MessageProcessContext messageProcessContext,
                 PhaseResultNotifier phaseResultNotifier,
                 Consumer<Either<MessagingException, CoreEvent>> terminateConsumer) {
      this.template = template;
      this.messageProcessContext = messageProcessContext;
      this.phaseResultNotifier = phaseResultNotifier;
      this.terminateConsumer = terminateConsumer;
    }
  }

}
