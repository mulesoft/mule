/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.component;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.Message;

/**
 * Thrown when a static file is requested but not found
 */
public class ResourceNotFoundException extends MessagingException {

  private static final long serialVersionUID = -6693780652453067693L;

  public ResourceNotFoundException(Message message, MuleEvent event, MessageProcessor failingMessageProcessor) {
    super(message, event, failingMessageProcessor);
  }

  public ResourceNotFoundException(Message message, MuleEvent event, Throwable cause) {
    super(message, event, cause);
  }
}
