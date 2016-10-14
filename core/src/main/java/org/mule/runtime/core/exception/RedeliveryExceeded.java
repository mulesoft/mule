/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RedeliveryExceeded implements FlowConstructAware, Initialisable {

  private List<Processor> messageProcessors = new CopyOnWriteArrayList<>();
  private MessageProcessorChain configuredMessageProcessors;
  private FlowConstruct flowConstruct;

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
      result = Event.builder(result).error(null)
          .message(InternalMessage.builder(result.getMessage()).exceptionPayload(null).build()).build();
    }
    return result;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}
