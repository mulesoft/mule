/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.exception.DefaultErrorTypeRepository.CRITICAL_ERROR_TYPE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static reactor.core.publisher.Mono.error;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.GlobalNameableObject;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;
import org.mule.runtime.core.internal.message.DefaultExceptionPayload;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;

/**
 * Selects which "on error" handler to execute based on filtering. Replaces the choice-exception-strategy from Mule 3. On error
 * handlers must implement {@link MessagingExceptionHandlerAcceptor} to be part of ErrorHandler.
 *
 * @since 4.0
 */
public class ErrorHandler extends AbstractMuleObjectOwner<MessagingExceptionHandlerAcceptor>
    implements MessagingExceptionHandlerAcceptor, MuleContextAware, Lifecycle, GlobalNameableObject {

  private static final String MUST_ACCEPT_ANY_EVENT_MESSAGE = "Default exception strategy must accept any event.";
  private List<MessagingExceptionHandlerAcceptor> exceptionListeners;
  private ErrorTypeMatcher criticalMatcher = new SingleErrorTypeMatcher(CRITICAL_ERROR_TYPE);

  protected String globalName;

  @Override
  public String getGlobalName() {
    return globalName;
  }

  @Override
  public void setGlobalName(String globalName) {
    this.globalName = globalName;
  }

  @Override
  public Event handleException(MessagingException exception, Event event) {
    event = addExceptionPayload(exception, event);
    if (isCriticalException(exception)) {
      return event;
    }
    for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners) {
      if (exceptionListener.accept(event)) {
        return exceptionListener.handleException(exception, event);
      }
    }
    throw new MuleRuntimeException(createStaticMessage(MUST_ACCEPT_ANY_EVENT_MESSAGE));
  }

  @Override
  public Publisher<Event> apply(MessagingException exception) {
    if (isCriticalException(exception)) {
      return error(exception);
    } else {
      Event event = addExceptionPayload(exception, exception.getEvent());
      exception.setProcessedEvent(event);
      for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners) {
        if (exceptionListener.accept(event)) {
          return exceptionListener.apply(exception);
        }
      }
      return error(new MuleRuntimeException(createStaticMessage(MUST_ACCEPT_ANY_EVENT_MESSAGE)));
    }
  }

  private Event addExceptionPayload(MessagingException exception, Event event) {
    return Event.builder(event)
        .message(InternalMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception)).build())
        .build();
  }

  private boolean isCriticalException(MessagingException exception) {
    Optional<Error> error = exception.getEvent().getError();
    return error.isPresent() && criticalMatcher.match(error.get().getErrorType());
  }

  public void setExceptionListeners(List<MessagingExceptionHandlerAcceptor> exceptionListeners) {
    this.exceptionListeners = exceptionListeners;
  }

  public List<MessagingExceptionHandlerAcceptor> getExceptionListeners() {
    return unmodifiableList(exceptionListeners);
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    addDefaultExceptionStrategyIfRequired();
    validateConfiguredExceptionStrategies();
  }

  private void addDefaultExceptionStrategyIfRequired() throws InitialisationException {
    if (!exceptionListeners.get(exceptionListeners.size() - 1).acceptsAll()) {
      String defaultErrorHandlerName = getMuleContext().getConfiguration().getDefaultErrorHandlerName();
      if (defaultErrorHandlerName != null && defaultErrorHandlerName.equals(this.getGlobalName())) {
        throw new InitialisationException(
                                          createStaticMessage("Default error-handler must include a final component that matches all errors."),
                                          this);
      }
      MessagingExceptionHandler defaultExceptionStrategy;
      try {
        defaultExceptionStrategy = getMuleContext().getDefaultErrorHandler();
      } catch (Exception e) {
        throw new InitialisationException(createStaticMessage("Failure initializing "
            + "error-handler. If error-handler is defined as default one "
            + "check that last exception strategy inside matches all errors"), e, this);
      }
      MessagingExceptionStrategyAcceptorDelegate acceptsAllStrategy =
          new MessagingExceptionStrategyAcceptorDelegate(defaultExceptionStrategy);
      initialiseIfNeeded(acceptsAllStrategy, muleContext);
      this.exceptionListeners.add(acceptsAllStrategy);
    }
  }

  @Override
  protected List<MessagingExceptionHandlerAcceptor> getOwnedObjects() {
    return exceptionListeners != null ? unmodifiableList(exceptionListeners) : emptyList();
  }

  private void validateConfiguredExceptionStrategies() {
    validateOnlyLastAcceptsAll();
  }

  private void validateOnlyLastAcceptsAll() {
    for (int i = 0; i < exceptionListeners.size() - 1; i++) {
      MessagingExceptionHandlerAcceptor messagingExceptionHandlerAcceptor = exceptionListeners.get(i);
      if (messagingExceptionHandlerAcceptor.acceptsAll()) {
        throw new MuleRuntimeException(
                                       createStaticMessage("Only last exception strategy inside <error-handler> can accept any message. Maybe expression attribute is empty."));
      }
    }
  }

  @Override
  public boolean accept(Event event) {
    return true;
  }

  @Override
  public boolean acceptsAll() {
    return true;
  }

}
