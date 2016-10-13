/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;

import java.util.concurrent.ExecutorService;

public class SensingNullMessageProcessor implements Processor, Disposable {

  public Event event;
  public Latch latch = new Latch();
  public Thread thread;
  private ExecutorService executor = newSingleThreadExecutor(new NamedThreadFactory(SensingNullMessageProcessor.class.getName()));

  protected InternalMessageSource source = new InternalMessageSource();
  private long waitTime = 0;
  private boolean enableNonBlocking = true;
  private String appendString;

  public SensingNullMessageProcessor() {
    super();
  }

  public SensingNullMessageProcessor(String appendString) {
    this.appendString = appendString;
  }

  private void sense(Event event) {
    sleepIfNeeded();
    this.event = event;
    thread = Thread.currentThread();
  }

  @Override
  public Event process(Event event) throws MuleException {
    sense(event);
    if (StringUtils.isNotEmpty(appendString)) {
      event = append(event);
    }
    latch.countDown();
    if (source.listener != null) {
      return source.listener.process(event);
    } else {
      if (event.getExchangePattern().hasResponse()) {
        return event;
      } else {
        return VoidMuleEvent.getInstance();
      }
    }
  }

  private Event append(Event event) {
    return Event.builder(event)
        .message(InternalMessage.builder().payload(event.getMessage().getPayload().getValue() + appendString).build())
        .build();
  }

  private void sleepIfNeeded() {
    if (waitTime > 0) {
      try {
        Thread.sleep(waitTime);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void dispose() {
    executor.shutdown();
  }

  public void clear() {
    event = null;
  }

  public MessageSource getMessageSource() {
    return source;
  }

  public void setWaitTime(long waitTime) {
    this.waitTime = waitTime;
  }

  class InternalMessageSource implements MessageSource {

    Processor listener;

    @Override
    public void setListener(Processor listener) {
      this.listener = listener;

    }

    @Override
    public String toString() {
      return ObjectUtils.toString(this);
    }
  }
}
