/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.api.component.execution.CompletableCallback.always;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.runtime.api.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.FLOW_BACK_PRESSURE;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.util.ExceptionUtils.containsType;
import static org.mule.runtime.core.internal.message.ErrorBuilder.builder;
import static org.mule.runtime.core.internal.policy.SourcePolicyContext.from;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.mule.runtime.core.internal.util.InternalExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.internal.util.message.MessageUtils.messageCollection;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.getCurrentEvent;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.SourceRemoteConnectionException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.interception.SourceInterceptor;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.CorrelationIdGenerator;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.NotificationHelper;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.rx.Exceptions;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.AbstractPipeline;
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
import org.mule.runtime.core.internal.processor.interceptor.CompletableInterceptorSourceFailureCallbackAdapter;
import org.mule.runtime.core.internal.processor.interceptor.CompletableInterceptorSourceSuccessCallbackAdapter;
import org.mule.runtime.core.internal.util.mediatype.MediaTypeDecoratedResultCollection;
import org.mule.runtime.core.internal.util.message.TransformingLegacyResultAdapterCollection;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.context.FlowProcessMediatorContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.sdk.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Routes a message through a Flow and coordinates error handling and response emitting.
 *
 * @since 4.3.0
 */
public class FlowProcessMediator implements Initialisable {

  private static final Logger LOGGER = getLogger(FlowProcessMediator.class);

  @Inject
  private InterceptorManager processorInterceptorManager;

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  @Inject
  private ErrorTypeLocator errorTypeLocator;

  @Inject
  private Collection<ExceptionContextProvider> exceptionContextProviders;

  @Inject
  private ServerNotificationManager notificationManager;

  @Inject
  private MuleContext muleContext;

  private final PolicyManager policyManager;
  private final PhaseResultNotifier phaseResultNotifier;
  private final List<CompletableInterceptorSourceSuccessCallbackAdapter> additionalSuccessInterceptors = new LinkedList<>();
  private final List<CompletableInterceptorSourceFailureCallbackAdapter> additionalFailureInterceptors = new LinkedList<>();
  private ErrorType sourceResponseGenerateErrorType;
  private ErrorType sourceResponseSendErrorType;
  private ErrorType sourceErrorResponseGenerateErrorType;
  private ErrorType sourceErrorResponseSendErrorType;
  private ErrorType flowBackPressureErrorType;
  private NotificationHelper notificationHelper;
  private final List<SourceInterceptor> sourceInterceptors = new LinkedList<>();
  private Optional<CorrelationIdGenerator> correlationIdGenerator;

  public FlowProcessMediator(PolicyManager policyManager, PhaseResultNotifier phaseResultNotifier) {
    this.policyManager = policyManager;
    this.phaseResultNotifier = phaseResultNotifier;
  }

  @Override
  public void initialise() throws InitialisationException {
    this.notificationHelper =
        new NotificationHelper(notificationManager, ConnectorMessageNotification.class, false);

    sourceResponseGenerateErrorType = errorTypeRepository.getErrorType(SOURCE_RESPONSE_GENERATE)
        .orElseThrow(createInitialisationExceptionFor(SOURCE_RESPONSE_GENERATE));

    sourceResponseSendErrorType = errorTypeRepository.getErrorType(SOURCE_RESPONSE_SEND)
        .orElseThrow(createInitialisationExceptionFor(SOURCE_RESPONSE_SEND));

    sourceErrorResponseGenerateErrorType = errorTypeRepository.getErrorType(SOURCE_ERROR_RESPONSE_GENERATE)
        .orElseThrow(createInitialisationExceptionFor(SOURCE_ERROR_RESPONSE_GENERATE));

    sourceErrorResponseSendErrorType = errorTypeRepository.getErrorType(SOURCE_ERROR_RESPONSE_SEND)
        .orElseThrow(createInitialisationExceptionFor(SOURCE_ERROR_RESPONSE_SEND));

    flowBackPressureErrorType = errorTypeRepository.getErrorType(FLOW_BACK_PRESSURE)
        .orElseThrow(createInitialisationExceptionFor(FLOW_BACK_PRESSURE));

    correlationIdGenerator = muleContext.getConfiguration().getDefaultCorrelationIdGenerator();

    if (processorInterceptorManager != null) {
      processorInterceptorManager.getSourceInterceptorFactories().stream().forEach(interceptorFactory -> {
        CompletableInterceptorSourceSuccessCallbackAdapter reactiveInterceptorSuccessAdapter =
            new CompletableInterceptorSourceSuccessCallbackAdapter(interceptorFactory);
        CompletableInterceptorSourceFailureCallbackAdapter reactiveInterceptorFailureAdapter =
            new CompletableInterceptorSourceFailureCallbackAdapter(interceptorFactory);
        try {
          muleContext.getInjector().inject(reactiveInterceptorSuccessAdapter);
          muleContext.getInjector().inject(reactiveInterceptorFailureAdapter);
        } catch (MuleException e) {
          throw new MuleRuntimeException(e);
        }
        additionalSuccessInterceptors.add(0, reactiveInterceptorSuccessAdapter);
        additionalFailureInterceptors.add(0, reactiveInterceptorFailureAdapter);
        sourceInterceptors.add(0, interceptorFactory.get());
      });
    }
  }

