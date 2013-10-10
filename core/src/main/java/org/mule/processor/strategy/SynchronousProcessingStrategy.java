/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor.strategy;

import org.mule.api.MuleContext;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.ProcessingStrategy;

import java.util.List;

/**
 * This strategy processes all message processors in the calling thread.
 */
public class SynchronousProcessingStrategy implements ProcessingStrategy
{
    @Override
    public void configureProcessors(List<MessageProcessor> processors,
                                    StageNameSource nameSource,
                                    MessageProcessorChainBuilder chainBuilder,
                                    MuleContext muleContext)
    {
        for (Object processor : processors)
        {
            if (processor instanceof MessageProcessor)
            {
                chainBuilder.chain((MessageProcessor) processor);
            }
            else if (processor instanceof MessageProcessorBuilder)
            {
                chainBuilder.chain((MessageProcessorBuilder) processor);
            }
            else
            {
                throw new IllegalArgumentException(
                    "MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
            }
        }
    }

}
