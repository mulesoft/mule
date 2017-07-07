/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.interceptor;

import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.internal.util.rx.Operators.nullSafeMap;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.interceptor.Interceptor;
import org.mule.runtime.core.api.management.stats.ProcessingTime;
import org.mule.runtime.core.api.processor.MessageProcessors;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * <code>EnvelopeInterceptor</code> is an intercepter that will fire before and after an event is received.
 */
public abstract class AbstractEnvelopeInterceptor extends AbstractInterceptingMessageProcessor
    implements Interceptor {

  /**
   * This method is invoked before the event is processed
   */
  public abstract Event before(Event event) throws MuleException;

  /**
   * This method is invoked after the event has been processed, unless an exception was thrown
   */
  public abstract Event after(Event event) throws MuleException;

  @Override
  public Event process(Event event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).handle(nullSafeMap(checkedFunction(event -> before(event)))).transform(applyNext())
        .handle(nullSafeMap(checkedFunction(event -> after(event))));
  }
}
