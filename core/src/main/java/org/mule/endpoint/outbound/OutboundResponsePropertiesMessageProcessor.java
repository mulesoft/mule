/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.processor.AbstractRequestResponseMessageProcessor;

import java.util.List;

/**
 * Propagates properties from request message to response message as defined by
 * {@link OutboundEndpoint#getResponseProperties()}.
 * <p>
 * //TODO This can became a standard MessageProcessor in the response chain if/when
 * event has a (immutable) reference to request message.
 */
public class OutboundResponsePropertiesMessageProcessor extends AbstractRequestResponseMessageProcessor
{

    private OutboundEndpoint endpoint;

    public OutboundResponsePropertiesMessageProcessor(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    @Override
    protected MuleEvent processResponse(MuleEvent response, final MuleEvent request) throws MuleException
    {
        if (isEventValid(response))
        {
            // Properties which should be carried over from the request message
            // to the response message
            List<String> responseProperties = endpoint.getResponseProperties();
            for (String propertyName : responseProperties)
            {
                Object propertyValue = request.getMessage().getOutboundProperty(propertyName);
                if (propertyValue != null)
                {
                    response.getMessage().setOutboundProperty(propertyName, propertyValue);
                }
            }
        }
        return response;
    }
}
