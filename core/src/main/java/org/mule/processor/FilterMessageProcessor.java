/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.routing.filter.FilterException;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.AbstractInterceptingMessageProcessor;

public class FilterMessageProcessor extends AbstractInterceptingMessageProcessor
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleMessage message = event.getMessage();
        InboundEndpoint endpoint = (InboundEndpoint) event.getEndpoint();

        // Apply the endpoint filter if one is configured
        if (endpoint.getFilter() != null && !endpoint.getFilter().accept(message))
        {
            // We need to update event in RequestContext because MessageAwareTransformer's use this.
            RequestContext.setEvent(new DefaultMuleEvent(message, event));            
            throw new FilterException(CoreMessages.messageRejectedByFilter(), endpoint.getFilter());
        }
        return processNext(event);
    }

    protected MuleMessage handleUnacceptedFilter(MuleMessage message, InboundEndpoint endpoint)
    {
        String messageId;
        messageId = message.getUniqueId();

        if (logger.isDebugEnabled())
        {
            logger.debug("Message " + messageId + " failed to pass filter on endpoint: " + endpoint
                         + ". Message is being ignored");
        }

        return message;
    }

}
