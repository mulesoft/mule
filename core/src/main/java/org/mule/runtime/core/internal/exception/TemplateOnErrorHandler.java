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
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.api.context.notification.ErrorHandlerNotification.PROCESS_END;
import static org.mule.runtime.core.api.context.notification.ErrorHandlerNotification.PROCESS_START;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.api.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.ErrorHandlerNotification;
import org.mule.runtime.core.api.exception.DisjunctiveErrorTypeMatcher;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.ErrorTypeRepository;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.message.DefaultExceptionPayload;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.routing.requestreply.ReplyToPropertyRequestReplyReplier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

public abstract class TemplateOnErrorHandler extends AbstractExceptionListener
    implements MessagingExceptionHandlerAcceptor {

  private MessageProcessorChain configuredMessageProcessors;
  private Processor replyToMessageProcessor = new ReplyToPropertyRequestReplyReplier();

  private String when;
  private boolean handleException;

  protected String errorType = null;
  protected ErrorTypeMatcher errorTypeMatcher = null;

  @Override
  final public Event handleException(MessagingException exception, Event event) {
    try {
      return from(applyInternal(exception, event)).block();
    } catch (Throwable throwable) {
      throw new RuntimeException(unwrap(throwable));
    }
  }

  @Override
  public Publisher<Event> apply(final MessagingException exception) {
    return applyInternal(exception, exception.getEvent());
  }

  private Publisher<Event> applyInternal(final MessagingException exception, Event event) {
    return just(event)
        .map(beforeRouting(exception))
        .flatMapMany(route(exception)).last()
        .map(afterRouting(exception))
        .doOnError(MessagingException.class, onRoutingError())
        .<Event>handle((result, sink) -> {
          if (exception.handled()) {
            sink.next(result);
          } else {
            exception.setProcessedEvent(result);
            sink.error(exception);
          }
        })
        .doOnTerminate((result, throwable) -> fireEndNotification(event, result, throwable));
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

  private void fireEndNotification(Event event, Event result, Throwable throwable) {
    muleContext.getNotificationManager()
        .fireNotification(new ErrorHandlerNotification(createInfo(result != null ? result
            : event, throwable instanceof MessagingException ? (MessagingException) throwable : null,
                                                                  configuredMessageProcessors),
                                                       getLocation(), PROCESS_END));
  }

  protected Function<Event, Publisher<Event>> route(MessagingException exception) {
    return event -> {
      if (getMessageProcessors().isEmpty()) {
        return just(event);
      } else {
        event = Event.builder(event)
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
    configuredMessageProcessors = newChain(getMessageProcessors());
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

  protected Event processReplyTo(Event event, Exception e) {
    try {
      return replyToMessageProcessor.process(event);
    } catch (MuleException ex) {
      logFatal(event, ex);
      return event;
    }
  }

  protected Event nullifyExceptionPayloadIfRequired(Event event) {
    if (this.handleException) {
      return Event.builder(event).error(null).message(InternalMessage.builder(event.getMessage()).exceptionPayload(null).build())
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
  public boolean accept(Event event) {
    return acceptsAll() || acceptsErrorType(event) || (when != null
        && muleContext.getExpressionManager().evaluateBoolean(when, event, getLocation()));
  }


  private boolean acceptsErrorType(Event event) {
    return errorTypeMatcher != null && errorTypeMatcher.match(event.getError().get().getErrorType());
  }

  @Override
  public boolean acceptsAll() {
    return errorTypeMatcher == null && when == null;
  }

  protected Function<Event, Event> afterRouting(MessagingException exception) {
    return event -> {
      if (event != null) {
        event = processReplyTo(event, exception);
        return nullifyExceptionPayloadIfRequired(event);
      }
      return event;
    };
  }

  protected Function<Event, Event> beforeRouting(MessagingException exception) {
    return event -> {
      muleContext.getNotificationManager()
          .fireNotification(new ErrorHandlerNotification(createInfo(event, exception, configuredMessageProcessors),
                                                         getLocation(), PROCESS_START));
      fireNotification(exception, event);
      logException(exception, event);
      processStatistics();
      // Reset this flag to apply situation where a error-handler exception is handled in a parent error-handler.
      exception.setInErrorHandler(false);
      markExceptionAsHandledIfRequired(exception);
      return event;
    };
  }

  /**
   * Used to log the error passed into this Exception Listener
   *
   * @param t the exception thrown
   */
  protected void logException(Throwable t, Event event) {
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
}
