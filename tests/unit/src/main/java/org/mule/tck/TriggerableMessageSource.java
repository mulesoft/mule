/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static reactor.core.publisher.Flux.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.NonBlockingMessageSource;
import org.mule.runtime.core.util.ObjectUtils;

import org.reactivestreams.Publisher;

public class TriggerableMessageSource implements NonBlockingMessageSource {

  protected Processor listener;

  public Event trigger(Event event) throws MuleException {
    return listener.process(event);
  }

  public Publisher<Event> triggerAsync(Event event) {
    return just(event).transform(listener);
  }

  public void setListener(Processor listener) {
    this.listener = listener;
  }

  @Override
  public String toString() {
    return ObjectUtils.toString(this);
  }

}
