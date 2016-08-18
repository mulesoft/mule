/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;

public class MessagingExceptionHandlerToSystemAdapter implements MessagingExceptionHandler {

  private MuleContext muleContext;

  public MessagingExceptionHandlerToSystemAdapter(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public MuleEvent handleException(Exception exception, MuleEvent event) {
    muleContext.getExceptionListener().handleException(exception);
    return event;
  }
}
