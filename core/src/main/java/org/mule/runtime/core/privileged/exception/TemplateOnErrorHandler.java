/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ERROR_HANDLER;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_END;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_START;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.updateRootContainerName;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.DisjunctiveErrorTypeMatcher;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;

@NoExtend
public abstract class TemplateOnErrorHandler extends AbstractExceptionListener
    implements MessagingExceptionHandlerAcceptor {

  @Inject
  protected ConfigurationComponentLocator locator;

  @Inject
  private ConfigurationProperties configurationProperties;

  protected Optional<Location> flowLocation = empty();
  private MessageProcessorChain configuredMessageProcessors;

  protected String when;
  protected boolean handleException;

  protected String errorType = null;
  protected ErrorTypeMatcher errorTypeMatcher = null;

  @Override
  final public CoreEvent handleException(Exception exception, CoreEvent event) {
    try {
      return from(applyInternal(exception, event)).block();
    } catch (Throwable throwable) {
      throw new RuntimeException(unwrap(throwable));
    }
  }

  @Override
  public Publisher<CoreEvent> apply(final Exception exception) {
    return applyInternal(exception, ((MessagingException) exception).getEvent());
  }

  private Publisher<CoreEvent> applyInternal(final Exception exception, CoreEvent event) {
    return just(event)
        .map(beforeRouting(exception))
        .flatMapMany(route(exception)).last()
        .map(afterRouting())
        .doOnError(MessagingException.class, onRoutingError())
        .<CoreEvent>handle((result, sink) -> {
          if (exception instanceof MessagingException) {
            final MessagingException messagingEx = (MessagingException) exception;
            if (messagingEx.handled()) {
              sink.next(result);
            } else {
              if (messagingEx.getEvent() != result) {
                messagingEx.setProcessedEvent(result);
              }
              sink.error(exception);
            }
          } else {
            sink.error(exception);
          }
        })
        .doOnSuccessOrError((result, throwable) -> fireEndNotification(event, result, throwable));
  }

  private Consumer<MessagingException> onRoutingError() {
    return me -> {
      try {
        logger.error("Exception during exception strategy execution");
        resolveAndLogException(me);
        TransactionCoordination.getInstance().rollbackCurrentTransaction();
      } catch (Exception ex) {
        // Do nothing
        logger.warn(ex.getMessage());
      }
    };
  }

  private void fireEndNotification(CoreEvent event, CoreEvent result, Throwable throwable) {
    notificationFirer.dispatch(new ErrorHandlerNotification(createInfo(result != null ? result
        : event, throwable instanceof MessagingException ? (MessagingException) throwable : null,
                                                                       configuredMessageProcessors),
                                                            getLocation(), PROCESS_END));
  }

  protected Function<CoreEvent, Publisher<CoreEvent>> route(Exception exception) {
    return event -> {
      if (getMessageProcessors().isEmpty()) {
        return just(event);
      }
      return from(processWithChildContext(event, configuredMessageProcessors, ofNullable(getLocation()),
                                          NullExceptionHandler.getInstance()));
    };
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

    Optional<ProcessingStrategy> processingStrategy = empty();
    if (flowLocation.isPresent()) {
      processingStrategy = getProcessingStrategy(locator, flowLocation.get());
    } else if (getLocation() != null) {
      processingStrategy = getProcessingStrategy(locator, getRootContainerLocation());
    }
    configuredMessageProcessors = newChain(processingStrategy, getMessageProcessors());

    if (configuredMessageProcessors != null) {
      configuredMessageProcessors.setMuleContext(muleContext);
    }

    errorTypeMatcher = createErrorType(muleContext.getErrorTypeRepository(), errorType, configurationProperties);
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
    }).collect(toList());
    return new DisjunctiveErrorTypeMatcher(matchers);
  }

  /**
   * @deprecated Use {@link #createErrorType(ErrorTypeRepository, String, ConfigurationProperties)} which handles correctly
   * lazy mule artifact contexts.
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
    this.when = when;
  }

  @Override
  public boolean accept(CoreEvent event) {
    return acceptsAll() || acceptsErrorType(event) || (when != null
        && muleContext.getExpressionManager().evaluateBoolean(when, event, getLocation()));
  }


  private boolean acceptsErrorType(CoreEvent event) {
    return errorTypeMatcher != null && errorTypeMatcher.match(event.getError().get().getErrorType());
  }

  protected Function<CoreEvent, CoreEvent> afterRouting() {
    return event -> {
      if (event != null) {
        return nullifyExceptionPayloadIfRequired(event);
      }
      return event;
    };
  }

  protected Function<CoreEvent, CoreEvent> beforeRouting(Exception exception) {
    return event -> {
      notificationFirer.dispatch(new ErrorHandlerNotification(createInfo(event, exception, configuredMessageProcessors),
                                                              getLocation(), PROCESS_START));
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
        || this.muleContext.getExpressionManager().evaluateBoolean(logException, event, getLocation(), true, true)) {
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
   * {@link ProcessingStrategy}, and define if a running {@link org.mule.runtime.core.api.transaction.Transaction} is
   * owned by the {@link org.mule.runtime.core.api.construct.Flow} or {@link org.mule.runtime.core.internal.processor.TryScope}
   * executing this ErrorHandler.
   * This is intended to be used when having references to Global ErrorHandlers, since each instance reference
   * should run with the processing strategy defined by the flow referencing it, and be able to rollback transactions.
   * @param location
   * @return copy of this ErrorHandler with location to retrieve {@link ProcessingStrategy}
   *
   * @since 4.3.0
   */
  public abstract TemplateOnErrorHandler duplicateFor(Location location);

}
