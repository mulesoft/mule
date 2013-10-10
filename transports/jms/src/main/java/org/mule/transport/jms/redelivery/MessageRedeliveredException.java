/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.redelivery;

import org.mule.DefaultMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.session.DefaultMuleSession;
import org.mule.transport.jms.i18n.JmsMessages;

public class MessageRedeliveredException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 9013890402770563931L;

    protected final transient ImmutableEndpoint endpoint;
    
    String messageId;
    int redeliveryCount;
    int maxRedelivery;

    public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, ImmutableEndpoint endpoint, FlowConstruct flow, MuleMessage muleMessage)
    {        
        super(JmsMessages.tooManyRedeliveries(messageId, redeliveryCount, maxRedelivery, endpoint), 
            new DefaultMuleEvent(muleMessage, endpoint, new DefaultMuleSession(flow, endpoint.getMuleContext())));
        this.messageId = messageId;
        this.redeliveryCount = redeliveryCount;
        this.maxRedelivery = maxRedelivery;
        this.endpoint = endpoint;
    }
    
    public String getMessageId()
    {
        return messageId;
    }

    public int getRedeliveryCount()
    {
        return redeliveryCount;
    }

    public int getMaxRedelivery()
    {
        return maxRedelivery;
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }
}
