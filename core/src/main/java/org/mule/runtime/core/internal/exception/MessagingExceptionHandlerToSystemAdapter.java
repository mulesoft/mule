/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;

public class MessagingExceptionHandlerToSystemAdapter implements MessagingExceptionHandler {

  private MuleContext muleContext;

  public MessagingExceptionHandlerToSystemAdapter(MuleContext muleContext) {
    requireNonNull(muleContext);
    this.muleContext = muleContext;
  }

  @Override
  public InternalEvent handleException(MessagingException exception, InternalEvent event) {
    muleContext.getExceptionListener().handleException(exception);
    return event;
  }
}
