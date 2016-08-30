/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RedeliveryExceeded implements FlowConstructAware, Initialisable {

  private List<MessageProcessor> messageProcessors = new CopyOnWriteArrayList<>();
  private MessageProcessorChain configuredMessageProcessors;
  private FlowConstruct flowConstruct;

  @Override
  public void initialise() throws InitialisationException {
    DefaultMessageProcessorChainBuilder defaultMessageProcessorChainBuilder =
        new DefaultMessageProcessorChainBuilder(this.flowConstruct);
    try {
      configuredMessageProcessors = defaultMessageProcessorChainBuilder.chain(messageProcessors).build();
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
  }

  public List<MessageProcessor> getMessageProcessors() {
    return Collections.unmodifiableList(messageProcessors);
  }

  public void setMessageProcessors(List<MessageProcessor> processors) {
    if (processors != null) {
      this.messageProcessors.clear();
      this.messageProcessors.addAll(processors);
    } else {
      throw new IllegalArgumentException("List of targets = null");
    }
  }

  public MuleEvent process(MuleEvent event) throws MuleException {
    MuleEvent result = event;
    if (!messageProcessors.isEmpty()) {
      result = configuredMessageProcessors.process(event);
    }
    if (result != null && !VoidMuleEvent.getInstance().equals(result)) {
      result = MuleEvent.builder(result).error(null)
          .message(MuleMessage.builder(event.getMessage()).exceptionPayload(null).build()).build();
    }
    return result;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}
