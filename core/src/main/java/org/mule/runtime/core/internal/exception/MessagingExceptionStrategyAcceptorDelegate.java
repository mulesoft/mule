/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import java.util.Arrays;
import java.util.List;

import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;

import org.reactivestreams.Publisher;

/**
 * Allows to use {@link org.mule.runtime.core.api.exception.MessagingExceptionHandler} as
 * {@link org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor}.
 */
public class MessagingExceptionStrategyAcceptorDelegate extends AbstractMuleObjectOwner<MessagingExceptionHandler>
    implements MessagingExceptionHandlerAcceptor {

  private MessagingExceptionHandler delegate;

  public MessagingExceptionStrategyAcceptorDelegate(MessagingExceptionHandler messagingExceptionHandler) {
    this.delegate = messagingExceptionHandler;
  }

  @Override
  public boolean accept(BaseEvent event) {
    if (delegate instanceof MessagingExceptionHandlerAcceptor) {
      return ((MessagingExceptionHandlerAcceptor) delegate).accept(event);
    }
    return true;
  }

  @Override
  public boolean acceptsAll() {
    if (delegate instanceof MessagingExceptionHandlerAcceptor) {
      return ((MessagingExceptionHandlerAcceptor) delegate).acceptsAll();
    }
    return true;
  }

  @Override
  public BaseEvent handleException(MessagingException exception, BaseEvent event) {
    return delegate.handleException(exception, event);
  }

  @Override
  public Publisher<BaseEvent> apply(MessagingException exception) {
    return delegate.apply(exception);
  }

  @Override
  protected List<MessagingExceptionHandler> getOwnedObjects() {
    return Arrays.asList(delegate);
  }

  public MessagingExceptionHandler getExceptionListener() {
    return this.delegate;
  }
}
