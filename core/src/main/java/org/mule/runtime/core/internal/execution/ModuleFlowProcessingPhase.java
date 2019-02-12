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
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.internal.message.ErrorBuilder.builder;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.core.internal.util.InternalExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessageCollection;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.when;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
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
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.interception.InterceptorManager;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalEvent.Builder;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.policy.SourcePolicy;
import org.mule.runtime.core.internal.policy.SourcePolicyFailureResult;
import org.mule.runtime.core.internal.policy.SourcePolicySuccessResult;
import org.mule.runtime.core.internal.processor.interceptor.ReactiveInterceptorSourceCallbackAdapter;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.internal.util.mediatype.MediaTypeDecoratedResultCollection;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;
import org.mule.runtime.core.privileged.processor.MessageProcessors;
import org.mule.runtime.extension.api.runtime.operation.Result;

import org.reactivestreams.Publisher;

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

  private final List<ReactiveInterceptorSourceCallbackAdapter> additionalInterceptors = new LinkedList<>();

  @Inject
  private InterceptorManager processorInterceptorManager;

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

    if (processorInterceptorManager != null) {
      processorInterceptorManager.getSourceInterceptorFactories().stream().forEach(interceptorFactory -> {
        ReactiveInterceptorSourceCallbackAdapter reactiveInterceptorAdapter =
            new ReactiveInterceptorSourceCallbackAdapter(interceptorFactory);
        try {
          muleContext.getInjector().inject(reactiveInterceptorAdapter);
        } catch (MuleException e) {
          throw new MuleRuntimeException(e);
        }
        additionalInterceptors.add(0, reactiveInterceptorAdapter);
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
            // Process policy and in turn flow emitting Either<SourcePolicyFailureResult,SourcePolicySuccessResult>> when
            // complete.
            .flatMap(request -> from(policy.process(request, template))
                // Perform processing of result by sending success or error response and handle errors that occur.
                // Returns Publisher<Void> to signal when this is complete or if it failed.
                .flatMap(policyResult -> policyResult.reduce(policyFailure(phaseContext, flowConstruct, messageSource),
                                                             policySuccess(phaseContext, flowConstruct, messageSource))))
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
      }
    } catch (Exception t) {
      phaseResultNotifier.phaseFailure(t);
    }
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
        for (ReactiveInterceptorSourceCallbackAdapter interceptor : additionalInterceptors) {
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
      return sendErrorResponse(failureResult.getMessagingException(), event -> failureResult.getErrorResponseParameters().get(),
                               ctx, flowConstruct)
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
      return from(ctx.template
          .sendFailureResponseToClient(messagingException, errorParameters.apply(event)))
              .onErrorMap(e -> new SourceErrorException(builder(event)
                  .error(builder(e).errorType(sourceErrorResponseSendErrorType).build()).build(),
                                                        sourceErrorResponseSendErrorType, e));
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
    Message message = template.getMessage();
    Builder eventBuilder;

    if (message.getPayload().getValue() instanceof SourceResultAdapter) {
      SourceResultAdapter adapter = (SourceResultAdapter) message.getPayload().getValue();
      eventBuilder =
          createEventBuilder(source.getLocation(), responseCompletion, flowConstruct, adapter.getCorrelationId().orElse(null),
                             message);

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
    } else {
      eventBuilder = createEventBuilder(source.getLocation(), responseCompletion, flowConstruct, null, message);
      policyManager.addSourcePointcutParametersIntoEvent(source, message.getAttributes(), eventBuilder);
    }

    return eventBuilder.build();
  }

  private Builder createEventBuilder(ComponentLocation sourceLocation, CompletableFuture<Void> responseCompletion,
                                     FlowConstruct flowConstruct, String correlationId, Message message) {
    return InternalEvent
        .builder(create(flowConstruct, NullExceptionHandler.getInstance(), sourceLocation, correlationId,
                        Optional.of(responseCompletion)))
        .message(message);
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
          // .transform(p -> applyWithChildContext(p, pub -> template.routeEventAsync(pub),
          // Optional.empty(), flowConstruct.getExceptionListener()));
          // .compose(p -> applyWithChildContext(p, pub -> template.routeEventAsync(pub),
          // Optional.empty(), flowConstruct.getExceptionListener()));
          .flatMap(p -> MessageProcessors.processWithChildContext(p, pub -> template.routeEventAsync(pub),
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
