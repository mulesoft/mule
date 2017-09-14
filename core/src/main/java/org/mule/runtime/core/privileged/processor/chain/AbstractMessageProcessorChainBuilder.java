/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Constructs a chain of {@link Processor}s and wraps the invocation of the chain in a composite MessageProcessor. Both
 * MessageProcessors and InterceptingMessageProcessor's can be chained together arbitrarily in a single chain.
 * InterceptingMessageProcessors simply intercept the next MessageProcessor in the chain. When other non-intercepting
 * MessageProcessors are used an adapter is used internally to chain the MessageProcessor with the next in the chain.
 * </p>
 * <p>
 * The MessageProcessor instance that this builder builds can be nested in other chains as required.
 * </p>
 */
abstract class AbstractMessageProcessorChainBuilder implements MessageProcessorChainBuilder {

  protected List processors = new ArrayList();
  protected String name;
  protected ProcessingStrategy processingStrategy;
  protected MuleContext muleContext;

  // Argument is of type Object because it could be a MessageProcessor or a MessageProcessorBuilder
  protected Processor initializeMessageProcessor(Object processor) {
    if (processor instanceof MessageProcessorBuilder) {
      return ((MessageProcessorBuilder) processor).build();
    } else {
      return (Processor) processor;
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setProcessingStrategy(ProcessingStrategy processingStrategy) {
    this.processingStrategy = processingStrategy;
  }
}
