/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.components;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.Message;

public class RestServiceException extends MessagingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -1026055907767407434L;

  public RestServiceException(Message message, MuleEvent event, MessageProcessor failingMessageProcessor) {
    super(message, event, failingMessageProcessor);
  }

  public RestServiceException(Message message, MuleEvent event, Throwable cause, MessageProcessor failingMessageProcessor) {
    super(message, event, cause, failingMessageProcessor);
  }
}
