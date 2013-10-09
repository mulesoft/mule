/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.factories;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessors;
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
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.setName("processor chain '"+name+"'");
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
        return MessageProcessors.lifecyleAwareMessageProcessorWrapper(builder.build());
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
