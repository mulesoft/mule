/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.PropertyScope;
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

        if (responseEvent != null)
        {
            // Properties which should be carried over from the request message
            // to the response message
            List<String> responseProperties = endpoint.getResponseProperties();
            for (String propertyName : responseProperties)
            {
                // TODO this piece of code looks moot now, may need to go
                Object propertyValue = event.getMessage().getProperty(propertyName, PropertyScope.OUTBOUND);
                if (propertyValue != null)
                {
                    responseEvent.getMessage().setProperty(propertyName, propertyValue, PropertyScope.OUTBOUND);
                }
            }
        }
        return responseEvent;
    }
}
