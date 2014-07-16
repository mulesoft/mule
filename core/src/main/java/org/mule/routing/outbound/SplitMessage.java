/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    private List<MessagePart> parts = new ArrayList<MessagePart>();

    public void addPart(Object part, OutboundEndpoint endpoint)
    {
        parts.add(new MessagePart(endpoint, part));
    }

    public MessagePart getPart(int i)
    {
        return parts.get(i);
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


        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append("MessagePart");
            sb.append("{endpoint=").append(endpoint.getName());
            sb.append(", part=").append(part);
            sb.append('}');
            return sb.toString();
        }
    }
}
