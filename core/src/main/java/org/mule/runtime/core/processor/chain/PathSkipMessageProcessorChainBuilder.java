/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;

/**
 * Constructs a chain of {@link org.mule.runtime.core.api.processor.MessageProcessor}s and wraps the invocation of the chain in a composite
 * MessageProcessor. This Builder wrapps the chain in an {@link InterceptingChainLifecycleWrapperPathSkip},
 * so it does not add a level into the path elements.
 */
public class PathSkipMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder {


  public PathSkipMessageProcessorChainBuilder(MuleContext muleContext) {
    super(muleContext);
  }

  public PathSkipMessageProcessorChainBuilder(FlowConstruct flowConstruct) {
    super(flowConstruct);
  }

  @Override
  protected InterceptingChainLifecycleWrapper buildMessageProcessorChain(DefaultMessageProcessorChain chain) {
    return new InterceptingChainLifecycleWrapperPathSkip(chain, processors, name);
  }
}
