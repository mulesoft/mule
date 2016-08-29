/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.processor.AbstractNonBlockingMessageProcessor;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.concurrent.Latch;

import java.util.concurrent.Executors;

public class SensingNullMessageProcessor extends AbstractNonBlockingMessageProcessor implements MessageProcessor {

  public MuleEvent event;
  public Latch latch = new Latch();
  public Thread thread;

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

  @Override
  protected void processNonBlocking(final MuleEvent event, CompletionHandler completionHandler) throws MuleException {
    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        sense(event);
        MuleEvent eventToProcess = event;
        if (StringUtils.isNotEmpty(appendString)) {
          eventToProcess = append(eventToProcess);
        }
        event.getReplyToHandler().processReplyTo(eventToProcess, null, null);
        latch.countDown();
      } catch (MuleException e) {
        event.getReplyToHandler().processExceptionReplyTo(new MessagingException(event, e), null);
      }
    });
  }

  private void sense(MuleEvent event) {
    sleepIfNeeded();
    this.event = event;
    thread = Thread.currentThread();
  }

  @Override
  protected MuleEvent processBlocking(MuleEvent event) throws MuleException {
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

  @Override
  public boolean isNonBlocking(MuleEvent event) {
    return super.isNonBlocking(event) && enableNonBlocking;
  }

  private MuleEvent append(MuleEvent event) {
    return MuleEvent.builder(event).message(MuleMessage.builder().payload(event.getMessage().getPayload() + appendString).build())
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

    MessageProcessor listener;

    @Override
    public void setListener(MessageProcessor listener) {
      this.listener = listener;

    }

    @Override
    public String toString() {
      return ObjectUtils.toString(this);
    }
  }
}
