/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.outbound;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;

import java.util.Iterator;

/**
 * Sets the outbound root message id on as a property of the message using the following key:
 * {@link org.mule.api.config.MuleProperties#MULE_ROOT_MESSAGE_ID_PROPERTY}.
 */
public class OutboundRootMessageIdPropertyMessageProcessor implements MessageProcessor
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        event.getMessage().setOutboundProperty(MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY,
            event.getMessage().getMessageRootId());
        return event;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
