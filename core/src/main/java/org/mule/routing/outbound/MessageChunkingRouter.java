/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;

/**
 * A router that breaks up the current message onto smaller parts and sends them to the
 * same destination. The Destination service needs to have a MessageChunkingAggregator
 * inbound router in order to rebuild the message at the other end.
 * 
 * @see org.mule.routing.inbound.MessageChunkingAggregator
 */
public class MessageChunkingRouter extends FilteringOutboundRouter
{
    private int messageSize = 0;
    private int numberOfMessages = 1;

    public int getMessageSize()
    {
        return messageSize;
    }

    public void setMessageSize(int messageSize)
    {
        this.messageSize = messageSize;
    }

    public int getNumberOfMessages()
    {
        return numberOfMessages;
    }

    public void setNumberOfMessages(int numberOfMessages)
    {
        this.numberOfMessages = numberOfMessages;
    }

    public MuleMessage route(MuleMessage message, MuleSession session, boolean synchronous)
        throws RoutingException
    {
        if (messageSize == 0 && numberOfMessages < 2)
        {
            return super.route(message, session, synchronous);
        }
        else if (messageSize > 0)
        {
            byte[] data = new byte[0];
            try
            {
                data = message.getPayloadAsBytes();
            }
            catch (Exception e)
            {
                throw new RoutingException(CoreMessages.failedToReadPayload(), message, getEndpoint(0,
                    message), e);
            }

            int parts = data.length / messageSize;
            if ((parts * messageSize) < data.length)
            {
                parts++;
            }
            int len = messageSize;
            MuleMessage part = null;
            int count = 0;
            int pos = 0;
            byte[] buffer = null;
            try
            {
                for (; count < parts; count++)
                {
                    if ((pos + len) > data.length)
                    {
                        len = data.length - pos;
                    }
                    buffer = new byte[len];
                    System.arraycopy(data, pos, buffer, 0, buffer.length);
                    pos += len;
                    part = new DefaultMuleMessage(buffer, message);
                    part.setCorrelationId(message.getUniqueId());
                    part.setCorrelationGroupSize(parts);
                    part.setCorrelationSequence(count);
                    // TODO - remove or downgrade once MULE-1718 is fixed,
                    // for now these really help see the problem if you set the level for this class to INFO
                    if (logger.isInfoEnabled())
                    {
                        logger.info("sending part " + count + " of " + parts);
                    }
                    super.route(part, session, synchronous);
                    if (logger.isInfoEnabled())
                    {
                        logger.info("sent");
                    }
                }

            }
            catch (RoutingException e)
            {
                // we'll want to send the whole message to the Exception handler
                e = new RoutingException(e.getI18nMessage(), e.getUmoMessage(), e.getEndpoint(), e.getCause());
                // e.addInfo("chunking", "true");
                // buffer = new byte[data.length - len];
                // System.arraycopy(data, len, buffer, 0, buffer.length);
                // e.addInfo("remaining data", buffer);
                throw e;
            }
        }
        return message;
    }
}
