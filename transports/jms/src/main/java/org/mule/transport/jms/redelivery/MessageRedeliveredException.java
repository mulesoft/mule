/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.redelivery;

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.jms.i18n.JmsMessages;

public class MessageRedeliveredException extends org.mule.api.exception.MessageRedeliveredException
{
    public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, InboundEndpoint endpoint, FlowConstruct flow, MuleMessage muleMessage)
    {
        super(messageId, redeliveryCount, maxRedelivery, endpoint,
            new DefaultMuleEvent(muleMessage, endpoint, flow),JmsMessages.tooManyRedeliveries(messageId, redeliveryCount, maxRedelivery, endpoint));
    }

}
