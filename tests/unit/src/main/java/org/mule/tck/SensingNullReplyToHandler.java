/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.concurrent.Latch;

public class SensingNullReplyToHandler implements ReplyToHandler {

  public Event event;
  public Latch latch = new Latch();

  @Override
  public Event processReplyTo(Event event, Message returnMessage, Object replyTo) throws MuleException {
    this.event = event;
    latch.countDown();
    return event;
  }

  public void clear() {
    event = null;
  }
}
