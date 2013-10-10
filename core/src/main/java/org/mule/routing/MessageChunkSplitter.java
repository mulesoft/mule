/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;

import java.util.ArrayList;
import java.util.List;

/**
 * A router that breaks up the current message onto smaller parts and sends them to
 * the same destination. The Destination service needs to have a
 * MessageChunkingAggregator inbound router in order to rebuild the message at the
 * other end.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www.eaipatterns.com/Sequencer.html</a>
 */
public class MessageChunkSplitter extends AbstractSplitter
{

    protected int messageSize = 0;

    public int getMessageSize()
    {
        return messageSize;
    }

    public void setMessageSize(int messageSize)
    {
        this.messageSize = messageSize;
    }

    @Override
    protected boolean isSplitRequired(MuleEvent event)
    {
        return messageSize != 0;
    }

    protected List<MuleMessage> splitMessage(MuleEvent event) throws RoutingException
    {
        MuleMessage message = event.getMessage();
        List<MuleMessage> messageParts = new ArrayList<MuleMessage>();
        byte[] data;
        try
        {
            data = message.getPayloadAsBytes();
        }
        catch (Exception e)
        {
            throw new RoutingException(CoreMessages.failedToReadPayload(), event, next, e);
        }

        int parts = data.length / messageSize;
        if ((parts * messageSize) < data.length)
        {
            parts++;
        }
        int len = messageSize;
        int count = 0;
        int pos = 0;
        byte[] buffer;
        for (; count < parts; count++)
        {
            if ((pos + len) > data.length)
            {
                len = data.length - pos;
            }
            buffer = new byte[len];
            System.arraycopy(data, pos, buffer, 0, buffer.length);
            pos += len;
            MuleMessage part = new DefaultMuleMessage(buffer, message, muleContext);
            part.setCorrelationId(message.getUniqueId());
            part.setCorrelationGroupSize(parts);
            part.setCorrelationSequence(count);
            messageParts.add(part);
        }
        return messageParts;
    }

}