  private Supplier<InitialisationException> createInitialisationExceptionFor(ComponentIdentifier sourceResponseGenerate) {
    return () -> new InitialisationException(createStaticMessage("ErrorType %s not found in repository",
                                                                 sourceResponseGenerate),
                                             this);
  }

  public void process(FlowProcessTemplate template,
                      MessageProcessContext messageProcessContext) {
    try {

      final MessageSource messageSource = messageProcessContext.getMessageSource();
      final Pipeline flowConstruct = (Pipeline) messageProcessContext.getFlowConstruct();
      final CompletableFuture<Void> responseCompletion = new CompletableFuture<>();
      final FlowProcessor flowExecutionProcessor =
          new FlowProcessor(publisher -> applyWithChildContext(from(publisher), template::routeEventAsync, empty()),
                            flowConstruct);

      final CoreEvent event = createEvent(template, messageSource,
                                          responseCompletion, flowConstruct);

      policyManager.addSourcePointcutParametersIntoEvent(messageSource, event.getMessage().getAttributes(),
                                                         (InternalEvent) event);

      try {
        final SourcePolicy policy =
            policyManager.createSourcePolicyInstance(messageSource, event, flowExecutionProcessor, template);

        final DefaultFlowProcessMediatorContext phaseContext = new DefaultFlowProcessMediatorContext(template,
                                                                                                     getTerminateConsumer(messageSource,
                                                                                                                          template),
                                                                                                     responseCompletion);
        ((InternalEvent) event).setFlowProcessMediatorContext(phaseContext);

        // registering source interceptor callback to the event context
        BaseEventContext rootContext = ((BaseEventContext) event.getContext()).getRootContext();
        sourceInterceptors.forEach(sourceInterceptor -> rootContext
            .onTerminated((e, t) -> sourceInterceptor.afterTerminated(messageSource.getLocation(), rootContext)));

        dispatch(event, policy, flowConstruct, phaseContext);
      } catch (Exception e) {
        template.sendFailureResponseToClient(messageProcessContext.getMessagingExceptionResolver()
            .resolve(new MessagingException(event, e), errorTypeLocator, exceptionContextProviders),
                                             template.getFailedExecutionResponseParametersFunction().apply(event),
                                             always(() -> phaseResultNotifier.phaseFailure(e)));

        ((BaseEventContext) event.getContext()).error(e);
        responseCompletion.complete(null);
      }
    } catch (Exception e) {
      phaseResultNotifier.phaseFailure(e);
    }
  }

  private void dispatch(@Nonnull CoreEvent event, SourcePolicy sourcePolicy, Pipeline flowConstruct,
                        DefaultFlowProcessMediatorContext ctx)
      throws Exception {
    try {
      onMessageReceived(event, flowConstruct, ctx);
      flowConstruct.checkBackpressure(event);
      ctx.template.getNotificationFunctions().forEach(notificationFunction -> notificationManager
          .fireNotification(notificationFunction.apply(event, flowConstruct.getSource())));
      sourcePolicy.process(event, ctx.template,
                           new CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>() {

                             @Override
                             public void complete(Either<SourcePolicyFailureResult, SourcePolicySuccessResult> value) {
                               dispatchResponse(flowConstruct, ctx, value);
                             }

                             @Override
                             public void error(Throwable e) {
                               dispatchResponse(flowConstruct, ctx,
                                                left(new SourcePolicyFailureResult(new MessagingException(event, e),
                                                                                   Collections::emptyMap)));
                             }
                           });
    } catch (Exception e) {
      e = (Exception) Exceptions.unwrap(e);
      if (e instanceof FlowBackPressureException) {
        ((BaseEventContext) event.getContext()).error(e);
        dispatchResponse(flowConstruct, ctx,
                         mapBackPressureExceptionToPolicyFailureResult(ctx.template, event, (FlowBackPressureException) e));
      } else {
        throw e;
      }
    }
  }

