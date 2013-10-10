/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.inbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;
import org.mule.util.StringMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class InboundLoggingMessageProcessor implements MessageProcessor
{
    protected final transient Log logger = LogFactory.getLog(getClass());
    protected InboundEndpoint endpoint;

    public InboundLoggingMessageProcessor(InboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
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

        return event;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
