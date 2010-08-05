/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.requestreply;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.RequestReplyReplierMessageProcessor;
import org.mule.api.transport.ReplyToHandler;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.transport.AbstractConnector;

import org.apache.commons.lang.BooleanUtils;

public class ReplyToPropertyRequestReplyReplier extends AbstractInterceptingMessageProcessor
    implements RequestReplyReplierMessageProcessor
{

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        Object replyTo = event.getMessage().getReplyTo();
        ReplyToHandler replyToHandler = getReplyToHandler(event.getMessage(),
            (InboundEndpoint) event.getEndpoint());
        // Do not propagate REPLY_TO
        event.getMessage().setReplyTo(null);

        MuleEvent resultEvent = processNext(event);

        // Allow components to stop processing of the ReplyTo property (e.g. CXF)
        final String replyToStop = resultEvent.getMessage().getInvocationProperty(
            MuleProperties.MULE_REPLY_TO_STOP_PROPERTY);
        if (resultEvent != null && !BooleanUtils.toBoolean(replyToStop))
        {
            processReplyTo(event, resultEvent, replyToHandler, replyTo);
        }

        return resultEvent;
    }

    protected ReplyToHandler getReplyToHandler(MuleMessage message, InboundEndpoint endpoint)
    {
        Object replyTo = message.getReplyTo();
        ReplyToHandler replyToHandler = null;
        if (replyTo != null)
        {
            replyToHandler = ((AbstractConnector) endpoint.getConnector()).getReplyToHandler(endpoint);
            // Use the response transformer for the event if one is set
            if (endpoint.getResponseTransformers() != null)
            {
                replyToHandler.setTransformers(endpoint.getResponseTransformers());
            }
        }
        return replyToHandler;
    }

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