  private void dispatchResponse(Pipeline flowConstruct, DefaultFlowProcessMediatorContext ctx,
                                Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result) {
    result.apply(policyFailure(flowConstruct, ctx), policySuccess(flowConstruct, ctx));
  }

  private void finish(Pipeline flowConstruct, DefaultFlowProcessMediatorContext ctx, Throwable exception) {
    try {
      if (exception != null) {
        onFailure(flowConstruct, ctx).accept(exception);
      } else {
        phaseResultNotifier.phaseSuccessfully();
      }
    } finally {
      ctx.responseCompletion.complete(null);
    }
  }

  /**
   * Notifies the {@link FlowConstruct} response listening party of the backpressure signal raised when trying to inject the event
   * for processing into the {@link ProcessingStrategy}.
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
                                                                                                                       FlowProcessTemplate template,
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
        new SourcePolicyFailureResult(new MessagingException(errorEvent, exception, exception.getFlow()),
                                      () -> template.getFailedExecutionResponseParametersFunction().apply(errorEvent));

    return left(result);
  }

  /**
   * Process success by attempting to send a response to client handling the case where response sending fails or the resolution
   * of response parameters fails.
   */
  private Consumer<SourcePolicySuccessResult> policySuccess(Pipeline flowConstruct, DefaultFlowProcessMediatorContext ctx) {
    return successResult -> {
      fireNotification(flowConstruct.getSource(), successResult.getResult(),
                       flowConstruct, MESSAGE_RESPONSE);

      CompletableCallback<Void> responseCallback = new CompletableCallback<Void>() {

        @Override
        public void complete(Void value) {
          onTerminate(flowConstruct, ctx, right(successResult.getResult()));
          finish(flowConstruct, ctx, null);
        }

        @Override
        public void error(Throwable e) {
          policySuccessError(flowConstruct,
                             new SourceErrorException(successResult.getResult(), sourceResponseSendErrorType, e));
        }
      };
      try {
        if (additionalSuccessInterceptors.isEmpty()) {
          ctx.template.sendResponseToClient(successResult.getResult(), successResult.getResponseParameters().get(),
                                            responseCallback);
        } else {
          BiConsumer<SourcePolicySuccessResult, CompletableCallback<Void>> sendResponseToClient =
              (result, callback) -> ctx.template.sendResponseToClient(result.getResult(),
                                                                      result.getResponseParameters().get(), callback);
          for (CompletableInterceptorSourceSuccessCallbackAdapter interceptor : additionalSuccessInterceptors) {
            sendResponseToClient = interceptor.apply(flowConstruct.getSource(), sendResponseToClient);
          }

          sendResponseToClient.accept(successResult, responseCallback);
        }
      } catch (Exception e) {
        policySuccessError(flowConstruct,
                           new SourceErrorException(successResult.getResult(), sourceResponseGenerateErrorType, e));
      }
    };
  }

  /**
   * Process failure success by attempting to send an error response to client handling the case where error response sending
   * fails or the resolution of error response parameters fails.
   */
  private Consumer<SourcePolicyFailureResult> policyFailure(Pipeline flowConstruct, DefaultFlowProcessMediatorContext ctx) {
    return failureResult -> {
      fireNotification(flowConstruct.getSource(), failureResult.getMessagingException().getEvent(),
                       flowConstruct, MESSAGE_ERROR_RESPONSE);

      CompletableCallback<Void> responseCallback = new CompletableCallback<Void>() {

        @Override
        public void complete(Void value) {
          onTerminate(flowConstruct, ctx, left(failureResult.getMessagingException()));
          finish(flowConstruct, ctx, null);
        }

        @Override
        public void error(Throwable e) {
          finish(flowConstruct, ctx, e);
        }
      };
      try {
        if (additionalFailureInterceptors.isEmpty()) {
          sendErrorResponse(failureResult.getMessagingException(), event -> failureResult.getErrorResponseParameters().get(), ctx,
                            responseCallback);
        } else {
          BiConsumer<SourcePolicyFailureResult, CompletableCallback<Void>> sendErrorResponse =
              (result, callback) -> sendErrorResponse(result.getMessagingException(),
                                                      event -> result.getErrorResponseParameters().get(), ctx, callback);
          for (CompletableInterceptorSourceFailureCallbackAdapter interceptor : additionalFailureInterceptors) {
            sendErrorResponse = interceptor.apply(flowConstruct.getSource(), sendErrorResponse);
          }
          sendErrorResponse.accept(failureResult, responseCallback);
        }
      } catch (Exception e) {
        responseCallback.error(new SourceErrorException(failureResult.getResult(), sourceErrorResponseGenerateErrorType, e,
                                                        failureResult.getMessagingException()));
      }
    };
  }

