/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.privileged.connector.ReplyToHandler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.concurrent.Latch;

public class SensingNullReplyToHandler implements ReplyToHandler {

  public CoreEvent event;
  public Latch latch = new Latch();

  @Override
  public CoreEvent processReplyTo(CoreEvent event, Message returnMessage, Object replyTo) throws MuleException {
    this.event = event;
    latch.countDown();
    return event;
  }

  public void clear() {
    event = null;
  }
}
