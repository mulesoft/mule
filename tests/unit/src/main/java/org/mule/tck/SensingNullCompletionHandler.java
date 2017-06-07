/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.runtime.core.execution.BlockingCompletionHandler;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.util.concurrent.Latch;

public class SensingNullCompletionHandler extends BlockingCompletionHandler<Event, MessagingException> {

  public Event event;
  public Exception exception;
  public Latch latch = new Latch();

  @Override
  protected void doOnCompletion(Event result) {
    this.event = result;
    latch.countDown();
  }

  @Override
  public void onFailure(MessagingException exception) {
    this.exception = exception;
    latch.countDown();
  }

  public void clear() {
    event = null;
  }

}
