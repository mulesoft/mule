/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.requestreply;

import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.InternalMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.RequestReplyReplierMessageProcessor;
import org.mule.api.transport.ReplyToHandler;
import org.mule.processor.AbstractInterceptingMessageProcessor;

import org.apache.commons.lang.BooleanUtils;

public abstract class AbstractReplyToPropertyRequestReplyReplier extends AbstractInterceptingMessageProcessor
        implements RequestReplyReplierMessageProcessor, InternalMessageProcessor
{

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent resultEvent;
        if (shouldProcessEvent(event))
        {
            Object replyTo = event.getReplyToDestination();
            ReplyToHandler replyToHandler = event.getReplyToHandler();

            resultEvent = processNext(event);

            // Allow components to stop processing of the ReplyTo property (e.g. CXF)
            final String replyToStop = resultEvent.getMessage().getInvocationProperty(
                    MuleProperties.MULE_REPLY_TO_STOP_PROPERTY);
            if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent)
                && !BooleanUtils.toBoolean(replyToStop))
            {
                // reply-to processing should not resurrect a dead event
                processReplyTo(event, resultEvent, replyToHandler, replyTo);
            }
        }
        else
        {
            resultEvent = processNext(event);
        }
        return resultEvent;
    }

    protected abstract boolean shouldProcessEvent(MuleEvent event);

    protected void processReplyTo(MuleEvent event,
                                  MuleEvent result,
                                  ReplyToHandler replyToHandler,
                                  Object replyTo) throws MuleException
    {
        if (result != null && replyToHandler != null)
        {
            String requestor = result.getMessage().getOutboundProperty(
                    MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
            if ((requestor != null && !requestor.equals(event.getFlowConstruct().getName()))
                || requestor == null)
            {
                replyToHandler.processReplyTo(event, result.getMessage(), replyTo);
            }
        }
    }

    public void setReplyProcessor(MessageProcessor replyMessageProcessor)
    {
        // Not used
    }

}
