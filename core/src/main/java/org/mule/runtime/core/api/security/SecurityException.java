/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.I18nMessage;

/**
 * <code>SecurityException</code> is a generic security exception
 */
public abstract class SecurityException extends MessagingException {

  protected SecurityException(I18nMessage message, Event event) {
    super(message, event);
  }

  protected SecurityException(I18nMessage message, Event event, Throwable cause) {
    super(message, event, cause);
  }

  protected SecurityException(I18nMessage message, Event event, Throwable cause, Processor failingMessageProcessor) {
    super(message, event, cause, failingMessageProcessor);
  }
}
