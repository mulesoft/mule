/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_END;
import static org.mule.runtime.api.notification.ErrorHandlerNotification.PROCESS_START;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.updateRootContainerName;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.ErrorHandlerNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.DisjunctiveErrorTypeMatcher;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.message.DefaultExceptionPayload;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.exception.AbstractExceptionListener;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.routing.requestreply.ReplyToPropertyRequestReplyReplier;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class TemplateOnErrorHandler extends AbstractExceptionListener
    implements MessagingExceptionHandlerAcceptor {

  private MessageProcessorChain configuredMessageProcessors;
  private Processor replyToMessageProcessor = new ReplyToPropertyRequestReplyReplier();

  protected String when;
  private boolean handleException;

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
        .map(afterRouting(exception))
        .doOnError(MessagingException.class, onRoutingError())
        .<CoreEvent>handle((result, sink) -> {
          if (exception instanceof MessagingException) {
            if (((MessagingException) exception).handled()) {
              sink.next(result);
            } else {
              ((MessagingException) exception).setProcessedEvent(result);
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
        me.setInErrorHandler(true);
        logger.error("Exception during exception strategy execution");
        doLogException(me);
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
      } else {
        event = CoreEvent.builder(event)
            .message(InternalMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception)).build())
            .build();
      }
      return from(processWithChildContext(event, configuredMessageProcessors, ofNullable(getLocation()),
                                          new MessagingExceptionHandlerToSystemAdapter(muleContext)));
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

  protected CoreEvent processReplyTo(CoreEvent event, Exception e) {
    try {
      return replyToMessageProcessor.process(event);
    } catch (MuleException ex) {
      logFatal(event, ex);
      return event;
    }
  }

  protected CoreEvent nullifyExceptionPayloadIfRequired(CoreEvent event) {
    if (this.handleException) {
      return CoreEvent.builder(event).error(null)
          .message(InternalMessage.builder(event.getMessage()).exceptionPayload(null).build())
          .build();
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
    if (getLocation() != null) {
      processingStrategy = getProcessingStrategy(muleContext, getRootContainerName());
    }
    configuredMessageProcessors =
        newChain(processingStrategy,
                 getMessageProcessors());

    if (configuredMessageProcessors != null) {
      configuredMessageProcessors.setMuleContext(muleContext);
    }

    errorTypeMatcher = createErrorType(muleContext.getErrorTypeRepository(), errorType);
  }

  public static ErrorTypeMatcher createErrorType(ErrorTypeRepository errorTypeRepository, String errorTypeNames) {
    if (errorTypeNames == null) {
      return null;
    }
    String[] errorTypeIdentifiers = errorTypeNames.split(",");
    List<ErrorTypeMatcher> matchers = stream(errorTypeIdentifiers).map((identifier) -> {
      String parsedIdentifier = identifier.trim();
      return new SingleErrorTypeMatcher(errorTypeRepository.lookupErrorType(buildFromStringRepresentation(parsedIdentifier))
          .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find ErrorType for the given identifier: '%s'",
                                                                          parsedIdentifier))));
    }).collect(toList());
    return new DisjunctiveErrorTypeMatcher(matchers);
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

  protected Function<CoreEvent, CoreEvent> afterRouting(Exception exception) {
    return event -> {
      if (event != null) {
        event = processReplyTo(event, exception);
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
      // Reset this flag to apply situation where a error-handler exception is handled in a parent error-handler.
      if (exception instanceof MessagingException) {
        ((MessagingException) exception).setInErrorHandler(false);
      }
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
      doLogException(t);
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
}
