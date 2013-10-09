/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;


public class OutboundEventTimeoutMessageProcessor implements MessageProcessor
{

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        int timeout = event.getMessage().getOutboundProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, -1);
        if (timeout >= 0)
        {
            event.setTimeout(timeout);
        }
        return event;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
