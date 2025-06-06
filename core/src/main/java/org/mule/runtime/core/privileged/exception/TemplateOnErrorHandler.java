/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import static org.mule.runtime.api.message.error.matcher.ErrorTypeMatcherUtils.createErrorTypeMatcher;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_END;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_START;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_ROLLBACK;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.transaction.TransactionUtils.profileTransactionAction;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.updateRootContainerName;
import static org.mule.runtime.core.internal.exception.ErrorHandlerContextManager.addContext;
import static org.mule.runtime.core.internal.util.LocationUtils.globalLocation;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getDefaultProcessingStrategyFactory;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.newSetFromMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toSet;

import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.error.matcher.ErrorTypeMatcher;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.ErrorHandlerContextManager;
import org.mule.runtime.core.internal.exception.ErrorHandlerContextManager.ErrorHandlerContext;
import org.mule.runtime.core.internal.exception.ExceptionRouter;
import org.mule.runtime.core.internal.exception.GlobalErrorHandler;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.transaction.TransactionAdapter;
import org.mule.runtime.core.privileged.message.PrivilegedError;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.metrics.api.MeterProvider;
import org.mule.runtime.metrics.api.error.ErrorMetrics;
import org.mule.runtime.metrics.api.error.ErrorMetricsFactory;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

@NoExtend
public abstract class TemplateOnErrorHandler extends AbstractDeclaredExceptionListener
    implements MessagingExceptionHandlerAcceptor {

  private static final Logger LOGGER = getLogger(TemplateOnErrorHandler.class);

  private static final Pattern ERROR_HANDLER_LOCATION_PATTERN = compile("[^/]*/[^/]*/[^/]*");
  public static final String MULE_RUNTIME_ERROR_METRICS = "Mule runtime error metrics";
  private ComponentTracer<CoreEvent> componentTracer;

  private boolean fromGlobalErrorHandler = false;

  @Inject
  private ExpressionManager expressionManager;

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  @Inject
  private ConfigurationProperties configurationProperties;

  @Inject
  private InternalProfilingService profilingService;
  private ProfilingDataProducer<TransactionProfilingEventContext, Object> rollbackProducer;

  @Inject
  private ComponentTracerFactory<CoreEvent> componentTracerFactory;
  protected Optional<String> flowLocation = empty();
  private MessageProcessorChain configuredMessageProcessors;

  protected Optional<String> when = empty();
  protected boolean handleException;

  protected String errorType = null;
  protected ErrorTypeMatcher errorTypeMatcher = null;

  private String errorHandlerLocation;
  private boolean isLocalErrorHandlerLocation;
  private final Set<String> componentsReferencingGlobalErrorHandler = new HashSet<>();
  private final Map<String, FailingComponentHandle> failingComponentHandled = new HashMap<>();

  private Optional<ProcessingStrategy> ownedProcessingStrategy;

  private Function<Function<Publisher<CoreEvent>, Publisher<CoreEvent>>, FluxSink<CoreEvent>> fluxFactory;

  private final CopyOnWriteArrayList<String> suppressedErrorTypeMatches = new CopyOnWriteArrayList<>();

  @Inject
  MeterProvider meterProvider;
  @Inject
  ErrorMetricsFactory errorMetricsFactory;
  private ErrorMetrics errorMetrics;

  public void addGlobalErrorHandlerComponentReference(ComponentLocation location) {
    componentsReferencingGlobalErrorHandler.add(location.getLocation());
  }

  public void addGlobalErrorHandlerComponentReference(String name) {
    componentsReferencingGlobalErrorHandler.add(name);
  }

  private final class OnErrorHandlerFluxObjectFactory
      implements Function<Function<Publisher<CoreEvent>, Publisher<CoreEvent>>, FluxSink<CoreEvent>>, Disposable {

    private final Optional<ProcessingStrategy> processingStrategy;
    private final Set<FluxSink<CoreEvent>> fluxSinks = newSetFromMap(new ConcurrentHashMap<>());

    public OnErrorHandlerFluxObjectFactory(Optional<ProcessingStrategy> processingStrategy) {
      this.processingStrategy = processingStrategy;
    }

    @Override
    public FluxSink<CoreEvent> apply(Function<Publisher<CoreEvent>, Publisher<CoreEvent>> publisherPostProcessor) {
      final FluxSinkRecorder<CoreEvent> sinkRef = new FluxSinkRecorder<>();
      Flux<CoreEvent> onErrorFlux = sinkRef.flux().map(beforeRouting());

      // This optimization omits the execution of the on error handler MessageProcessorChain when it does not have any Processor
      // (empty chain).
      if (!getMessageProcessors().isEmpty()) {
        onErrorFlux = onErrorFlux.transformDeferred(TemplateOnErrorHandler.this::route);
      } else {
        // We need to start the tracing that would be started by the on error handler MessageProcessorChain.
        onErrorFlux = onErrorFlux.doOnNext(coreEvent -> componentTracer.startSpan(coreEvent));
      }

      onErrorFlux = Flux.from(publisherPostProcessor
          .apply(onErrorFlux
              .onErrorContinue(MessagingException.class, onRoutingError())
              .map(afterRouting())
              .doOnNext(result -> {
                ErrorHandlerContext errorHandlerContext = ErrorHandlerContextManager.from(TemplateOnErrorHandler.this, result);
                fireEndNotification(errorHandlerContext.getOriginalEvent(), result, errorHandlerContext.getException());
              })
              .doOnNext(result -> {
                if (getMessageProcessors().isEmpty()) {
                  // We end the current span verifying that the name of the current span is the expected.
                  componentTracer
                      .endCurrentSpan(result);
                }
                ErrorHandlerContextManager.resolveHandling(TemplateOnErrorHandler.this, result);
              })))
          .doAfterTerminate(() -> fluxSinks.remove(sinkRef.getFluxSink()));

      if (processingStrategy.isPresent() && !fromGlobalErrorHandler) {
        String location = getLocation() != null ? getLocation().getLocation() : flowLocation.map(Object::toString).orElse("");
        processingStrategy.get().registerInternalSink(onErrorFlux, "error handler '" + location + "'");
      } else {
        onErrorFlux.subscribe();
      }
      final FluxSink<CoreEvent> fluxSink = sinkRef.getFluxSink();
      fluxSinks.add(fluxSink);
      return fluxSink;
    }

    @Override
    public void dispose() {
      fluxSinks.forEach(FluxSink::complete);
      fluxSinks.clear();
    }
  }

  @Override
  public final CoreEvent handleException(Exception exception, CoreEvent event) {
    try {
      return applyInternal(exception).block();
    } catch (Throwable throwable) {
      throw new RuntimeException(unwrap(throwable));
    }
  }

  @Override
  public synchronized void initialise() throws InitialisationException {
    componentTracer = componentTracerFactory.fromComponent(TemplateOnErrorHandler.this);
    errorMetrics = errorMetricsFactory.create(meterProvider.getMeterBuilder(MULE_RUNTIME_ERROR_METRICS).build());
    this.rollbackProducer = profilingService.getProfilingDataProducer(TX_ROLLBACK);
    super.initialise();
  }

  @Override
  public Consumer<Exception> router(Function<Publisher<CoreEvent>, Publisher<CoreEvent>> publisherPostProcessor,
                                    Consumer<CoreEvent> continueCallback,
                                    Consumer<Throwable> propagateCallback) {
    FluxSink<CoreEvent> fluxSink = fluxFactory.apply(publisherPostProcessor);

    return new ExceptionRouter() {

      @Override
      public void dispose() {
        fluxSink.complete();
      }

      @Override
      // All calling methods will end up transforming any error class other than MessagingException into that one.
      public void accept(Exception error) {
        // Measure only when enter the error handling scope and not when bubbling inside it.
        if (!ErrorHandlerContextManager.isHandling((MessagingException) error))
          measure((MessagingException) error);
        fluxSink.next(addContext(TemplateOnErrorHandler.this, (MessagingException) error, continueCallback, propagateCallback));
      }
    };
  }

  private void measure(MessagingException error) {
    errorMetrics.measure(error);
  }

  @Override
  public Publisher<CoreEvent> apply(final Exception exception) {
    return applyInternal(exception);
  }

  private Mono<CoreEvent> applyInternal(final Exception exception) {
    return Mono.create(sink -> {
      final Consumer<Exception> router = router(identity(),
                                                handledEvent -> sink.success(handledEvent),
                                                rethrownError -> sink.error(rethrownError));
      try {
        router.accept(exception);
      } finally {
        disposeIfNeeded(router, LOGGER);
      }
    });
  }

  private BiConsumer<Throwable, Object> onRoutingError() {
    return (me, obj) -> {
      try {
        logger.error("Exception during exception strategy execution");
        getExceptionListener().resolveAndLogException(me);
        boolean rollback = false;
        if (obj instanceof CoreEvent) {
          rollback = isOwnedTransaction((CoreEvent) obj, getException((CoreEvent) obj));
        } else if (obj instanceof Either) {
          if (((Either) obj).getLeft() instanceof MessagingException) {
            MessagingException exception = (MessagingException) ((Either) obj).getLeft();
            rollback = isOwnedTransaction(exception.getEvent(), exception);
          }
        }
        if (rollback) {
          profileTransactionAction(rollbackProducer, TX_ROLLBACK, getLocation());
          TransactionCoordination.getInstance().rollbackCurrentTransaction();
        }
      } catch (Exception ex) {
        logger.warn(ex.getMessage());
      }
      CoreEvent result = afterRouting().apply(((MessagingException) me).getEvent());
      fireEndNotification(ErrorHandlerContextManager.from(this, ((MessagingException) me).getEvent()).getOriginalEvent(), result,
                          me);
      // We measure the error because it's an error during the handling of an error X(
      measure((MessagingException) me);
      ErrorHandlerContextManager.resolveHandling(this, (MessagingException) me);
    };
  }

  private void fireEndNotification(CoreEvent event, CoreEvent result, Throwable throwable) {
    getNotificationFirer().dispatch(new ErrorHandlerNotification(createInfo(result != null ? result
        : event, throwable instanceof MessagingException ? (MessagingException) throwable : null,
                                                                            configuredMessageProcessors),
                                                                 getLocation(), PROCESS_END));
  }

  protected Publisher<CoreEvent> route(Publisher<CoreEvent> eventPublisher) {
    return from(eventPublisher).transform(configuredMessageProcessors);
  }

  @Override
  public void setMessageProcessors(List<Processor> processors) {
    super.setMessageProcessors(processors);
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return configuredMessageProcessors == null ? new ArrayList<>() : singletonList(configuredMessageProcessors);
  }


  private void markExceptionAsHandledIfRequired(Exception exception) {
    if (handleException) {
      markExceptionAsHandled(exception);
    }
  }

  protected void markExceptionAsHandled(Exception exception) {
    if (exception instanceof MessagingException) {
      ((MessagingException) exception).setHandled(true);
    }
  }

  protected CoreEvent nullifyExceptionPayloadIfRequired(CoreEvent event) {
    if (this.handleException) {
      return CoreEvent.builder(event).error(null).build();
    } else {
      return event;
    }
  }

  @Override
  public void start() throws MuleException {
    if (fromGlobalErrorHandler) {
      if (ownedProcessingStrategy.isPresent()) {
        startIfNeeded(ownedProcessingStrategy);
      }
    }
    super.start();
  }

  @Override
  public void stop() throws MuleException {
    if (fromGlobalErrorHandler) {
      if (ownedProcessingStrategy.isPresent()) {
        stopIfNeeded(ownedProcessingStrategy);
      }
    }
    super.stop();
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    super.doInitialise();
    Optional<ProcessingStrategy> processingStrategy;
    if (fromGlobalErrorHandler) {
      processingStrategy =
          ofNullable(getDefaultProcessingStrategyFactory(muleContext).create(muleContext, getLocation().getRootContainerName()));
      initialiseIfNeeded(processingStrategy);
      ownedProcessingStrategy = processingStrategy;
    } else if (flowLocation.isPresent()) {
      Location location = globalLocation(flowLocation.get());
      processingStrategy = getProcessingStrategy(locator, location);
    } else {
      processingStrategy = getProcessingStrategy(locator, this);
    }
    configuredMessageProcessors =
        buildNewChainWithListOfProcessors(processingStrategy, getMessageProcessors(), NullExceptionHandler.getInstance(),
                                          componentTracer);

    fluxFactory = new OnErrorHandlerFluxObjectFactory(processingStrategy);

    errorTypeMatcher = createErrorType(errorTypeRepository, errorType, configurationProperties);
    if (!inDefaultErrorHandler()) {
      errorHandlerLocation = getLocation().getLocation();
      isLocalErrorHandlerLocation = ERROR_HANDLER_LOCATION_PATTERN.matcher(errorHandlerLocation).find();
      if (isLocalErrorHandlerLocation) {
        errorHandlerLocation = errorHandlerLocation.substring(0, errorHandlerLocation.lastIndexOf('/'));
        errorHandlerLocation = errorHandlerLocation.substring(0, errorHandlerLocation.lastIndexOf('/'));
      }
    }
  }

  @Override
  public void dispose() {
    if (fromGlobalErrorHandler) {
      ownedProcessingStrategy.ifPresent(processingStrategy -> disposeIfNeeded(processingStrategy, LOGGER));
    }
    disposeIfNeeded(fluxFactory, LOGGER);
    super.dispose();
  }

  /**
   * @deprecated Use {@link #createErrorType(ErrorTypeRepository, String)} instead.
   */
  @Deprecated
  public static ErrorTypeMatcher createErrorType(ErrorTypeRepository errorTypeRepository, String errorTypeNames,
                                                 ConfigurationProperties configurationProperties) {
    return createErrorType(errorTypeRepository, errorTypeNames);
  }

  /**
   * @deprecated use
   *             {@link org.mule.runtime.api.message.error.matcher.ErrorTypeMatcherUtils#createErrorTypeMatcher(ErrorTypeRepository, String)}
   *             instead.
   */
  @Deprecated
  public static ErrorTypeMatcher createErrorType(ErrorTypeRepository errorTypeRepository, String errorTypeNames) {
    return createErrorTypeMatcher(errorTypeRepository, errorTypeNames);
  }

  public void setWhen(String when) {
    this.when = ofNullable(when);
  }

  @Override
  public boolean accept(CoreEvent event) {
    return acceptsAll() || (acceptsErrorType(event) && acceptsExpression(event));
  }

  private boolean acceptsErrorType(CoreEvent event) {
    Error error = event.getError().get();
    return errorTypeMatcher == null || errorTypeMatcher.match(error.getErrorType())
        || matchesSuppressedErrorType((PrivilegedError) error);
  }

  /**
   * Evaluates if the {@link #errorTypeMatcher} matches against any of the provided {@link PrivilegedError#getSuppressedErrors()}
   * error types.
   *
   * @param error {@link Error} that will be evaluated.
   * @return True if at least one match is found.
   */
  private boolean matchesSuppressedErrorType(PrivilegedError error) {
    for (Error suppressedError : error.getSuppressedErrors()) {
      ErrorType suppressedErrorType = suppressedError.getErrorType();
      if (errorTypeMatcher.match(suppressedErrorType)) {
        warnAboutSuppressedErrorTypeMatch(error.getErrorType(), suppressedErrorType);
        return true;
      }
    }
    return false;
  }

  /**
   * If it was not previously logged, logs a warning about a suppressed {@link ErrorType} match.
   *
   * @param eventErrorType      Unsuppressed {@link ErrorType} (recommended match).
   * @param suppressedErrorType Suppressed {@link ErrorType} that has been matched.
   */
  private void warnAboutSuppressedErrorTypeMatch(ErrorType eventErrorType, ErrorType suppressedErrorType) {
    // The warning message will be printed only once per matched error type
    if (suppressedErrorTypeMatches.addIfAbsent(suppressedErrorType.getIdentifier())) {
      logger
          .warn("Expected error type from flow '{}' has matched the following underlying error: {}. Consider changing it to match the reported error: {}.",
                getLocation().getLocation(), suppressedErrorType.getIdentifier(), eventErrorType.getIdentifier());
    }
  }

  private boolean acceptsExpression(CoreEvent event) {
    return !hasWhenExpression() || when.map(expr -> expressionManager.evaluateBoolean(expr, event, getLocation())).orElse(true);
  }

  public boolean hasWhenExpression() {
    return when.isPresent();
  }

  protected Function<CoreEvent, CoreEvent> afterRouting() {
    return event -> {
      if (event != null) {
        return nullifyExceptionPayloadIfRequired(event);
      }
      return event;
    };
  }

  protected Function<CoreEvent, CoreEvent> beforeRouting() {
    return event -> {
      MessagingException exception = (MessagingException) getException(event);

      getNotificationFirer().dispatch(new ErrorHandlerNotification(createInfo(event, exception, configuredMessageProcessors),

                                                                   getLocation(), PROCESS_START));
      if (getEnableNotifications()) {
        getExceptionListener().fireNotification(exception, event);
      }
      logException(exception, event);
      getExceptionListener().processStatistics();
      markExceptionAsHandledIfRequired(exception);
      return event;
    };
  }

  protected Exception getException(CoreEvent event) {
    // #getException() will always return a MessagingException but cannot be changed without breaking backwards compatibility
    return ErrorHandlerContextManager.from(this, event).getException();
  }

  protected Error getError(CoreEvent event) {
    return ((MessagingException) getException(event)).getEvent().getError().orElse(null);
  }

  /**
   * Used to log the error passed into this Exception Listener
   *
   * @param t the exception thrown
   */
  protected boolean logException(Throwable t, CoreEvent event) {
    if (TRUE.toString().equals(getLogException())
        || (!FALSE.toString().equals(getLogException())
            && expressionManager.evaluateBoolean(getLogException(), event, getLocation(), true, true))) {
      return getExceptionListener().resolveAndLogException(t);
    } else {
      return false;
    }
  }

  public void setHandleException(boolean handleException) {
    this.handleException = handleException;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  public void setRootContainerName(String rootContainerName) {
    updateRootContainerName(rootContainerName, this);
  }

  public void setFlowLocation(ComponentLocation location) {
    this.flowLocation = ofNullable(location).map(this::normalizeLocation);
  }

  private String normalizeLocation(ComponentLocation loc) {
    String location = loc.getLocation();
    if (location.endsWith("/errorHandler")) {
      return location.substring(0, location.lastIndexOf('/'));
    }
    return location;
  }

  /**
   * Creates a copy of this ErrorHandler, with the defined location. This location allows to retrieve the
   * {@link ProcessingStrategy}, and define if a running {@link Transaction} is owned by the
   * {@link org.mule.runtime.core.api.construct.Flow} or {@link org.mule.runtime.core.internal.processor.TryScope} executing this
   * ErrorHandler. This is intended to be used when having references to Global ErrorHandlers, since each instance reference
   * should run with the processing strategy defined by the flow referencing it, and be able to rollback transactions.
   *
   * @param location
   * @return copy of this ErrorHandler with location to retrieve {@link ProcessingStrategy}
   *
   * @since 4.3.0
   */
  public abstract TemplateOnErrorHandler duplicateFor(ComponentLocation location);

  private static class FailingComponentHandle {

    private final int timesToHandleBeforeReachingTxOwner;
    private int timesHandled;

    FailingComponentHandle(int timesToHandleBeforeReachingTxOwner) {
      this.timesToHandleBeforeReachingTxOwner = timesToHandleBeforeReachingTxOwner;
      timesHandled = 1;
    }

    boolean isHandled() {
      return timesHandled == timesToHandleBeforeReachingTxOwner;
    }

    void handle() {
      timesHandled++;
    }

  }

  protected boolean isOwnedTransaction(CoreEvent event, Exception exception) {
    Transaction transaction = TransactionCoordination.getInstance().getTransaction();
    if (transaction == null || !(transaction instanceof TransactionAdapter)
        || !((TransactionAdapter) transaction).getComponentLocation().isPresent()) {
      return false;
    }

    final TransactionAdapter txAdapter = (TransactionAdapter) transaction;
    if (inDefaultErrorHandler()) {
      // This case is for an implicit error handler, if we are in a configured default error handler then
      // it will be the same as the global error handler case.
      return defaultErrorHandlerOwnsTransaction(txAdapter);
    }

    if (fromGlobalErrorHandler && exception != null) {
      String tempTransactionLocation = txAdapter.getComponentLocation().get().getLocation();
      if (tempTransactionLocation.endsWith("/source")) {
        // Set the flow as the location, as flows are the owners of transactions started by sources
        tempTransactionLocation = tempTransactionLocation.substring(0, tempTransactionLocation.lastIndexOf('/'));
      }
      final String transactionLocation = tempTransactionLocation;

      if (!componentsReferencingGlobalErrorHandler.contains(transactionLocation)) {
        return false;
      }

      String localFailingComponentLocation = ((MessagingException) exception).getFailingComponent().getLocation().getLocation();
      String failingComponentHandledKey = transactionLocation + localFailingComponentLocation;
      FailingComponentHandle failingComponentHandle = null;
      if (failingComponentHandled.get(failingComponentHandledKey) != null) {
        if (getFlowStackElementLocation(event.getFlowCallStack().peek()).endsWith(localFailingComponentLocation)) {
          // There was an on-error-continue in the middle of the global on-error-propagate handlers, process handle logic from
          // scratch
          failingComponentHandled.remove(failingComponentHandledKey);
        } else {
          failingComponentHandle = failingComponentHandled.get(failingComponentHandledKey);
        }
      }

      if (failingComponentHandle != null) {
        failingComponentHandle.handle();
        if (failingComponentHandle.isHandled()) {
          failingComponentHandled.remove(failingComponentHandledKey);
          return true;
        }

        return false;
      } else {
        StringBuilder fullFailingComponentLocationUpToTxOwnerBuilder = new StringBuilder();
        for (FlowStackElement element : event.getFlowCallStack().getElements()) {
          String location = getFlowStackElementLocation(element);
          fullFailingComponentLocationUpToTxOwnerBuilder.insert(0, location + "/");
          if (location.startsWith(transactionLocation)) {
            // Stack element of the flow containing the transaction owner found, processing is finished
            break;
          }
        }
        String fullFailingComponentLocationUpToTxOwner = fullFailingComponentLocationUpToTxOwnerBuilder.toString();

        String transactionOwnerFlow = getFlow(transactionLocation);
        Set<String> referencesContainingFailingComponentUpToTxOwner =
            componentsReferencingGlobalErrorHandler.stream().map(this::normalizeRef)
                // If the flow of the global error handler reference component is not the same as the one of the failing component
                // location, then it's been called as a flow-ref
                .filter(ref -> getFlow(ref).equals(getFlow(fullFailingComponentLocationUpToTxOwner))
                    ? fullFailingComponentLocationUpToTxOwner.contains(ref)
                    : fullFailingComponentLocationUpToTxOwner.contains(normalizeFlowRef(ref)))
                .filter(ref -> !getFlow(ref).equals(transactionOwnerFlow) || ref.startsWith(transactionLocation))
                .collect(toSet());
        if (referencesContainingFailingComponentUpToTxOwner.size() > 1) {
          // The failing component is nested within more than one component that reference this handler:
          // transactionOwnerWithThisHandler-componentWithThisHandler-...-failingComponent
          failingComponentHandled.put(failingComponentHandledKey,
                                      new FailingComponentHandle(referencesContainingFailingComponentUpToTxOwner.size()));

          return false;
        }

        return true;
      }
    }

    return isOwnedTransactionByLocalErrorHandler(txAdapter);
  }

  private String getFlowStackElementLocation(FlowStackElement element) {
    return element.executingLocation().getLocation();
  }

  /**
   * @param location location from which to extract the {@link java.util.concurrent.Flow} location.
   * @return the {@link java.util.concurrent.Flow} part of the given {@code location}.
   */
  private String getFlow(String location) {
    return location.substring(0, location.indexOf('/') + 1);
  }

  /**
   * @param reference component reference to {@link GlobalErrorHandler} to normalize.
   * @return normalized component reference.
   */
  private String normalizeRef(String reference) {
    return reference.endsWith("/") ? reference : reference + "/";
  }

  /**
   * @param reference component reference to {@link GlobalErrorHandler} of a component called with a flow-ref.
   * @return normalized component reference.
   */
  private String normalizeFlowRef(String reference) {
    return "/" + reference;
  }

  private boolean isOwnedTransactionByLocalErrorHandler(TransactionAdapter transaction) {
    // We are in a simple scenario where the error handler's location ends with "/error-handler/1".
    // We cannot use the RootContainerLocation, since in case of nested TryScopes (the outer one creating the tx)
    // the RootContainerLocation will be the same for both, and we don't want the inner TryScope's OnErrorPropagate
    // to rollback the tx.
    if (!isLocalErrorHandlerLocation) {
      return sameRootContainerLocation(transaction);
    }
    String transactionLocation = transaction.getComponentLocation().get().getLocation();
    return errorHandlerLocation.equals(transactionLocation);
  }

  private boolean sameRootContainerLocation(TransactionAdapter transaction) {
    String transactionContainerName = transaction.getComponentLocation().get().getRootContainerName();
    return transactionContainerName.equals(this.getRootContainerLocation().getGlobalName());
  }

  private boolean inDefaultErrorHandler() {
    return getLocation() == null;
  }

  private boolean defaultErrorHandlerOwnsTransaction(TransactionAdapter transaction) {
    String transactionLocation = transaction.getComponentLocation().get().getLocation();
    if (flowLocation.isPresent()) {
      // We are in a default error handler for a TryScope, which must have been replicated to match the tx location
      // to rollback it
      return transactionLocation.equals(flowLocation.get());
    } else {
      // We are in a default error handler of a Flow
      return sameRootContainerLocation(transaction);
    }
  }

  protected ErrorTypeRepository getErrorTypeRepository() {
    return errorTypeRepository;
  }

  public void setFromGlobalErrorHandler(boolean fromGlobalErrorHandler) {
    this.fromGlobalErrorHandler = fromGlobalErrorHandler;
  }

}
