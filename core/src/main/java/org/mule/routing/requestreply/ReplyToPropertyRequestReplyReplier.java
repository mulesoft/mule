/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.requestreply;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.RequestReplyReplierMessageProcessor;
import org.mule.api.transport.ReplyToHandler;
import org.mule.processor.AbstractInterceptingMessageProcessor;

import org.apache.commons.lang.BooleanUtils;

public class ReplyToPropertyRequestReplyReplier extends AbstractReplyToPropertyRequestReplyReplier
{

    @Override
    protected boolean shouldProcessEvent(MuleEvent event)
    {
        return event.getExchangePattern().hasResponse();
    }

}
