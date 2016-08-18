/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.MessageProcessor;

import java.util.LinkedList;

public class SimpleMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder {

  public SimpleMessageProcessorChainBuilder(FlowConstruct flowConstruct) {
    super(flowConstruct);
  }

  public SimpleMessageProcessorChainBuilder(MuleContext muleContext) {
    super(muleContext);
  }

  @Override
  protected DefaultMessageProcessorChain createInnerChain(LinkedList<MessageProcessor> tempList) {
    return new SimpleMessageProcessorChain(tempList);
  }

  @Override
  protected DefaultMessageProcessorChain createOuterChain(LinkedList<MessageProcessor> tempList) {
    return new SimpleMessageProcessorChain(tempList);
  }

}
