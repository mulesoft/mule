/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.processor.AbstractInterceptingMessageProcessor;

import java.util.List;

/**
 * Propagates properties from request message to response message as defined by
 * {@link OutboundEndpoint#getResponseProperties()}.
 * <p>
 * //TODO This can became a standard MessageProcessor in the response chain if/when
 * event has a (immutable) reference to request message.
 */
public class OutboundResponsePropertiesMessageProcessor extends AbstractInterceptingMessageProcessor
{

    private OutboundEndpoint endpoint;

    public OutboundResponsePropertiesMessageProcessor(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent responseEvent = processNext(event);

        if (isEventValid(responseEvent))
        {
            // Properties which should be carried over from the request message
            // to the response message
            List<String> responseProperties = endpoint.getResponseProperties();
            for (String propertyName : responseProperties)
            {
                Object propertyValue = event.getMessage().getOutboundProperty(propertyName);
                if (propertyValue != null)
                {
                    responseEvent.getMessage().setOutboundProperty(propertyName, propertyValue);
                }
            }
        }
        return responseEvent;
    }
}
