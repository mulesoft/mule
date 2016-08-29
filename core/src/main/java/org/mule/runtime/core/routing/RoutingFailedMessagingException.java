/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.Message;

/**
 * Exception through by routing strategies when routing fails
 */
public class RoutingFailedMessagingException extends MessagingException {

  public RoutingFailedMessagingException(Message message, MuleEvent event) {
    super(message, event);
  }

  public RoutingFailedMessagingException(Message message, MuleEvent event, MessageProcessor failingMessageProcessor) {
    super(message, event, failingMessageProcessor);
  }

  public RoutingFailedMessagingException(Message message, MuleEvent event, Throwable cause) {
    super(message, event, cause);
  }

  public RoutingFailedMessagingException(MuleEvent event, Throwable cause) {
    super(event, cause);
  }

  public RoutingFailedMessagingException(Message message, MuleEvent event, Throwable cause,
                                         MessageProcessor failingMessageProcessor) {
    super(message, event, cause, failingMessageProcessor);
  }

  public RoutingFailedMessagingException(MuleEvent event, Throwable cause, MessageProcessor failingMessageProcessor) {
    super(event, cause, failingMessageProcessor);
  }
}
