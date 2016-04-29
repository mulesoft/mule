/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.redelivery;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.transport.jms.i18n.JmsMessages;

public class MessageRedeliveredException extends org.mule.runtime.core.api.exception.MessageRedeliveredException
{
    public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, InboundEndpoint endpoint, FlowConstruct flow, MuleMessage muleMessage)
    {
        super(messageId, redeliveryCount, maxRedelivery, endpoint,
            buildEvent(endpoint, flow, muleMessage),JmsMessages.tooManyRedeliveries(messageId, redeliveryCount, maxRedelivery, endpoint));
    }

    protected static DefaultMuleEvent buildEvent(InboundEndpoint endpoint, FlowConstruct flow, MuleMessage muleMessage)
    {
        final DefaultMuleEvent event = new DefaultMuleEvent(muleMessage, flow);
        event.populateFieldsFromInboundEndpoint(endpoint);
        return event;
    }

}
