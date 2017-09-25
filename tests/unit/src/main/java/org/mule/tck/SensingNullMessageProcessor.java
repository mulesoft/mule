/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.util.ObjectUtils;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.util.concurrent.NamedThreadFactory;

import java.util.concurrent.ExecutorService;

public class SensingNullMessageProcessor implements Processor, Disposable {

  public CoreEvent event;
  public Latch latch = new Latch();
  public Thread thread;
  private ExecutorService executor = newSingleThreadExecutor(new NamedThreadFactory(SensingNullMessageProcessor.class.getName()));

  protected InternalMessageSource source = new InternalMessageSource();
  private long waitTime = 0;
  private String appendString;

  public SensingNullMessageProcessor() {
    super();
  }

  public SensingNullMessageProcessor(String appendString) {
    this.appendString = appendString;
  }

  private void sense(CoreEvent event) {
    sleepIfNeeded();
    this.event = event;
    thread = Thread.currentThread();
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    sense(event);
    if (!isEmpty(appendString)) {
      event = append(event);
    }
    latch.countDown();
    if (source.listener != null) {
      return source.listener.process(event);
    } else {
      return event;
    }
  }

  private CoreEvent append(CoreEvent event) {
    return CoreEvent.builder(event).message(of(event.getMessage().getPayload().getValue() + appendString)).build();
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

  class InternalMessageSource extends AbstractComponent implements MessageSource {

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
