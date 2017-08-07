/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class RedeliveryExceeded implements Initialisable, ReactiveProcessor {

  private List<Processor> messageProcessors = new CopyOnWriteArrayList<>();
  private MessageProcessorChain configuredMessageProcessors;

  @Override
  public void initialise() throws InitialisationException {
    configuredMessageProcessors = newChain(messageProcessors);
  }

  public List<Processor> getMessageProcessors() {
    return Collections.unmodifiableList(messageProcessors);
  }

  public void setMessageProcessors(List<Processor> processors) {
    if (processors != null) {
      this.messageProcessors.clear();
      this.messageProcessors.addAll(processors);
    } else {
      throw new IllegalArgumentException("List of targets = null");
    }
  }

  public Event process(Event event) throws MuleException {
    Event result = event;
    if (!messageProcessors.isEmpty()) {
      result = configuredMessageProcessors.process(event);
    }
    if (result != null) {
      result = removeErrorFromEvent(result);
    }
    return result;
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> eventPublisher) {
    if (!messageProcessors.isEmpty()) {
      return Flux.from(eventPublisher).transform(configuredMessageProcessors);
    } else {
      return Flux.from(eventPublisher).map(event -> removeErrorFromEvent(event));
    }
  }

  private Event removeErrorFromEvent(Event result) {
    return Event.builder(result).error(null)
        .message(InternalMessage.builder(result.getMessage()).exceptionPayload(null).build()).build();
  }

}
