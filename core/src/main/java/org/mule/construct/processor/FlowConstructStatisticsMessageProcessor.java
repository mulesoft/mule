/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