  /**
   * Handle errors caused when attempting to process a success response by invoking flow error handler and disregarding the result
   * and sending an error response.
   */
  private void policySuccessError(Pipeline flowConstruct, SourceErrorException see) {
    MessagingException messagingException =
        see.toMessagingException(exceptionContextProviders, flowConstruct.getSource());

    if (flowConstruct instanceof AbstractPipeline) {
      ((AbstractPipeline) flowConstruct).errorRouterForSourceResponseError(flow -> me -> {
        final InternalEvent event = (InternalEvent) ((MessagingException) me).getEvent();
        final DefaultFlowProcessMediatorContext ctx =
            (DefaultFlowProcessMediatorContext) event.<DefaultFlowProcessMediatorContext>getFlowProcessMediatorContext();
        sendErrorResponse((MessagingException) me,
                          from(event)
                              .getResponseParametersProcessor()
                              .getFailedExecutionResponseParametersFunction(),
                          ctx,
                          new CompletableCallback<Void>() {

                            @Override
                            public void complete(Void value) {
                              onTerminate(flow, ctx, left(me));
                              finish(flow, ctx, null);
                            }

                            @Override
                            public void error(Throwable e) {
                              finish(flow, ctx, e);
                            }
                          });
      }).accept(messagingException);
    }
  }

  /**
   * Send an error response. This may be due to an error being propagated from the Flow or due to a failure sending a success
   * response. Error caused by failures in the flow error handler do not result in an error message being sent.
   */
  private void sendErrorResponse(MessagingException messagingException,
                                 Function<CoreEvent, Map<String, Object>> errorParameters,
                                 final DefaultFlowProcessMediatorContext ctx,
                                 CompletableCallback<Void> callback) {

    CoreEvent event = messagingException.getEvent();

    try {
      if (!containsType(messagingException, SourceRemoteConnectionException.class)) {
        ctx.template.sendFailureResponseToClient(messagingException, errorParameters.apply(event),
                                                 new CompletableCallback<Void>() {

                                                   @Override
                                                   public void complete(Void value) {
                                                     callback.complete(value);
                                                   }

                                                   @Override
                                                   public void error(Throwable e) {
                                                     callback.error(new SourceErrorException(builder(event)
                                                         .error(builder(e).errorType(sourceErrorResponseSendErrorType).build())
                                                         .build(),
                                                                                             sourceErrorResponseSendErrorType,
                                                                                             e));
                                                   }
                                                 });
      } else {
        callback.complete(null);
      }
    } catch (Exception e) {
      callback.error(new SourceErrorException(event, sourceErrorResponseGenerateErrorType, e, messagingException));
    }
  }

  /*
   * Consumer invoked when processing fails due to an error sending an error response, of because the error originated from within
   * an error handler.
   */
  private Consumer<Throwable> onFailure(Pipeline flowConstruct, DefaultFlowProcessMediatorContext ctx) {
    return throwable -> {
      onTerminate(flowConstruct, ctx, left(throwable));
      throwable = throwable instanceof SourceErrorException ? throwable.getCause() : throwable;
      Exception failureException = throwable instanceof Exception ? (Exception) throwable : new DefaultMuleException(throwable);
      phaseResultNotifier.phaseFailure(failureException);
    };
  }

  private Consumer<Either<MessagingException, CoreEvent>> getTerminateConsumer(MessageSource messageSource,
                                                                               FlowProcessTemplate template) {
    return eventOrException -> template.afterPhaseExecution(eventOrException.mapLeft(messagingException -> {
      messagingException.setProcessedEvent(createErrorEvent(messagingException.getEvent(), messageSource, messagingException,
                                                            errorTypeLocator));
      return messagingException;
    }));
  }

  /*
   * Consumer invoked for each new execution of this processing phase.
   */
  private void onMessageReceived(CoreEvent event, Pipeline flowConstruct, DefaultFlowProcessMediatorContext ctx) {
    fireNotification(flowConstruct.getSource(), event, flowConstruct, MESSAGE_RECEIVED);
    ctx.template.getNotificationFunctions().forEach(notificationFunction -> notificationManager
        .fireNotification(notificationFunction.apply(event, flowConstruct.getSource())));
  }

