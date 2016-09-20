/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.util.concurrent.Latch;

public class SensingNullReplyToHandler implements ReplyToHandler {

  public Event event;
  public Exception exception;
  public Latch latch = new Latch();

  @Override
  public Event processReplyTo(Event event, InternalMessage returnMessage, Object replyTo) throws MuleException {
    this.event = event;
    latch.countDown();
    return event;
  }

  @Override
  public void processExceptionReplyTo(MessagingException exception, Object replyTo) {
    this.exception = exception;
    latch.countDown();
  }

  public void clear() {
    event = null;
  }
}
