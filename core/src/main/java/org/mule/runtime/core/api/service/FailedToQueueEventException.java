/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.service;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.Message;

/**
 * <code>FailedToQueueEventException</code> is thrown when an event cannot be put on an internal service queue.
 */

public class FailedToQueueEventException extends MessagingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -8368283988424746098L;

  public FailedToQueueEventException(Message message, MuleEvent event, MessageProcessor failingMessageProcessor) {
    super(message, event, failingMessageProcessor);
  }

  public FailedToQueueEventException(Message message, MuleEvent event, Throwable cause,
                                     MessageProcessor failingMessageProcessor) {
    super(message, event, cause, failingMessageProcessor);
  }
}
