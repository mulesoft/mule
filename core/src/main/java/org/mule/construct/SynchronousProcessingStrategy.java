/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import org.mule.api.construct.Pipeline;
import org.mule.api.construct.PipelineProcessingStrategy;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChainBuilder;

/**
 * This strategy processes all message processors in the calling thread.
 */
public class SynchronousProcessingStrategy implements PipelineProcessingStrategy
{
    @Override
    public void configureProcessors(Pipeline pipeline, MessageProcessorChainBuilder chainBuilder)
    {
        for (Object processor : pipeline.getMessageProcessors())
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
