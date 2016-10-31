/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.exception.MessagingException;

public class ExceptionHandlingMessageProcessor extends AbstractInterceptingMessageProcessor {

  @Override
  public Event process(Event event) throws MuleException {
    try {
      return processNext(event);
    } catch (Exception e) {
      e = (Exception) ExceptionHelper.sanitizeIfNeeded(e);
      return flowConstruct.getExceptionListener().handleException(new MessagingException(event, e, this), event);
    }
  }
}
