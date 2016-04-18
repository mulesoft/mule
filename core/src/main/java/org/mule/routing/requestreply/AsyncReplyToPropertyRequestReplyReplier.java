/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.requestreply;

import org.mule.api.NonBlockingSupported;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.transport.DefaultReplyToHandler;

public class AsyncReplyToPropertyRequestReplyReplier extends AbstractReplyToPropertyRequestReplyReplier implements NonBlockingSupported
{

    @Override
    protected boolean shouldProcessEvent(MuleEvent event)
    {
        // Only process ReplyToHandler is running one-way and standard ReplyToHandler is being used.
        return !event.getExchangePattern().hasResponse() && (event.getFlowConstruct() instanceof Flow) && event
                .getReplyToHandler() instanceof DefaultReplyToHandler;
    }

}
