/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class MessageProcessorChainFactoryBean implements FactoryBean
{

    protected List processors;
    protected String name;

    public Class getObjectType()
    {
        return MessageProcessor.class;
    }

    public void setMessageProcessors(List processors)
    {
        this.processors = processors;
    }

    public Object getObject() throws Exception
    {
        MessageProcessorChainBuilder builder = getBuilderInstance();
        for (Object processor : processors)
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
        return builder.build();
    }

    protected MessageProcessorChainBuilder getBuilderInstance()
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.setName("processor chain '"+name+"'");
        return builder;
    }

    public boolean isSingleton()
    {
        return false;
    }

    public void setName(String name)
    {
        this.name = name;
    }

}
