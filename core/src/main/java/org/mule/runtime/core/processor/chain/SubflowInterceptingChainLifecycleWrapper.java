/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.notification.FlowTraceManager;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.util.NotificationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates message processor identfiers specific for subflows.
 */
public class SubflowInterceptingChainLifecycleWrapper extends InterceptingChainLifecycleWrapper implements SubFlowMessageProcessor
{

    private String subFlowName;

    public SubflowInterceptingChainLifecycleWrapper(MessageProcessorChain chain, List<MessageProcessor> processors, String name)
    {
        super(chain, processors, name);
        this.subFlowName = name;
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        MessageProcessorPathElement subprocessors = pathElement.addChild(name).addChild("subprocessors");
        //Only MP till first InterceptiongMessageProcessor should be used to generate the Path,
        // since the next ones will be generated by the InterceptingMessageProcessor because they are added as an inned chain
        List<MessageProcessor> filteredMessageProcessorList = new ArrayList<MessageProcessor>();
        for (MessageProcessor messageProcessor : processors)
        {
            if (messageProcessor instanceof InterceptingMessageProcessor)
            {
                filteredMessageProcessorList.add(messageProcessor);
                break;
            }
            else
            {
                filteredMessageProcessorList.add(messageProcessor);
            }
        }
        NotificationUtils.addMessageProcessorPathElements(filteredMessageProcessorList, subprocessors);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        FlowTraceManager flowTraceManager = event.getMuleContext().getFlowTraceManager();
        flowTraceManager.onFlowStart(event, getSubFlowName());

        try
        {
            return super.process(event);
        }
        finally
        {
            flowTraceManager.onFlowComplete(event);
        }
    }

    @Override
    public String getSubFlowName()
    {
        return subFlowName;
    }
}
