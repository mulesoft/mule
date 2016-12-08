/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static reactor.core.publisher.Flux.from;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.NonBlockingMessageSource;
import org.mule.runtime.core.util.ObjectUtils;

import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class TriggerableMessageSource implements NonBlockingMessageSource, Function<Publisher<Event>, Publisher<Event>> {

  protected Processor listener;

  public TriggerableMessageSource() {
    // empty
  }

  public TriggerableMessageSource(Processor listener) {
    this.listener = listener;
  }

  public Event trigger(Event event) throws MuleException {
    return listener.process(event);
  }

  public void setListener(Processor listener) {
    this.listener = listener;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).transform(listener);
  }
}
