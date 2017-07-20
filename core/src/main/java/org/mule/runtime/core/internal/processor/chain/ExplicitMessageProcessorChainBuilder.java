/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;

/**
 * Constructs a {@link MessageProcessorChain} in the same way that the {@link DefaultMessageProcessorChainBuilder} constructs
 * chains but
 */
public class ExplicitMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder {

  protected MessageProcessorChain createInterceptingChain(Processor head, List<Processor> processors,
                                                          List<Processor> processorForLifecycle) {
    return new ExplicitMessageProcessorChain(name, head, processors, processorForLifecycle);
  }

  /**
   * Generates message processor identfiers specific for subflows.
   */
  public static class ExplicitMessageProcessorChain extends DefaultMessageProcessorChain {

    protected ExplicitMessageProcessorChain(String name, Processor head, List<Processor> processors,
                                            List<Processor> processorsForLifecycle) {
      super(name, head, processors, processorsForLifecycle);
    }

  }
}
