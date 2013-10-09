/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;

import java.util.Iterator;

/**
 * Sets the outbound root message id on as a property of the message using the following key:
 * {@link org.mule.api.config.MuleProperties#MULE_ROOT_MESSAGE_ID_PROPERTY}.
 */
public class OutboundRootMessageIdPropertyMessageProcessor implements MessageProcessor
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        event.getMessage().setOutboundProperty(MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY,
            event.getMessage().getMessageRootId());
        return event;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
