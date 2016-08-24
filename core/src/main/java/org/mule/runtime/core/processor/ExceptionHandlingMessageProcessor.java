/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.config.ExceptionHelper;

public class ExceptionHandlingMessageProcessor extends AbstractInterceptingMessageProcessor {

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    try {
      return processNext(event);
    } catch (Exception e) {
      e = (Exception) ExceptionHelper.sanitizeIfNeeded(e);
      return flowConstruct.getExceptionListener().handleException(e, event);
    }
  }
}
