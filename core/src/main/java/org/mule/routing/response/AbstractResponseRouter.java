/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.api.MuleMessage;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.routing.ResponseRouter;
import org.mule.routing.AbstractRouter;
import org.mule.routing.MuleMessageInfoMapping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractResponseRouter</code> is a base class for all Response Routers
 */

public abstract class AbstractResponseRouter extends AbstractRouter implements ResponseRouter
{
    protected final Log logger = LogFactory.getLog(getClass());

    protected MessageInfoMapping messageInfoMapping = new MuleMessageInfoMapping();


    public MessageInfoMapping getMessageInfoMapping()
    {
        return messageInfoMapping;
    }

    public void setMessageInfoMapping(MessageInfoMapping messageInfoMapping)
    {
        this.messageInfoMapping = messageInfoMapping;
    }


    /**
     * Extracts a 'Correlation Id' from a reply message. The correlation Id does not
     * have to be the Message Correlation Id. It can be extracted from the message
     * payload if desired.
     * 
     * @param message a received reply message
     * @return the correlation Id for this message
     */
    protected Object getReplyAggregateIdentifier(MuleMessage message)
    {
        return messageInfoMapping.getCorrelationId(message);
    }

    /**
     * Extracts a Group identifier from the current event. When an event is received
     * with a group identifier not registered with this router, a new group is
     * created. The id returned here can be a correlationId or some custom
     * aggregation Id. This implementation uses the Unique Message Id of the
     * MuleMessage being returned a
     * 
     * @param message A response messages received on the response router endpoint
     * @return an aggregation Id for this event
     */
    protected Object getCallResponseAggregateIdentifier(MuleMessage message)
    {
        return messageInfoMapping.getMessageId(message);
    }
}
