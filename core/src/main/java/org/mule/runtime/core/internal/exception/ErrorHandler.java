/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.updateRootContainerName;
import static reactor.core.publisher.Mono.error;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.message.DefaultExceptionPayload;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import org.reactivestreams.Publisher;

import java.util.List;

/**
 * Selects which "on error" handler to execute based on filtering. Replaces the choice-exception-strategy from Mule 3. On error
 * handlers must implement {@link MessagingExceptionHandlerAcceptor} to be part of ErrorHandler.
 *
 * @since 4.0
 */
public class ErrorHandler extends AbstractMuleObjectOwner<MessagingExceptionHandlerAcceptor>
    implements MessagingExceptionHandlerAcceptor, MuleContextAware, Lifecycle {

  private static final String MUST_ACCEPT_ANY_EVENT_MESSAGE = "Default exception strategy must accept any event.";
  private List<MessagingExceptionHandlerAcceptor> exceptionListeners;
  private String name;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    addCriticalErrorHandler();
    addDefaultErrorHandlerIfRequired();
    validateConfiguredExceptionStrategies();
  }

  @Override
  public CoreEvent handleException(MessagingException exception, CoreEvent event) {
    event = addExceptionPayload(exception, event);
    for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners) {
      if (exceptionListener.accept(event)) {
        return exceptionListener.handleException(exception, event);
      }
    }
    throw new MuleRuntimeException(createStaticMessage(MUST_ACCEPT_ANY_EVENT_MESSAGE));
  }

  @Override
  public Publisher<CoreEvent> apply(MessagingException exception) {
    CoreEvent event = addExceptionPayload(exception, exception.getEvent());
    exception.setProcessedEvent(event);
    for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners) {
      if (exceptionListener.accept(event)) {
        return exceptionListener.apply(exception);
      }
    }
    return error(new MuleRuntimeException(createStaticMessage(MUST_ACCEPT_ANY_EVENT_MESSAGE)));
  }

  @Override
  protected List<MessagingExceptionHandlerAcceptor> getOwnedObjects() {
    return exceptionListeners != null ? unmodifiableList(exceptionListeners) : emptyList();
  }

  @Override
  public boolean accept(CoreEvent event) {
    return true;
  }

  @Override
  public boolean acceptsAll() {
    return true;
  }

  private CoreEvent addExceptionPayload(MessagingException exception, CoreEvent event) {
    return CoreEvent.builder(event)
        .message(InternalMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception)).build())
        .build();
  }

  private void addCriticalErrorHandler() {
    exceptionListeners.add(0, new OnCriticalErrorHandler());
  }

  private void addDefaultErrorHandlerIfRequired() throws InitialisationException {
    if (!exceptionListeners.get(exceptionListeners.size() - 1).acceptsAll()) {
      String defaultErrorHandlerName = getMuleContext().getConfiguration().getDefaultErrorHandlerName();
      MessagingExceptionHandler defaultErrorHandler;
      if (defaultErrorHandlerName != null && defaultErrorHandlerName.equals(name)) {
        logger
            .warn("Default 'error-handler' should include a final \"catch-all\" 'on-error-propagate'. Attempting implicit injection.");
        try {
          defaultErrorHandler = new ErrorHandlerFactory()
              .createDefault(((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(NotificationDispatcher.class));
        } catch (RegistrationException e) {
          throw new InitialisationException(createStaticMessage("Could not inject \"catch-all\" handler in default 'error-handler'."),
                                            e, this);
        }
      } else {
        try {
          defaultErrorHandler = getMuleContext().getDefaultErrorHandler(of(getRootContainerName()));
        } catch (Exception e) {
          throw new InitialisationException(createStaticMessage("Failure initializing "
              + "error-handler. If error-handler is defined as default one "
              + "check that last exception strategy inside matches all errors"),
                                            e, this);
        }
      }
      MessagingExceptionStrategyAcceptorDelegate acceptsAllStrategy =
          new MessagingExceptionStrategyAcceptorDelegate(defaultErrorHandler);
      initialiseIfNeeded(acceptsAllStrategy, muleContext);
      this.exceptionListeners.add(acceptsAllStrategy);
    }
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

  public void setExceptionListeners(List<MessagingExceptionHandlerAcceptor> exceptionListeners) {
    this.exceptionListeners = exceptionListeners;
  }

  public List<MessagingExceptionHandlerAcceptor> getExceptionListeners() {
    return unmodifiableList(exceptionListeners);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setRootContainerName(String rootContainerName) {
    updateRootContainerName(rootContainerName, this);
    for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners) {
      if (exceptionListener instanceof TemplateOnErrorHandler) {
        ((TemplateOnErrorHandler) exceptionListener).setRootContainerName(rootContainerName);
      }
    }
  }

}
