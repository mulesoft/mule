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

import org.mule.api.MuleContext;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;

import java.util.List;

/**
 * Simple implementation of {@link AbstractFlowConstruct} that allows a list of
 * {@link MessageProcessor}s that will be used to process messages to be configured.
 * These MessageProcessors are chained together using the
 * {@link InterceptingChainMessageProcessorBuilder}.
 * <p/>
 * If not message processors are configured then the source message is simply
 * returned.
 */
public class SimpleFlowConstruct extends AbstractFlowConstruct
{
    protected List messageProcessors;

    public void setMessageProcessors(List messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    public List getMessageProcessors()
    {
        return messageProcessors;
    }

    public SimpleFlowConstruct(String name, MuleContext muleContext)
    {
        super(name, muleContext);
    }

    @Override
    protected void configureMessageProcessors(InterceptingChainMessageProcessorBuilder builder)
    {
        for (Object processor : messageProcessors)
        {
            if (processor instanceof MessageProcessor)
            {
                builder.chain((MessageProcessor) processor);
            }
            else if (processor instanceof MessageProcessorBuilder)
            {
                builder.chain((MessageProcessorBuilder) processor);
            }
            else
            {
                throw new IllegalArgumentException(
                    "MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
            }
        }
    }

    @Deprecated
    public void setEndpoint(InboundEndpoint endpoint)
    {
        this.messageSource = endpoint;
    }
}
