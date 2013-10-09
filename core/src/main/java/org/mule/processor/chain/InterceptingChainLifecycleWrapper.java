/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor.chain;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.execution.MessageProcessorExecutionTemplate;

import java.util.List;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the
 * chain. This is so that if this chain is nested in another chain the next
 * MessageProcessor in the parent chain is not injected into the first in the nested
 * chain.
 */
public class InterceptingChainLifecycleWrapper extends AbstractMessageProcessorChain
{
    private MessageProcessorChain chain;
    private MessageProcessorExecutionTemplate messageProcessorExecutionTemplate = MessageProcessorExecutionTemplate.createExecutionTemplate();

    public InterceptingChainLifecycleWrapper(MessageProcessorChain chain,
                                             List<MessageProcessor> processors,
                                             String name)
    {
        super(name, processors);
        this.chain = chain;
    }

    @Override
    public List<MessageProcessor> getMessageProcessors()
    {
        return chain.getMessageProcessors();
    }

    @Override
    public String getName()
    {
        return chain.getName();
    }

    @Override
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        return chain.process(event);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        super.setMuleContext(context);
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof MuleContextAware)
            {
                ((MuleContextAware) processor).setMuleContext(context);
            }
        }
        if (chain instanceof MuleContextAware)
        {
            ((MuleContextAware) chain).setMuleContext(context);
        }
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event == null)
        {
            return null;
        }

        return messageProcessorExecutionTemplate.execute(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return InterceptingChainLifecycleWrapper.super.process(event);
            }
        },event);
    }
}
