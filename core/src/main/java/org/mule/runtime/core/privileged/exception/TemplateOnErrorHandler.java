/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import static java.util.Optional.of;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_END;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_START;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LAX_ERROR_TYPES;
import static org.mule.runtime.core.api.exception.WildcardErrorTypeMatcher.WILDCARD_TOKEN;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.updateRootContainerName;
import static org.mule.runtime.core.internal.exception.ErrorHandlerContextManager.addContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.stream;
import static java.util.Collections.newSetFromMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;

import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.ast.internal.error.ErrorTypeBuilder;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.DisjunctiveErrorTypeMatcher;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.exception.WildcardErrorTypeMatcher;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.ErrorHandlerContextManager;
import org.mule.runtime.core.internal.exception.ErrorHandlerContextManager.ErrorHandlerContext;
import org.mule.runtime.core.internal.exception.ExceptionRouter;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.privileged.message.PrivilegedError;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.transaction.TransactionAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

  private static final Pattern ERROR_HANDLER_LOCATION_PATTERN = compile(".*/.*/.*");

  private boolean fromGlobalErrorHandler = false;

  @Inject
  private ExpressionManager expressionManager;

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  @Inject
  private ConfigurationProperties configurationProperties;

  protected Optional<Location> flowLocation = empty();
  private MessageProcessorChain configuredMessageProcessors;

  protected Optional<String> when = empty();
  protected boolean handleException;

  protected String errorType = null;
  protected ErrorTypeMatcher errorTypeMatcher = null;

  private String errorHandlerLocation;
  private boolean isLocalErrorHandlerLocation;

  private Function<Function<Publisher<CoreEvent>, Publisher<CoreEvent>>, FluxSink<CoreEvent>> fluxFactory;

  private final CopyOnWriteArrayList<String> suppressedErrorTypeMatches = new CopyOnWriteArrayList<>();

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

      if (!getMessageProcessors().isEmpty()) {
        onErrorFlux = onErrorFlux.compose(TemplateOnErrorHandler.this::route);
      }

      onErrorFlux = Flux.from(publisherPostProcessor
          .apply(onErrorFlux
              .onErrorContinue(MessagingException.class, onRoutingError())
              .map(afterRouting())
              .doOnNext(result -> {
                ErrorHandlerContext errorHandlerContext = ErrorHandlerContextManager.from(TemplateOnErrorHandler.this, result);
                fireEndNotification(errorHandlerContext.getOriginalEvent(), result, errorHandlerContext.getException());
              })
              .doOnNext(result -> ErrorHandlerContextManager.resolveHandling(TemplateOnErrorHandler.this, result))))
          .doAfterTerminate(() -> fluxSinks.remove(sinkRef.getFluxSink()));

      if (processingStrategy.isPresent()) {
        processingStrategy.get().registerInternalSink(onErrorFlux, "error handler '" + getLocation().getLocation() + "'");
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
  final public CoreEvent handleException(Exception exception, CoreEvent event) {
    try {
      return applyInternal(exception).block();
    } catch (Throwable throwable) {
      throw new RuntimeException(unwrap(throwable));
    }
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
      public void accept(Exception error) {
        // All calling methods will end up transforming any error class other than MessagingException into that one
        fluxSink.next(addContext(TemplateOnErrorHandler.this, (MessagingException) error, continueCallback, propagateCallback));
      }
    };
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
    return (me, event) -> {
      try {
        logger.error("Exception during exception strategy execution");
        getExceptionListener().resolveAndLogException(me);
        if (isOwnedTransaction(((CoreEvent) event).getContext().getOriginatingLocation().getRootContainerName())) {
          TransactionCoordination.getInstance().rollbackCurrentTransaction();
        }
      } catch (Exception ex) {
        // Do nothing
        logger.warn(ex.getMessage());
      }
      CoreEvent result = afterRouting().apply(((MessagingException) me).getEvent());
      fireEndNotification(ErrorHandlerContextManager.from(this, ((MessagingException) me).getEvent()).getOriginalEvent(), result,
                          me);
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
  protected void doInitialise() throws InitialisationException {
    super.doInitialise();
    Optional<ProcessingStrategy> processingStrategy = empty();
    if (fromGlobalErrorHandler) {
      processingStrategy = getProcessingStrategyFromGlobalErrorHandler(locator);
    } else if (getLocation() != null) {
      processingStrategy = getProcessingStrategy(locator, this);
    }
    configuredMessageProcessors =
        buildNewChainWithListOfProcessors(processingStrategy, getMessageProcessors(), NullExceptionHandler.getInstance());

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

  private Optional<ProcessingStrategy> getProcessingStrategyFromGlobalErrorHandler(ConfigurationComponentLocator locator) {
    return of(new OnRuntimeProcessingStrategy(locator));
  }

  @Override
  public void dispose() {
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

  public static ErrorTypeMatcher createErrorType(ErrorTypeRepository errorTypeRepository, String errorTypeNames) {
    if (errorTypeNames == null) {
      return null;
    }
    String[] errorTypeIdentifiers = errorTypeNames.split(",");
    List<ErrorTypeMatcher> matchers = stream(errorTypeIdentifiers).map((identifier) -> {
      String parsedIdentifier = identifier.trim();
      final ComponentIdentifier errorTypeComponentIdentifier = buildFromStringRepresentation(parsedIdentifier);

      if (doesErrorTypeContainWildcards(errorTypeComponentIdentifier)) {
        return new WildcardErrorTypeMatcher(errorTypeComponentIdentifier);
      } else {
        return new SingleErrorTypeMatcher(errorTypeRepository.lookupErrorType(errorTypeComponentIdentifier)
            .orElseGet(() -> {
              if (getBoolean(MULE_LAX_ERROR_TYPES)) {
                LOGGER.warn("Could not find ErrorType for the given identifier: {}", parsedIdentifier);
                return ErrorTypeBuilder.builder()
                    .namespace(errorTypeComponentIdentifier.getNamespace())
                    .identifier(errorTypeComponentIdentifier.getName())
                    .parentErrorType(errorTypeRepository.getAnyErrorType())
                    .build();
              } else {
                throw new MuleRuntimeException(createStaticMessage("Could not find ErrorType for the given identifier: '%s'",
                                                                   parsedIdentifier));
              }
            }));
      }

    }).collect(toList());
    return new DisjunctiveErrorTypeMatcher(matchers);
  }

  private static boolean doesErrorTypeContainWildcards(ComponentIdentifier errorTypeIdentifier) {
    if (errorTypeIdentifier == null) {
      return false;
    }

    if (Objects.equals(WILDCARD_TOKEN, errorTypeIdentifier.getName())) {
      return true;
    }

    if (Objects.equals(WILDCARD_TOKEN, errorTypeIdentifier.getNamespace())) {
      return true;
    }

    return false;
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
      Exception exception = getException(event);

      getNotificationFirer().dispatch(new ErrorHandlerNotification(createInfo(event, exception, configuredMessageProcessors),

                                                                   getLocation(), PROCESS_START));
      getExceptionListener().fireNotification(exception, event);
      logException(exception, event);
      getExceptionListener().processStatistics();
      markExceptionAsHandledIfRequired(exception);
      return event;
    };
  }

  protected Exception getException(CoreEvent event) {
    return ErrorHandlerContextManager.from(this, event).getException();
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

  public void setFlowLocation(Location location) {
    this.flowLocation = ofNullable(location);
  }

  /**
   * Creates a copy of this ErrorHandler, with the defined location. This location allows to retrieve the
   * {@link ProcessingStrategy}, and define if a running {@link org.mule.runtime.core.api.transaction.Transaction} is owned by the
   * {@link org.mule.runtime.core.api.construct.Flow} or {@link org.mule.runtime.core.internal.processor.TryScope} executing this
   * ErrorHandler. This is intended to be used when having references to Global ErrorHandlers, since each instance reference
   * should run with the processing strategy defined by the flow referencing it, and be able to rollback transactions.
   *
   * @param location
   * @return copy of this ErrorHandler with location to retrieve {@link ProcessingStrategy}
   *
   * @since 4.3.0
   */
  public abstract TemplateOnErrorHandler duplicateFor(Location location);

  protected boolean isOwnedTransaction(String location) {
    TransactionAdapter transaction = (TransactionAdapter) TransactionCoordination.getInstance().getTransaction();
    if (transaction == null || !transaction.getComponentLocation().isPresent()) {
      return false;
    }

    if (inDefaultErrorHandler()) {
      return defaultErrorHandlerOwnsTransaction(transaction);
    }

    if (fromGlobalErrorHandler) {
      return transaction.getComponentLocation().get().getRootContainerName().equals(location);
    } else {
      // We are in a simple scenario where the error handler's location ends with "/error-handler/1".
      // We cannot use the RootContainerLocation, since in case of nested TryScopes (the outer one creating the tx)
      // the RootContainerLocation will be the same for both, and we don't want the inner TryScope's OnErrorPropagate
      // to rollback the tx.
      if (!isLocalErrorHandlerLocation) {
        return sameRootContainerLocation(transaction);
      }
      String transactionLocation = transaction.getComponentLocation().get().getLocation();
      return (sameRootContainerLocation(transaction) && errorHandlerLocation.equals(transactionLocation));
    }
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
      return transactionLocation.equals(flowLocation.get().toString());
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
