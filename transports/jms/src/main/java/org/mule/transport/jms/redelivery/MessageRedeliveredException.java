/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
