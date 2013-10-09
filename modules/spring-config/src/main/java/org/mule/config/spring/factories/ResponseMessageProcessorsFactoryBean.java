/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.factories;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class ResponseMessageProcessorsFactoryBean implements FactoryBean
{

    protected List messageProcessors;

    public Class getObjectType()
    {
        return MessageProcessor.class;
    }

    public void setMessageProcessors(List messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    public Object getObject() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.setName("'response' child processor chain");
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
        ResponseMessageProcessorAdapter responseAdapter = new ResponseMessageProcessorAdapter();
        responseAdapter.setProcessor(builder.build());
        return responseAdapter;
    }

    public boolean isSingleton()
    {
        return false;
    }

}