  private String evaluateCorrelationIdExpressionGenerator() {
    if (correlationIdGenerator.isPresent()) {
      return correlationIdGenerator.get().generateCorrelationId();
    } else {
      return null;
    }
  }

  private String resolveSourceCorrelationId(SourceResultAdapter adapter) {
    // If there is not a correlation ID coming from the Source, use the one generated by the default generator,
    // if there's any.
    return adapter.getCorrelationId().orElseGet(() -> evaluateCorrelationIdExpressionGenerator());
  }

  private CoreEvent createEvent(FlowProcessTemplate template, MessageSource source,
                                CompletableFuture<Void> responseCompletion, FlowConstruct flowConstruct) {

    SourceResultAdapter adapter = template.getSourceMessage();
    Builder eventBuilder =
        createEventBuilder(source.getLocation(), responseCompletion, flowConstruct, resolveSourceCorrelationId(adapter));

    return eventBuilder.message(eventCtx -> {
      final Result<?, ?> result = adapter.getResult();
      final Object resultValue = result.getOutput();

      Message eventMessage;
      if (resultValue instanceof Collection && adapter.isCollection()) {
        Collection<Result> resultCollection = new TransformingLegacyResultAdapterCollection((Collection) resultValue);
        eventMessage = toMessage(Result.<Collection<Message>, TypedValue<?>>builder()
            .output(messageCollection(new MediaTypeDecoratedResultCollection(resultCollection,
                                                                             adapter.getPayloadMediaTypeResolver()),
                                      adapter.getCursorProviderFactory(),
                                      ((BaseEventContext) eventCtx).getRootContext(),
                                      source.getLocation()))
            .mediaType(result.getMediaType().orElse(ANY))
            .build());
      } else {
        eventMessage = toMessage(result, adapter.getMediaType(), adapter.getCursorProviderFactory(),
                                 ((BaseEventContext) eventCtx).getRootContext(), source.getLocation());
      }

      return eventMessage;
    }).build();
  }

  private Builder createEventBuilder(ComponentLocation sourceLocation, CompletableFuture<Void> responseCompletion,
                                     FlowConstruct flowConstruct, String correlationId) {
    return InternalEvent.builder(create(flowConstruct, sourceLocation, correlationId, Optional.of(responseCompletion)));
  }

  /**
   * This method will not throw any {@link Exception}.
   *
   * @param ctx    the {@link DefaultFlowProcessMediatorContext}
   * @param result the outcome of trying to send the response of the source through the source. In the case of error, only
   *               {@link MessagingException} or {@link SourceErrorException} are valid values on the {@code left} side of this
   *               parameter.
   */
  private void onTerminate(Pipeline flowConstruct, DefaultFlowProcessMediatorContext ctx, Either<Throwable, CoreEvent> result) {
    safely(result.mapLeft(throwable -> {
      if (throwable instanceof MessagingException) {
        return (MessagingException) throwable;
      } else if (throwable instanceof SourceErrorException) {
        return ((SourceErrorException) throwable).toMessagingException(exceptionContextProviders, flowConstruct.getSource());
      } else {
        return null;
      }
    }), mapped -> ctx.terminateConsumer.accept(mapped), e -> {
    });
  }

  private void fireNotification(Component source, CoreEvent event, FlowConstruct flow, int action) {
    try {
      if (event == null) {
        // Null result only happens when there's a filter in the chain.
        // Unfortunately a filter causes the whole chain to return null
        // and there's no other way to retrieve the last event but using the RequestContext.
        // see https://www.mulesoft.org/jira/browse/MULE-8670
        event = getCurrentEvent();
        if (event == null) {
          return;
        }
      }
      notificationHelper.fireNotification(source, event, flow.getLocation(), action);
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(format("Could not fire notification. Action: %s", action), e);
      }
    }
  }

  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  /**
   * Container for passing relevant context between private methods to avoid long method signatures everywhere.
   */
  public static final class DefaultFlowProcessMediatorContext implements FlowProcessMediatorContext {

    private final FlowProcessTemplate template;
    private final Consumer<Either<MessagingException, CoreEvent>> terminateConsumer;
    private final CompletableFuture<Void> responseCompletion;

    private DefaultFlowProcessMediatorContext(FlowProcessTemplate template,
                                              Consumer<Either<MessagingException, CoreEvent>> terminateConsumer,
                                              CompletableFuture<Void> responseCompletion) {
      this.template = template;
      this.terminateConsumer = terminateConsumer;
      this.responseCompletion = responseCompletion;
    }

    @Override
    public DefaultFlowProcessMediatorContext copy() {
      return this;
    }
  }
}
