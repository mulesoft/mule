/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.NamedObject;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.processor.AsyncDelegateMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.util.concurrent.ThreadNameHelper;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class AsyncMessageProcessorsFactoryBean implements FactoryBean, MuleContextAware, NamedObject
{

    protected MuleContext muleContext;

    protected List messageProcessors;
    protected ThreadingProfile threadingProfile;
    protected String name;

    public Class getObjectType()
    {
        return MessageProcessor.class;
    }

    public void setThreadingProfile(ThreadingProfile threadingProfile)
    {
        this.threadingProfile = threadingProfile;
    }

    public void setMessageProcessors(List messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    public Object getObject() throws Exception
    {
        if (threadingProfile == null)
        {
            threadingProfile = muleContext.getDefaultThreadingProfile();
        }

        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.setName("'async' child chain");
        final MuleConfiguration config = muleContext.getConfiguration();
        final String threadPrefix = ThreadNameHelper.asyncProcessor(muleContext, name);

        AsyncDelegateMessageProcessor asyncProcessor = new AsyncDelegateMessageProcessor(threadingProfile,
                                                                                                 threadPrefix,
                                                                                                 config.getShutdownTimeout());
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
        asyncProcessor.setDelegate(builder.build());
        return  asyncProcessor;
    }

    public boolean isSingleton()
    {
        return false;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

}
