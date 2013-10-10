/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.config.i18n.CoreMessages;

import java.util.ArrayList;
import java.util.List;

/**
 * A Split message contains one or more message parts with an endpoint associated with each part.
 * This class is used by the Message Splitter routers ({@link org.mule.routing.outbound.AbstractRoundRobinMessageSplitter})
 * to define a mapping between message parts and the endpoint to dispatch on.
 */
public class SplitMessage
{
    private List parts = new ArrayList();

    public void addPart(Object part, OutboundEndpoint endpoint)
    {
        parts.add(new MessagePart(endpoint, part));
    }

    public MessagePart getPart(int i)
    {
        return (MessagePart) parts.get(i);
    }

    public int size()
    {
        return parts.size();
    }

    public class MessagePart
    {
        private Object part;
        private OutboundEndpoint endpoint;

        public MessagePart(OutboundEndpoint endpoint, Object part)
        {
            if (endpoint == null)
            {
                throw new IllegalArgumentException(CoreMessages.objectIsNull("splitter endpoint").getMessage());
            }

            if (part == null)
            {
                throw new IllegalArgumentException(CoreMessages.objectIsNull("splitter messagePart").getMessage());
            }
            this.endpoint = endpoint;
            this.part = part;
        }

        public OutboundEndpoint getEndpoint()
        {
            return endpoint;
        }

        public Object getPart()
        {
            return part;
        }


        public String toString()
        {
            final StringBuffer sb = new StringBuffer();
            sb.append("MessagePart");
            sb.append("{endpoint=").append(endpoint.getName());
            sb.append(", part=").append(part);
            sb.append('}');
            return sb.toString();
        }
    }
}
