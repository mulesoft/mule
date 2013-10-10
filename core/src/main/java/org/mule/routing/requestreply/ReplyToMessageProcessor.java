/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.requestreply;

import org.apache.commons.lang.BooleanUtils;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.transport.ReplyToHandler;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.processor.AbstractInterceptingMessageProcessor;

/**
 * Send message according to reply to property
 */
public class ReplyToMessageProcessor extends AbstractInterceptingMessageProcessor
        implements InterceptingMessageProcessor
{

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent resultEvent;
        //In config is service then this is executed by ServiceInternalMessageProcessor
        if (event.getFlowConstruct() instanceof SimpleFlowConstruct)
        {
            Object replyTo = event.getReplyToDestination();
            ReplyToHandler replyToHandler = event.getReplyToHandler();
            // Do not propagate REPLY_TO
            event.getMessage().setReplyTo(null);

            resultEvent = processNext(event);

            // Allow components to stop processing of the ReplyTo property (e.g. CXF)
            final String replyToStop = resultEvent.getMessage().getInvocationProperty(
                    MuleProperties.MULE_REPLY_TO_STOP_PROPERTY);
            if (resultEvent != null && !BooleanUtils.toBoolean(replyToStop))
            {
                processReplyTo(event, resultEvent, replyToHandler, replyTo);
            }
        }
        else
        {
            resultEvent = processNext(event);
        }
        return resultEvent;
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

}
