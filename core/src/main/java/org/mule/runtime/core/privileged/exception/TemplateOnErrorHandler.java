/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.Boolean.TRUE;
import static java.lang.Runtime.getRuntime;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_END;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_START;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.exception.WildcardErrorTypeMatcher.WILDCARD_TOKEN;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.updateRootContainerName;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.DisjunctiveErrorTypeMatcher;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.exception.WildcardErrorTypeMatcher;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.rx.RoundRobinFluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.TransactionAwareFluxSinkSupplier;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.transaction.TransactionAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

@NoExtend
public abstract class TemplateOnErrorHandler extends AbstractExceptionListener
    implements MessagingExceptionHandlerAcceptor {


  private static final Pattern ERROR_HANDLER_LOCATION_PATTERN = compile(".*/.*/.*");
  private static final String ERROR_EXCEPTION = "error.exception.";
  private static final String ERROR_SINK = "error.sink.";
  private static final String ERROR_SUCCESS_CALLBACK = "error.success.";
  private static final String ERROR_RETHROWN_CALLBACK = "error.error.";
  private static final String ERROR_EVENT = "error.event.";

  @Inject
  protected ConfigurationComponentLocator locator;

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
  private ComponentLocation location;

  private Supplier<FluxSink<CoreEvent>> routingSink;

  private final class OnErrorHandlerFluxObjectFactory implements Supplier<FluxSink<CoreEvent>> {

    @Override
    public FluxSink<CoreEvent> get() {
      final FluxSinkRecorder<CoreEvent> sinkRef = new FluxSinkRecorder<>();
      Flux<CoreEvent> onErrorFlux = Flux.create(sinkRef).map(beforeRouting());

      if (!getMessageProcessors().isEmpty()) {
        onErrorFlux = onErrorFlux.compose(TemplateOnErrorHandler.this::route);
      }

      onErrorFlux = onErrorFlux
          .onErrorContinue(MessagingException.class, onRoutingError())
          .map(afterRouting())
          .doOnNext(result -> fireEndNotification(getOriginalEvent(result), result, getException(result)))
          .doOnNext(TemplateOnErrorHandler.this::resolveHandling);

      onErrorFlux.subscribe();
      return sinkRef.getFluxSink();
    }

  }

  @Override
  final public CoreEvent handleException(Exception exception, CoreEvent event) {
    try {
      return Mono.<CoreEvent>from(sink -> routeError(exception, handledEvent -> sink.onNext(handledEvent),
                                                     rethrownError -> sink.onError(rethrownError)))
          .block();
    } catch (Throwable throwable) {
      throw new RuntimeException(unwrap(throwable));
    }
  }

  @Override
  public void routeError(Exception error, Consumer<CoreEvent> continueCallback,
                         Consumer<Throwable> propagateCallback) {
    MessagingException messagingError = (MessagingException) error;
    CoreEvent failureEvent = messagingError.getEvent();
    routingSink.get().next(quickCopy(failureEvent, of(getParameterId(ERROR_EXCEPTION, failureEvent), error,
                                                      getParameterId(ERROR_SUCCESS_CALLBACK, failureEvent), continueCallback,
                                                      getParameterId(ERROR_RETHROWN_CALLBACK, failureEvent), propagateCallback,
                                                      getParameterId(ERROR_EVENT, failureEvent), failureEvent)));
  }

  @Override
  public Publisher<CoreEvent> apply(final Exception exception) {
    return applyInternal(exception, ((MessagingException) exception).getEvent());
  }

  private Publisher<CoreEvent> applyInternal(final Exception exception, CoreEvent event) {
    return Mono.create(sink -> routingSink.get().next(quickCopy(event, of(getParameterId(ERROR_EXCEPTION, event), exception,
                                                                          getParameterId(ERROR_SINK, event), sink,
                                                                          getParameterId(ERROR_EVENT, event), event))));
  }

  private void resolveHandling(CoreEvent result) {
    Exception exception = getException(result);
    Consumer<CoreEvent> successfullyHandledConsumer = getInternalParameter(ERROR_SUCCESS_CALLBACK, result);
    Consumer<Throwable> errorConsumer = getInternalParameter(ERROR_RETHROWN_CALLBACK, result);
    if (exception instanceof MessagingException) {
      final MessagingException messagingEx = (MessagingException) exception;
      if (messagingEx.handled()) {
        successfullyHandledConsumer.accept(result);
      } else {
        if (messagingEx.getEvent() != result) {
          messagingEx.setProcessedEvent(result);
        }
        errorConsumer.accept(exception);
      }
    } else {
      errorConsumer.accept(exception);
    }
  }

  protected Exception getException(CoreEvent event) {
    return getInternalParameter(ERROR_EXCEPTION, event);
  }

  protected CoreEvent getOriginalEvent(CoreEvent event) {
    return getInternalParameter(ERROR_EVENT, event);
  }

  private String getParameterId(String errorException, CoreEvent event) {
    return errorException + event.getContext().getId();
  }

  private <T> T getInternalParameter(String keyPrefix, CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(getParameterId(keyPrefix, event));
  }

  private BiConsumer<Throwable, Object> onRoutingError() {
    return (me, event) -> {
      try {
        logger.error("Exception during exception strategy execution");
        resolveAndLogException(me);
        if (isOwnedTransaction()) {
          TransactionCoordination.getInstance().rollbackCurrentTransaction();
        }
      } catch (Exception ex) {
        // Do nothing
        logger.warn(ex.getMessage());
      }
      CoreEvent result = afterRouting().apply(((MessagingException) me).getEvent());
      fireEndNotification(getOriginalEvent(result), result, me);
      Consumer<Throwable> sink = getInternalParameter(ERROR_RETHROWN_CALLBACK, result);
      sink.accept(me);
    };
  }

  private void fireEndNotification(CoreEvent event, CoreEvent result, Throwable throwable) {
    notificationFirer.dispatch(new ErrorHandlerNotification(createInfo(result != null ? result
        : event, throwable instanceof MessagingException ? (MessagingException) throwable : null,
                                                                       configuredMessageProcessors),
                                                            location, PROCESS_END));
  }

  protected Publisher<CoreEvent> route(Publisher<CoreEvent> eventPublisher) {
    return applyWithChildContext(eventPublisher, configuredMessageProcessors, ofNullable(location),
                                 NullExceptionHandler.getInstance());
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

  private void processStatistics() {
    if (statistics != null && statistics.isEnabled()) {
      statistics.incExecutionError();
    }
  }

  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    super.doInitialise(muleContext);
    this.location = this.getLocation();
    Optional<ProcessingStrategy> processingStrategy = empty();
    if (flowLocation.isPresent()) {
      processingStrategy = getProcessingStrategy(locator, flowLocation.get());
    } else if (location != null) {
      processingStrategy = getProcessingStrategy(locator, getRootContainerLocation());
    }
    configuredMessageProcessors = newChain(processingStrategy, getMessageProcessors());

    if (configuredMessageProcessors != null) {
      configuredMessageProcessors.setMuleContext(muleContext);
    }

    errorTypeMatcher = createErrorType(muleContext.getErrorTypeRepository(), errorType, configurationProperties);
    if (!inDefaultErrorHandler()) {
      errorHandlerLocation = this.location.getLocation();
      isLocalErrorHandlerLocation = ERROR_HANDLER_LOCATION_PATTERN.matcher(errorHandlerLocation).find();
      if (isLocalErrorHandlerLocation) {
        errorHandlerLocation = errorHandlerLocation.substring(0, errorHandlerLocation.lastIndexOf('/'));
        errorHandlerLocation = errorHandlerLocation.substring(0, errorHandlerLocation.lastIndexOf('/'));
      }
    }
  }

  @Override
  public void start() throws MuleException {
    super.start();
    OnErrorHandlerFluxObjectFactory factory = new OnErrorHandlerFluxObjectFactory();
    routingSink =
        new TransactionAwareFluxSinkSupplier<>(factory,
                                               new RoundRobinFluxSinkSupplier<>(getRuntime().availableProcessors(), factory));
  }

  @Override
  public void dispose() {
    super.dispose();
    disposeIfNeeded(routingSink, logger);
  }

  public static ErrorTypeMatcher createErrorType(ErrorTypeRepository errorTypeRepository, String errorTypeNames,
                                                 ConfigurationProperties configurationProperties) {
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
              // When lazy init deployment is used an error-mapping may not be initialized due to the component that declares it
              // could not be part of the minimal application model. So, whenever we found that scenario we have to create the
              // errorType if not present in the repository already.
              if (configurationProperties.resolveBooleanProperty(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY).orElse(false)) {
                return errorTypeRepository.addErrorType(errorTypeComponentIdentifier, errorTypeRepository.getAnyErrorType());
              }
              throw new MuleRuntimeException(createStaticMessage("Could not find ErrorType for the given identifier: '%s'",
                                                                 parsedIdentifier));
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

  /**
   * @deprecated Use {@link #createErrorType(ErrorTypeRepository, String, ConfigurationProperties)} which handles correctly lazy
   *             mule artifact contexts.
   */
  @Deprecated
  public static ErrorTypeMatcher createErrorType(ErrorTypeRepository errorTypeRepository, String errorTypeNames) {
    return createErrorType(errorTypeRepository, errorTypeNames, new ConfigurationProperties() {

      @Override
      public <T> Optional<T> resolveProperty(String propertyKey) {
        return Optional.empty();
      }

      @Override
      public Optional<Boolean> resolveBooleanProperty(String property) {
        return Optional.empty();
      }

      @Override
      public Optional<String> resolveStringProperty(String property) {
        return Optional.empty();
      }
    });
  }

  public void setWhen(String when) {
    this.when = ofNullable(when);
  }

  @Override
  public boolean accept(CoreEvent event) {
    return acceptsAll() || (acceptsErrorType(event) && acceptsExpression(event));
  }


  private boolean acceptsErrorType(CoreEvent event) {
    return errorTypeMatcher == null || errorTypeMatcher.match(event.getError().get().getErrorType());
  }

  private boolean acceptsExpression(CoreEvent event) {
    return when.map(expr -> muleContext.getExpressionManager().evaluateBoolean(expr, event, getLocation())).orElse(true);
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
      notificationFirer.dispatch(new ErrorHandlerNotification(createInfo(event, exception, configuredMessageProcessors),
                                                              location, PROCESS_START));
      fireNotification(exception, event);
      logException(exception, event);
      processStatistics();
      markExceptionAsHandledIfRequired(exception);
      return event;
    };
  }

  /**
   * Used to log the error passed into this Exception Listener
   *
   * @param t the exception thrown
   */
  protected void logException(Throwable t, CoreEvent event) {
    if (TRUE.toString().equals(logException)
        || this.muleContext.getExpressionManager().evaluateBoolean(logException, event, location, true, true)) {
      resolveAndLogException(t);
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

  protected void setFlowLocation(Location location) {
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


  private boolean isTransactionInGlobalErrorHandler(TransactionAdapter transaction) {
    String transactionContainerName = transaction.getComponentLocation().get().getRootContainerName();
    return flowLocation.isPresent() && transactionContainerName.equals(flowLocation.get().getGlobalName());
  }

  protected boolean isOwnedTransaction() {
    TransactionAdapter transaction = (TransactionAdapter) TransactionCoordination.getInstance().getTransaction();
    if (transaction == null || !transaction.getComponentLocation().isPresent()) {
      return false;
    }

    if (inDefaultErrorHandler()) {
      return defaultErrorHandlerOwnsTransaction(transaction);
    } else if (isTransactionInGlobalErrorHandler((transaction))) {
      // We are in a GlobalErrorHandler that is defined for the container (Flow or TryScope) that created the tx
      return true;
    } else if (flowLocation.isPresent()) {
      // We are in a Global Error Handler, which is not the one that created the Tx
      return false;
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
    return location == null;
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
}
