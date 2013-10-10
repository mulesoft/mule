/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;

/**
 * A router that breaks up the current message onto smaller parts and sends them to the
 * same destination. The Destination service needs to have a MessageChunkingAggregator
 * inbound router in order to rebuild the message at the other end.
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

    @Override
    public MuleEvent route(MuleEvent event) throws RoutingException
    {
        MuleMessage message = event.getMessage();
        MuleSession session = event.getSession();
        if (messageSize == 0 && numberOfMessages < 2)
        {
            return super.route(event);
        }
        else if (messageSize > 0)
        {
            byte[] data;
            try
            {
                data = message.getPayloadAsBytes();
            }
            catch (Exception e)
            {
                throw new RoutingException(CoreMessages.failedToReadPayload(), event, getRoute(0, event), e);
            }

            int parts = data.length / messageSize;
            if ((parts * messageSize) < data.length)
            {
                parts++;
            }
            int len = messageSize;
            MuleMessage part;
            int count = 0;
            int pos = 0;
            byte[] buffer;
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
                    part = new DefaultMuleMessage(buffer, message, muleContext);
                    part.setCorrelationId(message.getUniqueId());
                    part.setCorrelationGroupSize(parts);
                    part.setCorrelationSequence(count);

                    if (logger.isInfoEnabled())
                    {
                        logger.info(String.format("sending part %d of %d (seq # %d)", count + 1, parts, count));
                    }
                    super.route(new DefaultMuleEvent(part, event.getExchangePattern(), flowConstruct, session));
                    if (logger.isInfoEnabled())
                    {
                        logger.info("sent");
                    }
                }
            }
            catch (RoutingException e)
            {
                // we'll want to send the whole message to the Exception handler
                e = new RoutingException(e.getI18nMessage(), e.getEvent(), e.getRoute(), e.getCause());
                // e.addInfo("chunking", "true");
                // buffer = new byte[data.length - len];
                // System.arraycopy(data, len, buffer, 0, buffer.length);
                // e.addInfo("remaining data", buffer);
                throw e;
            }
        }
        return event;
    }
}
