/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Collections.emptyMap;
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
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.internal.message.ErrorBuilder.builder;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.core.internal.util.InternalExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessageCollection;
import static org.mule.runtime.core.internal.util.rx.RxUtils.createRoundRobinFluxSupplier;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
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
import org.mule.runtime.core.internal.processor.interceptor.CompletableInterceptorSourceCallbackAdapter;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.internal.util.mediatype.MediaTypeDecoratedResultCollection;
import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.xml.namespace.QName;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

/**
 * This phase routes the message through the flow.
 * <p>
 * To participate of this phase, {@link MessageProcessTemplate} must implement {@link ModuleFlowProcessingPhaseTemplate}
 * <p>
 * This implementation will know how to process messages from extension's sources
 */
public class ModuleFlowProcessingPhase
    extends NotificationFiringProcessingPhase<ModuleFlowProcessingPhaseTemplate> implements Initialisable, Startable, Stoppable {

  private static final Logger LOGGER = getLogger(ModuleFlowProcessingPhase.class);

  private ErrorType sourceResponseGenerateErrorType;
  private ErrorType sourceResponseSendErrorType;
  private ErrorType sourceErrorResponseGenerateErrorType;
  private ErrorType sourceErrorResponseSendErrorType;
  private ConfigurationComponentLocator componentLocator;
  private FluxSinkSupplier<PhaseContext> dispatchFlux;
  private FluxSinkSupplier<PhaseContext> responseFlux;
  private FluxSinkSupplier<PhaseContext> terminationFlux;

  private final PolicyManager policyManager;

  private final List<CompletableInterceptorSourceCallbackAdapter> additionalInterceptors = new LinkedList<>();

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
        CompletableInterceptorSourceCallbackAdapter reactiveInterceptorAdapter =
            new CompletableInterceptorSourceCallbackAdapter(interceptorFactory);
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
  public void start() throws MuleException {
    //Consumer<PhaseContext> onMessageReceive = onMessageReceived();
    //just(templateEvent)
    //    .doOnNext(ctx -> {
    //      fireNotification(ctx.messageProcessContext.getMessageSource(), ctx.event, ctx.flowConstruct, MESSAGE_RECEIVED);
    //      ctx.template.getNotificationFunctions().forEach(notificationFunction -> muleContext.getNotificationManager()
    //          .fireNotification(notificationFunction.apply(ctx.event, ctx.messageProcessContext.getMessageSource())))
    //    })
    //    // Check back pressure against source dependant strategy
    //    .doOnNext(ctx -> ctx.flowConstruct.checkBackpressure)
    //    // Process policy and in turn flow emitting Either<SourcePolicyFailureResult,SourcePolicySuccessResult>> when
    //    // complete.
    //    .flatMap(request -> from(policy.process(request, template)))
    //    // In case back pressure was fired, the exception will be propagated as a SourcePolicyFailureResult, wrapping inside
    //    // the back pressure exception
    //    .onErrorResume(FlowBackPressureException.class,
    //                   mapBackPressureExceptionToPolicyFailureResult(template, templateEvent))
    //    // Perform processing of result by sending success or error response and handle errors that occur.
    //    // Returns Publisher<Void> to signal when this is complete or if it failed.
    //    .flatMap(policyResult -> policyResult.reduce(policyFailure(phaseContext, flowConstruct, messageSource),
    //                                                 policySuccess(phaseContext, flowConstruct, messageSource)))
    //    .doOnSuccess(aVoid -> phaseResultNotifier.phaseSuccessfully())
    //    .doOnError(onFailure(flowConstruct, messageSource, phaseResultNotifier, terminateConsumer))
    //    // Complete EventContext via responseCompletion Mono once everything is done.
    //    .doAfterTerminate(() -> responseCompletion.complete(null))
    //    .subscribe();

    int roundRobinSize = Runtime.getRuntime().availableProcessors() * 2;
    terminationFlux = createRoundRobinFluxSupplier(flux -> {
      return flux
          .doOnNext(ctx -> {
            finish(ctx);
          });
    }, roundRobinSize);


    responseFlux = createRoundRobinFluxSupplier(flux -> {
      return flux
          .doOnNext(ctx -> {

            ctx.result.apply(policyFailure(ctx), policySuccess(ctx));

          });
    }, roundRobinSize);

    dispatchFlux = createRoundRobinFluxSupplier(flux -> {
      return flux
          .doOnNext(ctx -> {
            onMessageReceived();
            ctx.flowConstruct.checkBackpressure(ctx.event);
            ctx.sourcePolicy.process(ctx.event, ctx.template,
                                     new CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>() {

                                       @Override
                                       public void complete(Either<SourcePolicyFailureResult, SourcePolicySuccessResult> value) {
                                         ctx.result = value;
                                         responseFlux.get().next(ctx);
                                       }

                                       @Override
                                       public void error(Throwable e) {
                                         // TODO: Y aca??????
                                         ctx.result = left(new SourcePolicyFailureResult(new MessagingException(ctx.event, e), () -> emptyMap()));
                                         responseFlux.get().next(ctx);
                                       }
                                     });
          }).onErrorContinue(FlowBackPressureException.class, (e, phaseContext) -> {
            PhaseContext ctx = (PhaseContext) phaseContext;
            ctx.result = mapBackPressureExceptionToPolicyFailureResult(ctx.template, ctx.event, (FlowBackPressureException) e);
            responseFlux.get().next(ctx);
          });
    }, roundRobinSize);
  }

  private void finish(PhaseContext ctx) {
    try {
      if (ctx.exception != null) {
        onFailure(ctx).accept(ctx.exception);
      } else {
        ctx.phaseResultNotifier.phaseSuccessfully();
      }
    } finally {
      ctx.responseCompletion.complete(null);
    }
  }

  @Override
  public void stop() throws MuleException {
    // yes, you read right. At stop, we need to dispose the flux as that's what stops it.
    disposeIfNeeded(dispatchFlux, LOGGER);
    disposeIfNeeded(responseFlux, LOGGER);
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
      final CompletableFuture<Void> responseCompletion = new CompletableFuture<>();
      final FlowProcessor flowExecutionProcessor = new FlowProcessor(template, flowConstruct);
      CoreEvent event = createEvent(template, messageSource, responseCompletion, flowConstruct);

      try {
        final SourcePolicy policy =
            policyManager.createSourcePolicyInstance(messageSource, event, flowExecutionProcessor, template);

        final PhaseContext phaseContext = new PhaseContext(template,
                                                           messageSource,
                                                           messageProcessContext,
                                                           phaseResultNotifier,
                                                           flowConstruct,
                                                           getTerminateConsumer(messageSource, template),
                                                           responseCompletion,
                                                           event,
                                                           policy);

        dispatchFlux.get().next(phaseContext);

      } catch (Exception e) {
        template.sendFailureResponseToClient(
            new MessagingExceptionResolver(messageProcessContext.getMessageSource()).resolve(new MessagingException(event, e), muleContext),
            template.getFailedExecutionResponseParametersFunction().apply(event),
            CompletableCallback.<Void>empty().finallyAfter(() -> phaseResultNotifier.phaseFailure(e)));
      }
    } catch (Exception t) {
      phaseResultNotifier.phaseFailure(t);
    }
  }

  /**
   * Notifies the {@link FlowConstruct} response listening party of the backpressure signal raised when trying to inject the
   * event for processing into the {@link ProcessingStrategy}.
   * <p>
   * By wrapping the thrown backpressure exception in an {@link Either} which contains the {@link SourcePolicyFailureResult}, one
   * can consider as if the backpressure signal was fired from inside the policy + flow execution chain, and reuse all handling
   * logic.
   *
   * @param template the processing template being used
   * @param event    the event that caused the backpressure signal to be fired
   * @return an exception mapper that notifies the {@link FlowConstruct} response listener of the backpressure signal
   */
  protected Either<SourcePolicyFailureResult, SourcePolicySuccessResult> mapBackPressureExceptionToPolicyFailureResult(
      ModuleFlowProcessingPhaseTemplate template,
      CoreEvent event,
      FlowBackPressureException exception) {

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

    return left(result);
  }

  /*
   * Consumer invoked for each new execution of this processing phase.
   */
  private Consumer<PhaseContext> onMessageReceived() {
    return ctx -> {
      fireNotification(ctx.messageProcessContext.getMessageSource(), ctx.event, ctx.flowConstruct, MESSAGE_RECEIVED);
      ctx.template.getNotificationFunctions().forEach(notificationFunction -> muleContext.getNotificationManager()
          .fireNotification(notificationFunction.apply(ctx.event, ctx.messageProcessContext.getMessageSource())));
    };
  }

  /**
   * Process success by attempting to send a response to client handling the case where response sending fails or the resolution
   * of response parameters fails.
   */
  private Consumer<SourcePolicySuccessResult> policySuccess(PhaseContext ctx) {
    return successResult -> {
      fireNotification(ctx.messageProcessContext.getMessageSource(), successResult.getEvent(),
                       ctx.flowConstruct, MESSAGE_RESPONSE);

      BiConsumer<SourcePolicySuccessResult, CompletableCallback<Void>> sendResponseToClient =
          (result, responseCallback) -> ctx.template.sendResponseToClient(result.getEvent(),
                                                                          result.getResponseParameters().get(),
                                                                          responseCallback);

      CompletableCallback<Void> responseCallback = new CompletableCallback<Void>() {

        @Override
        public void complete(Void value) {
          onTerminate(ctx, right(successResult.getEvent()));
          finish(ctx);
        }

        @Override
        public void error(Throwable e) {
          policySuccessError(new SourceErrorException(successResult.getEvent(), sourceResponseSendErrorType, e),
                             successResult,
                             ctx);
        }
      };

      try {
        for (CompletableInterceptorSourceCallbackAdapter interceptor : additionalInterceptors) {
          sendResponseToClient = interceptor.apply(ctx.messageSource, sendResponseToClient);
        }

        sendResponseToClient.accept(successResult, responseCallback);
      } catch (Exception e) {
        policySuccessError(new SourceErrorException(successResult.getEvent(), sourceResponseGenerateErrorType, e),
                           successResult,
                           ctx);
      }
    };
  }

  /**
   * Process failure success by attempting to send an error response to client handling the case where error response sending
   * fails or the resolution of error response parameters fails.
   */
  private Consumer<SourcePolicyFailureResult> policyFailure(PhaseContext ctx) {
    return failureResult -> {
      fireNotification(ctx.messageProcessContext.getMessageSource(), failureResult.getMessagingException().getEvent(),
                       ctx.flowConstruct, MESSAGE_ERROR_RESPONSE);

      sendErrorResponse(failureResult.getMessagingException(),
                        event -> failureResult.getErrorResponseParameters().get(),
                        ctx, new CompletableCallback<Void>() {

            @Override
            public void complete(Void value) {
              onTerminate(ctx, left(failureResult.getMessagingException()));
              finish(ctx);
            }

            @Override
            public void error(Throwable e) {
              ctx.exception = e;
              finish(ctx);
            }
          });
    };
  }

  /**
   * Handle errors caused when attempting to process a success response by invoking flow error handler and disregarding the result
   * and sending an error response.
   */
  private void policySuccessError(SourceErrorException see, SourcePolicySuccessResult successResult, PhaseContext ctx) {

    MessagingException messagingException =
        see.toMessagingException(ctx.flowConstruct.getMuleContext().getExceptionContextProviders(), ctx.messageSource);

    just(messagingException).flatMapMany(ctx.flowConstruct.getExceptionListener()).last().onErrorResume(e -> empty()).subscribe();
    sendErrorResponse(messagingException, successResult.createErrorResponseParameters(), ctx, new CompletableCallback<Void>() {

      @Override
      public void complete(Void value) {
        onTerminate(ctx, left(messagingException));
        finish(ctx);
      }

      @Override
      public void error(Throwable e) {
        ctx.exception = e;
        finish(ctx);
      }
    });
  }

  /**
   * Send an error response. This may be due to an error being propagated from the Flow or due to a failure sending a success
   * response. Error caused by failures in the flow error handler do not result in an error message being sent.
   */
  private void sendErrorResponse(MessagingException messagingException,
                                 Function<CoreEvent, Map<String, Object>> errorParameters,
                                 final PhaseContext ctx,
                                 CompletableCallback<Void> callback) {

    CoreEvent event = messagingException.getEvent();

    try {
      ctx.template.sendFailureResponseToClient(messagingException, errorParameters.apply(event), new CompletableCallback<Void>() {

        @Override
        public void complete(Void value) {
          callback.complete(value);
        }

        @Override
        public void error(Throwable e) {
          callback.error(new SourceErrorException(builder(event)
                                                      .error(builder(e).errorType(sourceErrorResponseSendErrorType).build())
                                                      .build(),
                                                  sourceErrorResponseSendErrorType, e));
        }
      });
    } catch (Exception e) {
      callback.error(new SourceErrorException(event, sourceErrorResponseGenerateErrorType, e, messagingException));
    }
  }

  /*
   * Consumer invoked when processing fails due to an error sending an error response, of because the error originated from within
   * an error handler.
   */
  private Consumer<Throwable> onFailure(PhaseContext ctx) {
    return throwable -> {
      onTerminate(ctx, left(throwable));
      throwable = throwable instanceof SourceErrorException ? throwable.getCause() : throwable;
      Exception failureException = throwable instanceof Exception ? (Exception) throwable : new DefaultMuleException(throwable);
      ctx.phaseResultNotifier.phaseFailure(failureException);
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
                                     .output(toMessageCollection(
                                         new MediaTypeDecoratedResultCollection((Collection<Result>) resultValue,
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
   * @param ctx    the {@link PhaseContext}
   * @param result the outcome of trying to send the response of the source through the source. In the case of error, only
   *               {@link MessagingException} or {@link SourceErrorException} are valid values on the {@code left} side of this
   *               parameter.
   */
  private void onTerminate(PhaseContext ctx, Either<Throwable, CoreEvent> result) {
    safely(() -> ctx.terminateConsumer.accept(result.mapLeft(throwable -> {
      if (throwable instanceof MessagingException) {
        return (MessagingException) throwable;
      } else if (throwable instanceof SourceErrorException) {
        return ((SourceErrorException) throwable)
            .toMessagingException(ctx.flowConstruct.getMuleContext().getExceptionContextProviders(), ctx.messageSource);
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
      return applyWithChildContext(from(publisher),
                                   template::routeEventAsync,
                                   Optional.empty(),
                                   flowConstruct.getExceptionListener());
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


  /**
   * Container for passing relevant context between private methods to avoid long method signatures everywhere.
   */
  private static final class PhaseContext {

    private final ModuleFlowProcessingPhaseTemplate template;
    private final MessageSource messageSource;
    private final MessageProcessContext messageProcessContext;
    private final PhaseResultNotifier phaseResultNotifier;
    private final FlowConstruct flowConstruct;
    private final Consumer<Either<MessagingException, CoreEvent>> terminateConsumer;
    private final CompletableFuture<Void> responseCompletion;
    private final SourcePolicy sourcePolicy;
    private final CoreEvent event;
    private Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result;
    private Throwable exception;

    private PhaseContext(ModuleFlowProcessingPhaseTemplate template,
                         MessageSource messageSource,
                         MessageProcessContext messageProcessContext,
                         PhaseResultNotifier phaseResultNotifier,
                         FlowConstruct flowConstruct,
                         Consumer<Either<MessagingException, CoreEvent>> terminateConsumer,
                         CompletableFuture<Void> responseCompletion,
                         CoreEvent event,
                         SourcePolicy sourcePolicy) {
      this.template = template;
      this.messageSource = messageSource;
      this.messageProcessContext = messageProcessContext;
      this.phaseResultNotifier = phaseResultNotifier;
      this.flowConstruct = flowConstruct;
      this.terminateConsumer = terminateConsumer;
      this.responseCompletion = responseCompletion;
      this.event = event;
      this.sourcePolicy = sourcePolicy;
    }
  }
}
