/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.requestreply;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;

/**
 * ReplyToMessageProcessor for async flow.
 * <p/>
 * In synchronous flows will skip execution
 */
public class ReplyToAsyncProcessor extends ReplyToMessageProcessor

{

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (shouldProcessEvent(event))
        {
            return super.process(event);
        }
        else
        {
            return processNext(event);
        }
    }

    protected boolean shouldProcessEvent(final MuleEvent event) throws MessagingException
    {
        Object messageProperty = event.getMessage().getInboundProperty(MuleProperties.MULE_FORCE_SYNC_PROPERTY);
        boolean forceSync = Boolean.TRUE.equals(messageProperty);

        boolean hasResponse = event.getEndpoint().getExchangePattern().hasResponse();
        boolean isTransacted = event.getEndpoint().getTransactionConfig().isTransacted();

        return !forceSync && !hasResponse && !isTransacted;
    }

}
