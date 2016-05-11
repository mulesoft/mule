/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class ResponseMessageProcessorsFactoryBean implements FactoryBean, MuleContextAware
{

    protected List messageProcessors;
    private MuleContext muleContext;

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
        responseAdapter.setMuleContext(muleContext);
        return responseAdapter;
    }

    public boolean isSingleton()
    {
        return false;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

}
