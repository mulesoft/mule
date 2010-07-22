/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.inbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.processor.AbstractMessageObserver;
import org.mule.util.StringMessageUtils;

public class InboundLoggingMessageProcessor extends AbstractMessageObserver
{

    protected InboundEndpoint endpoint;

    public InboundLoggingMessageProcessor(InboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    @Override
    public void observe(MuleEvent event)
    {
        MuleMessage message = event.getMessage();
        if (logger.isDebugEnabled())
        {
            logger.debug("Message Received on: " + endpoint.getEndpointURI());
        }
        if (logger.isTraceEnabled())
        {
            try
            {
                logger.trace("Message Payload: \n"
                             + StringMessageUtils.truncate(StringMessageUtils.toString(message.getPayload()),
                                 200, false));
                logger.trace("Message detail: \n" + StringMessageUtils.headersToString(message));
            }
            catch (Exception e)
            {
                // ignore
            }
        }
    }
}
