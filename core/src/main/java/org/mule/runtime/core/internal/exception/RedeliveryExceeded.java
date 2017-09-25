/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;

import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.message.InternalMessage;

import org.reactivestreams.Publisher;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import reactor.core.publisher.Flux;

public class RedeliveryExceeded extends AbstractComponent implements Initialisable, ReactiveProcessor {

  @Inject
  private MuleContext muleContext;

  private List<Processor> messageProcessors = new CopyOnWriteArrayList<>();
  private MessageProcessorChain configuredMessageProcessors;

  @Override
  public void initialise() throws InitialisationException {
    configuredMessageProcessors =
        newChain(getProcessingStrategy(muleContext, getRootContainerName()), messageProcessors);
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

  public CoreEvent process(CoreEvent event) throws MuleException {
    CoreEvent result = event;
    if (!messageProcessors.isEmpty()) {
      result = configuredMessageProcessors.process(event);
    }
    if (result != null) {
      result = removeErrorFromEvent(result);
    }
    return result;
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> eventPublisher) {
    if (!messageProcessors.isEmpty()) {
      return Flux.from(eventPublisher).transform(configuredMessageProcessors);
    } else {
      return Flux.from(eventPublisher).map(event -> removeErrorFromEvent(event));
    }
  }

  private CoreEvent removeErrorFromEvent(CoreEvent result) {
    return CoreEvent.builder(result).error(null)
        .message(InternalMessage.builder(result.getMessage()).exceptionPayload(null).build()).build();
  }

}
