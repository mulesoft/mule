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
 * Sets the outbound endpoint uri on as a property of the message using the following key:
 * {@link MuleProperties#MULE_ENDPOINT_PROPERTY}.
 */
public class OutboundEndpointPropertyMessageProcessor implements MessageProcessor
{

    private String[] ignoredPropertyOverrides = new String[]{MuleProperties.MULE_METHOD_PROPERTY};

    private OutboundEndpoint endpoint;

    public OutboundEndpointPropertyMessageProcessor(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        event.getMessage().setOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY,
            endpoint.getEndpointURI().toString());

        if (endpoint.getProperties() != null)
        {
            for (Iterator<?> iterator = endpoint.getProperties().keySet().iterator(); iterator.hasNext();)
            {
                String prop = (String) iterator.next();
                Object value = endpoint.getProperties().get(prop);
                // don't overwrite property on the message
                if (!ignoreProperty(event.getMessage(), prop))
                {
                    // inbound endpoint properties are in the invocation scope
                    event.getMessage().setInvocationProperty(prop, value);
                }
            }
        }
        event = OptimizedRequestContext.unsafeSetEvent(event);
        return event;
    }

    protected boolean ignoreProperty(MuleMessage message, String key)
    {
        if (key == null)
        {
            return true;
        }

        for (int i = 0; i < ignoredPropertyOverrides.length; i++)
        {
            if (key.equals(ignoredPropertyOverrides[i]))
            {
                return false;
            }
        }

        return null != message.getOutboundProperty(key);
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
