/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

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
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Unhandleable.FLOW_BACK_PRESSURE;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.api.util.ExceptionUtils.containsType;
import static org.mule.runtime.core.internal.message.ErrorBuilder.builder;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.core.internal.util.InternalExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessageCollection;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextDontComplete;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.when;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.FlowBackPressureException;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.interception.InterceptorManager;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalEvent.Builder;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.policy.SourcePolicy;
import org.mule.runtime.core.internal.policy.SourcePolicyFailureResult;
import org.mule.runtime.core.internal.policy.SourcePolicySuccessResult;
import org.mule.runtime.core.internal.processor.interceptor.AbstractReactiveInterceptorSourceCallbackAdapter;
import org.mule.runtime.core.internal.processor.interceptor.ReactiveInterceptorSourceFailureCallbackAdapter;
import org.mule.runtime.core.internal.processor.interceptor.ReactiveInterceptorSourceSuccessCallbackAdapter;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.internal.util.mediatype.MediaTypeDecoratedResultCollection;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.xml.namespace.QName;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
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

  private ErrorType sourceResponseGenerateErrorType;
  private ErrorType sourceResponseSendErrorType;
  private ErrorType sourceErrorResponseGenerateErrorType;
  private ErrorType sourceErrorResponseSendErrorType;
  private ConfigurationComponentLocator componentLocator;

  private final PolicyManager policyManager;

  private final List<ReactiveInterceptorSourceSuccessCallbackAdapter> additionalSuccessInterceptors = new LinkedList<>();
  private final List<ReactiveInterceptorSourceFailureCallbackAdapter> additionalFailureInterceptors = new LinkedList<>();

  @Inject
  private InterceptorManager processorInterceptorManager;
  private ErrorType flowBackPressureErrorType;

  public ModuleFlowProcessingPhase(PolicyManager policyManager) {
    this.policyManager = policyManager;
  }

  @Override
  public void initialise() throws InitialisationException {
    final ErrorTypeRepository errorTypeRepository = muleContext.getErrorTypeRepository();
    componentLocator = muleContext.getConfigurationComponentLocator();

    sourceResponseGenerateErrorType = errorTypeRepository.getErrorType(SOURCE_RESPONSE_GENERATE).get();
    sourceResponseSendErrorType = errorTypeRepository.getErrorType(SOURCE_RESPONSE_SEND).get();
    sourceErrorResponseGenerateErrorType = errorTypeRepository.getErrorType(SOURCE_ERROR_RESPONSE_GENERATE).get();
    sourceErrorResponseSendErrorType = errorTypeRepository.getErrorType(SOURCE_ERROR_RESPONSE_SEND).get();
    flowBackPressureErrorType = errorTypeRepository.getErrorType(FLOW_BACK_PRESSURE).get();

    if (processorInterceptorManager != null) {
      processorInterceptorManager.getSourceInterceptorFactories().stream().forEach(interceptorFactory -> {
        ReactiveInterceptorSourceSuccessCallbackAdapter reactiveInterceptorSuccessAdapter =
            new ReactiveInterceptorSourceSuccessCallbackAdapter(interceptorFactory);
        ReactiveInterceptorSourceFailureCallbackAdapter reactiveInterceptorFailureAdapter =
            new ReactiveInterceptorSourceFailureCallbackAdapter(interceptorFactory);
        try {
          muleContext.getInjector().inject(reactiveInterceptorSuccessAdapter);
          muleContext.getInjector().inject(reactiveInterceptorFailureAdapter);
        } catch (MuleException e) {
          throw new MuleRuntimeException(e);
        }
        additionalSuccessInterceptors.add(0, reactiveInterceptorSuccessAdapter);
        additionalFailureInterceptors.add(0, reactiveInterceptorFailureAdapter);
      });
    }
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
      final Consumer<Either<MessagingException, CoreEvent>> terminateConsumer = getTerminateConsumer(messageSource, template);
      final CompletableFuture<Void> responseCompletion = new CompletableFuture<>();
      final CoreEvent templateEvent = createEvent(template, messageSource, responseCompletion, flowConstruct);

      try {
        FlowProcessor flowExecutionProcessor = new FlowProcessor(template, flowConstruct);
        final SourcePolicy policy =
            policyManager.createSourcePolicyInstance(messageSource, templateEvent, flowExecutionProcessor, template);
        final PhaseContext phaseContext =
            new PhaseContext(template, messageProcessContext, phaseResultNotifier, terminateConsumer);

        just(templateEvent)
            .doOnNext(onMessageReceived(template, messageProcessContext, flowConstruct))
            // Check backpressure against source dependant strategy
            .doOnNext(flowConstruct::checkBackpressure)
            // Process policy and in turn flow emitting Either<SourcePolicyFailureResult,SourcePolicySuccessResult>> when
            // complete.
            .flatMap(request -> from(policy.process(request, template)))
            // In case backpressure was fired, the exception will be propagated as a SourcePolicyFailureResult, wrapping inside
            // the backpressure exception
            .onErrorResume(FlowBackPressureException.class,
                           ex -> {
                             ((BaseEventContext) templateEvent.getContext()).error(ex);
                             return mapBackPressureExceptionToPolicyFailureResult(template, templateEvent).apply(ex);
                           })
            // Perform processing of result by sending success or error response and handle errors that occur.
            // Returns Publisher<Void> to signal when this is complete or if it failed.
            .flatMap(policyResult -> policyResult.reduce(policyFailure(phaseContext, flowConstruct, messageSource),
                                                         policySuccess(phaseContext, flowConstruct, messageSource)))
            .doOnSuccess(aVoid -> phaseResultNotifier.phaseSuccessfully())
            .doOnError(onFailure(flowConstruct, messageSource, phaseResultNotifier, terminateConsumer))
            // Complete EventContext via responseCompletion Mono once everything is done.
            .doAfterTerminate(() -> responseCompletion.complete(null))
            .subscribe();
      } catch (Exception e) {
        from(template.sendFailureResponseToClient(new MessagingExceptionResolver(messageProcessContext.getMessageSource())
            .resolve(new MessagingException(templateEvent, e), muleContext),
                                                  template.getFailedExecutionResponseParametersFunction().apply(templateEvent)))
                                                      .doOnTerminate(() -> phaseResultNotifier.phaseFailure(e)).subscribe();

        ((BaseEventContext) templateEvent.getContext()).error(e);
        responseCompletion.complete(null);
      }
    } catch (Exception t) {
      phaseResultNotifier.phaseFailure(t);
    }
  }

  /**
   * Notifies the {@link FlowConstruct} response listenting party of the backpressure signal raised when trying to inject the
   * event for processing into the {@link org.mule.runtime.core.api.processor.strategy.ProcessingStrategy}.
   * <p>
   * By wrapping the thrown backpressure exception in an {@link Either} which contains the {@link SourcePolicyFailureResult}, one
   * can consider as if the backpressure signal was fired from inside the policy + flow execution chain, and reuse all handling
   * logic.
   *
   * @param template the processing template being used
   * @param event the event that caused the backpressure signal to be fired
   * @return an exception mapper that notifies the {@link FlowConstruct} response listener of the backpressure signal
   */
  protected Function<FlowBackPressureException, Mono<? extends Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>> mapBackPressureExceptionToPolicyFailureResult(ModuleFlowProcessingPhaseTemplate template,
                                                                                                                                                                            CoreEvent event) {
    return exception -> {

      // Build error event
      CoreEvent errorEvent =
          CoreEvent.builder(event)
              .error(ErrorBuilder.builder(exception)
                  .errorType(flowBackPressureErrorType)
                  .build())
              .build();

      // Since the decision whether the event is handled by the source onError or onBackPressure callback is made in
      // SourceAdapter by checking the ErrorType, the exception is wrapped
      SourcePolicyFailureResult result =
          new SourcePolicyFailureResult(new MessagingException(errorEvent, exception),
                                        () -> template.getFailedExecutionResponseParametersFunction().apply(errorEvent));

      return just(left(result));
    };
  }

  /*
   * Consumer invoked for each new execution of this processing phase.
   */
  private Consumer<CoreEvent> onMessageReceived(ModuleFlowProcessingPhaseTemplate template,
                                                MessageProcessContext messageProcessContext, FlowConstruct flowConstruct) {
    return request -> {
      fireNotification(messageProcessContext.getMessageSource(), request, flowConstruct, MESSAGE_RECEIVED);
      template.getNotificationFunctions().forEach(notificationFunction -> muleContext.getNotificationManager()
          .fireNotification(notificationFunction.apply(request, messageProcessContext.getMessageSource())));
    };
  }

  /*
   * Process success by attempting to send a response to client handling the case where response sending fails or the resolution
   * of response parameters fails.
   */
  private Function<SourcePolicySuccessResult, Mono<Void>> policySuccess(final PhaseContext ctx, FlowConstruct flowConstruct,
                                                                        MessageSource messageSource) {
    return successResult -> {
      fireNotification(ctx.messageProcessContext.getMessageSource(), successResult.getResult(),
                       flowConstruct, MESSAGE_RESPONSE);
      try {
        Function<SourcePolicySuccessResult, Publisher<Void>> sendResponseToClient =
            result -> ctx.template.sendResponseToClient(result.getResult(), result.getResponseParameters().get());
        for (ReactiveInterceptorSourceSuccessCallbackAdapter interceptor : additionalSuccessInterceptors) {
          sendResponseToClient = interceptor.apply(messageSource, sendResponseToClient);
        }

        return from(sendResponseToClient.apply(successResult))
            .doOnSuccess(v -> onTerminate(flowConstruct, messageSource, ctx.terminateConsumer,
                                          right(successResult.getResult())))
            .onErrorResume(e -> policySuccessError(new SourceErrorException(successResult.getResult(),
                                                                            sourceResponseSendErrorType, e),
                                                   successResult, ctx, flowConstruct, messageSource));
      } catch (Exception e) {
        return policySuccessError(new SourceErrorException(successResult.getResult(), sourceResponseGenerateErrorType, e),
                                  successResult, ctx, flowConstruct, messageSource);
      }
    };
  }

  /*
   * Process failure success by attempting to send an error response to client handling the case where error response sending
   * fails or the resolution of error response parameters fails.
   */
  private Function<SourcePolicyFailureResult, Mono<Void>> policyFailure(final PhaseContext ctx, FlowConstruct flowConstruct,
                                                                        MessageSource messageSource) {
    return failureResult -> {
      fireNotification(ctx.messageProcessContext.getMessageSource(), failureResult.getMessagingException().getEvent(),
                       flowConstruct, MESSAGE_ERROR_RESPONSE);
      Function<SourcePolicyFailureResult, Publisher<Void>> sendErrorResponse =
          result -> sendErrorResponse(failureResult.getMessagingException(),
                                      event -> failureResult.getErrorResponseParameters().get(), ctx, flowConstruct);
      for (ReactiveInterceptorSourceFailureCallbackAdapter interceptor : additionalFailureInterceptors) {
        sendErrorResponse = interceptor.apply(messageSource, sendErrorResponse);
      }
      return from(sendErrorResponse.apply(failureResult))
          .doOnSuccess(v -> onTerminate(flowConstruct, messageSource, ctx.terminateConsumer,
                                        left(failureResult.getMessagingException())));
    };
  }

  /*
   * Handle errors caused when attempting to process a success response by invoking flow error handler and disregarding the result
   * and sending an error response.
   */
  private Mono<Void> policySuccessError(SourceErrorException see, SourcePolicySuccessResult successResult, PhaseContext ctx,
                                        FlowConstruct flowConstruct, MessageSource messageSource) {

    MessagingException messagingException =
        see.toMessagingException(flowConstruct.getMuleContext().getExceptionContextProviders(), messageSource);

    return when(just(messagingException).flatMapMany(flowConstruct.getExceptionListener()).last()
        .onErrorResume(e -> empty()),
                sendErrorResponse(messagingException, successResult.createErrorResponseParameters(), ctx, flowConstruct)
                    .doOnSuccess(v -> onTerminate(flowConstruct, messageSource, ctx.terminateConsumer,
                                                  left(messagingException))))
                                                      .then();
  }

  /*
   * Send an error response. This may be due to an error being propagated from the Flow or due to a failure sending a success
   * response. Error caused by failures in the flow error handler do not result in an error message being sent.
   */
  private Mono<Void> sendErrorResponse(MessagingException messagingException,
                                       Function<CoreEvent, Map<String, Object>> errorParameters,
                                       final PhaseContext ctx, FlowConstruct flowConstruct) {
    CoreEvent event = messagingException.getEvent();
    try {
      // When broken pipe happens there's not need to send failure response to client
      if (!containsType(messagingException, ConnectionException.class)) {
        return from(ctx.template
            .sendFailureResponseToClient(messagingException, errorParameters.apply(event)))
                .onErrorMap(e -> new SourceErrorException(builder(event)
                    .error(builder(e).errorType(sourceErrorResponseSendErrorType).build()).build(),
                                                          sourceErrorResponseSendErrorType, e));
      } else {
        return empty();
      }
    } catch (Exception e) {
      return error(new SourceErrorException(event, sourceErrorResponseGenerateErrorType, e, messagingException));
    }
  }

  /*
   * Consumer invoked when processing fails due to an error sending an error response, of because the error originated from within
   * an error handler.
   */
  private Consumer<Throwable> onFailure(FlowConstruct flowConstruct, MessageSource messageSource,
                                        PhaseResultNotifier phaseResultNotifier,
                                        Consumer<Either<MessagingException, CoreEvent>> terminateConsumer) {
    return throwable -> {
      onTerminate(flowConstruct, messageSource, terminateConsumer, left(throwable));
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

  private CoreEvent createEvent(ModuleFlowProcessingPhaseTemplate template, MessageSource source,
                                CompletableFuture<Void> responseCompletion, FlowConstruct flowConstruct) {

    SourceResultAdapter adapter = template.getSourceMessage();
    Builder eventBuilder =
        createEventBuilder(source.getLocation(), responseCompletion, flowConstruct, adapter.getCorrelationId().orElse(null));

    return eventBuilder.message(eventCtx -> {
      final Result<?, ?> result = adapter.getResult();
      final Object resultValue = result.getOutput();

      Message eventMessage;
      if (resultValue instanceof Collection && adapter.isCollection()) {
        eventMessage = toMessage(Result.<Collection<Message>, TypedValue>builder()
            .output(toMessageCollection(new MediaTypeDecoratedResultCollection((Collection<Result>) resultValue,
                                                                               adapter.getPayloadMediaTypeResolver()),
                                        adapter.getCursorProviderFactory(),
                                        ((BaseEventContext) eventCtx).getRootContext()))
            .mediaType(result.getMediaType().orElse(ANY))
            .build());
      } else {
        eventMessage = toMessage(result, adapter.getMediaType(), adapter.getCursorProviderFactory(),
                                 ((BaseEventContext) eventCtx).getRootContext());
      }

      policyManager.addSourcePointcutParametersIntoEvent(source, eventMessage.getAttributes(), eventBuilder);
      return eventMessage;
    }).build();
  }

  private Builder createEventBuilder(ComponentLocation sourceLocation, CompletableFuture<Void> responseCompletion,
                                     FlowConstruct flowConstruct, String correlationId) {
    return InternalEvent
        .builder(create(flowConstruct, NullExceptionHandler.getInstance(), sourceLocation, correlationId,
                        Optional.of(responseCompletion)));
  }

  /**
   * This method will not throw any {@link Exception}.
   *
   * @param flowConstruct the flow being executed.
   * @param messageSource the source that triggered the flow execution.
   * @param terminateConsumer the action to perform on the transformed result.
   * @param result the outcome of trying to send the response of the source through the source. In the case of error, only
   *        {@link MessagingException} or {@link SourceErrorException} are valid values on the {@code left} side of this
   *        parameter.
   */
  private void onTerminate(FlowConstruct flowConstruct, MessageSource messageSource,
                           Consumer<Either<MessagingException, CoreEvent>> terminateConsumer,
                           Either<Throwable, CoreEvent> result) {
    safely(() -> terminateConsumer.accept(result.mapLeft(throwable -> {
      if (throwable instanceof MessagingException) {
        return (MessagingException) throwable;
      } else if (throwable instanceof SourceErrorException) {
        return ((SourceErrorException) throwable)
            .toMessagingException(flowConstruct.getMuleContext().getExceptionContextProviders(), messageSource);
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

  private class FlowProcessor implements Processor, Component {

    private final ModuleFlowProcessingPhaseTemplate template;
    private final FlowConstruct flowConstruct;

    public FlowProcessor(ModuleFlowProcessingPhaseTemplate template, FlowConstruct flowConstruct) {
      this.template = template;
      this.flowConstruct = flowConstruct;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return processToApply(event, this);
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return Flux.from(publisher)
          .flatMap(p -> processWithChildContextDontComplete(p, pub -> template.routeEventAsync(pub),
                                                            Optional.empty(), flowConstruct.getExceptionListener()));
    }

    @Override
    public Object getAnnotation(QName name) {
      return flowConstruct.getAnnotation(name);
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return flowConstruct.getAnnotations();
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ComponentLocation getLocation() {
      return flowConstruct.getLocation();
    }

    @Override
    public Location getRootContainerLocation() {
      return flowConstruct.getRootContainerLocation();
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
