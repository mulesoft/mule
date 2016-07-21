/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import org.mule.runtime.core.api.GlobalNameableObject;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.message.ErrorBuilder;
import org.mule.runtime.core.processor.AbstractMuleObjectOwner;

import java.util.Collections;
import java.util.List;

/**
 * Selects which "on error" handler to execute based on filtering. Replaces the choice-exception-strategy from Mule 3.
 * On error handlers must implement {@link org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor} to be part of
 * ErrorHandler.
 *
 * @since 4.0
 */
public class ErrorHandler extends AbstractMuleObjectOwner<MessagingExceptionHandlerAcceptor>
    implements MessagingExceptionHandlerAcceptor, MuleContextAware, Lifecycle, MessageProcessorContainer, GlobalNameableObject {

  private List<MessagingExceptionHandlerAcceptor> exceptionListeners;

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
  public MuleEvent handleException(MessagingException exception, MuleEvent event) {
    event.setMessage(MuleMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception)).build());
    event.setError(ErrorBuilder.builder(exception).build());
    for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners) {
      if (exceptionListener.accept(event)) {
        event.setMessage(MuleMessage.builder(event.getMessage()).exceptionPayload(null).build());
        event.setError(null);
        return exceptionListener.handleException(exception, event);
      }
    }
    throw new MuleRuntimeException(CoreMessages.createStaticMessage("Default exception strategy must accept any event."));
  }

  public void setExceptionListeners(List<MessagingExceptionHandlerAcceptor> exceptionListeners) {
    this.exceptionListeners = exceptionListeners;
  }

  public List<MessagingExceptionHandlerAcceptor> getExceptionListeners() {
    return Collections.unmodifiableList(exceptionListeners);
  }

  @Override
  public void initialise() throws InitialisationException {
    addDefaultExceptionStrategyIfRequired();
    super.initialise();
    validateConfiguredExceptionStrategies();
  }

  private void addDefaultExceptionStrategyIfRequired() throws InitialisationException {
    if (!exceptionListeners.get(exceptionListeners.size() - 1).acceptsAll()) {
      MessagingExceptionHandler defaultExceptionStrategy;
      try {
        defaultExceptionStrategy = getMuleContext().getDefaultExceptionStrategy();
      } catch (Exception e) {
        throw new InitialisationException(CoreMessages.createStaticMessage("Failure initializing "
            + "error-handler. If error-handler is defined as default one "
            + "check that last exception strategy inside matches all errors"), e, this);
      }
      this.exceptionListeners.add(new MessagingExceptionStrategyAcceptorDelegate(defaultExceptionStrategy));
    }
  }

  @Override
  protected List<MessagingExceptionHandlerAcceptor> getOwnedObjects() {
    return Collections.unmodifiableList(exceptionListeners);
  }

  private void validateConfiguredExceptionStrategies() {
    validateOnlyLastAcceptsAll();
    validateOnlyOneHandlesRedelivery();
  }

  private void validateOnlyOneHandlesRedelivery() {
    boolean rollbackWithRedelivery = false;
    for (int i = 0; i < exceptionListeners.size(); i++) {
      MessagingExceptionHandler messagingExceptionHandler = exceptionListeners.get(i);
      if (messagingExceptionHandler instanceof MessagingExceptionStrategyAcceptorDelegate) {
        messagingExceptionHandler =
            ((MessagingExceptionStrategyAcceptorDelegate) messagingExceptionHandler).getExceptionListener();
      }
      if (messagingExceptionHandler instanceof OnErrorPropagateHandler
          && ((OnErrorPropagateHandler) messagingExceptionHandler).hasMaxRedeliveryAttempts()) {
        if (rollbackWithRedelivery) {
          throw new MuleRuntimeException(CoreMessages
              .createStaticMessage("Only one rollback exception strategy inside <error-handler> can handle message redelivery."));
        }
        rollbackWithRedelivery = true;
      }
    }
  }

  private void validateOnlyLastAcceptsAll() {
    for (int i = 0; i < exceptionListeners.size() - 1; i++) {
      MessagingExceptionHandlerAcceptor messagingExceptionHandlerAcceptor = exceptionListeners.get(i);
      if (messagingExceptionHandlerAcceptor.acceptsAll()) {
        throw new MuleRuntimeException(CoreMessages
            .createStaticMessage("Only last exception strategy inside <error-handler> can accept any message. Maybe expression attribute is empty."));
      }
    }
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    int idx = 0;
    for (MessagingExceptionHandlerAcceptor listener : exceptionListeners) {
      if (listener instanceof MessageProcessorContainer) {
        MessageProcessorPathElement exceptionListener = pathElement.addChild(String.valueOf(idx));
        ((MessageProcessorContainer) listener).addMessageProcessorPathElements(exceptionListener);
      }
      idx++;
    }

  }

  @Override
  public boolean accept(MuleEvent event) {
    return true;
  }

  @Override
  public boolean acceptsAll() {
    return true;
  }
}
