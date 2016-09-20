/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.processor;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test non-blocking {@link Processor} implementation that simply uses a @{link Executor} to invoke the
 * {@link org.mule.runtime.core.api.connector.ReplyToHandler} in another thread.
 */
public class TestNonBlockingProcessor implements Processor, Initialisable, Disposable {

  private ExecutorService executor;

  @Override
  public Event process(final Event event) throws MuleException {
    if (event.isAllowNonBlocking() && event.getReplyToHandler() != null) {
      executor.execute(() -> {
        try {
          event.getReplyToHandler().processReplyTo(event, null, null);
        } catch (MessagingException e1) {
          event.getReplyToHandler().processExceptionReplyTo(e1, null);
        } catch (MuleException e2) {
          event.getReplyToHandler().processExceptionReplyTo(new MessagingException(event, e2), null);
        }
      });
      return VoidMuleEvent.getInstance();
    } else {
      return event;
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    executor = Executors.newCachedThreadPool();
  }

  @Override
  public void dispose() {
    executor.shutdown();
  }
}
