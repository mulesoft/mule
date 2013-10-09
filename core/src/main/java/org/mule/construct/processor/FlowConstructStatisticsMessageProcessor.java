/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.construct.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;

public class FlowConstructStatisticsMessageProcessor implements MessageProcessor, FlowConstructAware
{
    protected FlowConstruct flowConstruct;

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (flowConstruct.getStatistics().isEnabled())
        {
            if (event.getExchangePattern().hasResponse())
            {
                flowConstruct.getStatistics().incReceivedEventSync();
            }
            else
            {
                flowConstruct.getStatistics().incReceivedEventASync();
            }
        }

        return event;
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
