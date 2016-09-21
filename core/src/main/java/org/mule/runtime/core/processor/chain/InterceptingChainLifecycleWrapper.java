/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static org.mule.runtime.core.execution.MessageProcessorExecutionTemplate.createExecutionTemplate;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;

import java.util.List;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the chain. This is so that if this chain is
 * nested in another chain the next MessageProcessor in the parent chain is not injected into the first in the nested chain.
 */
public class InterceptingChainLifecycleWrapper extends AbstractMessageProcessorChain {

  private DefaultMessageProcessorChain chain;
  private MessageProcessorExecutionTemplate messageProcessorExecutionTemplate = createExecutionTemplate();

  public InterceptingChainLifecycleWrapper(DefaultMessageProcessorChain chain, List<Processor> processors, String name) {
    super(name, processors);
    this.chain = chain;
  }

  @Override
  public List<Processor> getMessageProcessors() {
    return chain.getMessageProcessors();
  }

  @Override
  public String getName() {
    return chain.getName();
  }

  @Override
  protected Event doProcess(Event event) throws MuleException {
    return chain.process(event);
  }

  @Override
  public Event process(Event event) throws MuleException {
    if (event == null) {
      return null;
    }

    return messageProcessorExecutionTemplate.execute(event1 -> InterceptingChainLifecycleWrapper.super.process(event1), event);
  }

  public void setTemplateMuleContext(MuleContext context) {
    messageProcessorExecutionTemplate.setMuleContext(context);
    chain.setTemplateMuleContext(context);
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    messageProcessorExecutionTemplate.setFlowConstruct(flowConstruct);
    chain.setFlowConstruct(flowConstruct);
    super.setFlowConstruct(flowConstruct);
  }
}
