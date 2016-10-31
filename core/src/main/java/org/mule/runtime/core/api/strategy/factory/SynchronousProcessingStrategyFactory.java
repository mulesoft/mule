/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.strategy.factory;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;

/**
 * This factory's strategies processes all message processors in the calling thread.
 */
public class SynchronousProcessingStrategyFactory implements ProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create() {
    return new SynchronousProcessingStrategy();
  }

  public static class SynchronousProcessingStrategy implements ProcessingStrategy {

    @Override
    public void configureProcessors(List<Processor> processors,
                                    org.mule.runtime.core.api.processor.StageNameSource nameSource,
                                    MessageProcessorChainBuilder chainBuilder, MuleContext muleContext) {
      for (Object processor : processors) {
        if (processor instanceof Processor) {
          chainBuilder.chain((Processor) processor);
        } else if (processor instanceof MessageProcessorBuilder) {
          chainBuilder.chain((MessageProcessorBuilder) processor);
        } else {
          throw new IllegalArgumentException("MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
        }
      }
    }

  }

}
