/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.processor;

import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.service.Service;
import org.mule.api.transport.ReplyToHandler;
import org.mule.processor.AbstractInterceptingMessageProcessor;

import org.apache.commons.lang.BooleanUtils;
@Deprecated
public class ServiceInternalMessageProcessor extends AbstractInterceptingMessageProcessor
{

    protected Service service;

    public ServiceInternalMessageProcessor(Service service)
    {
        this.service = service;
    }

    /**
     * We do all this together here rather than chaining them in order to conserve
     * 2.x exception handling behaviour
     */
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent resultEvent;
        try
        {

            resultEvent = service.getComponent().process(event);
            resultEvent = processNext(resultEvent);

            if (!event.getExchangePattern().hasResponse())
            {

                Object replyTo = event.getReplyToDestination();
                ReplyToHandler replyToHandler = event.getReplyToHandler();

                // Allow components to stop processing of the ReplyTo property (e.g.
                // CXF)
                if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent) && replyTo != null)
                {
                    String replyToStop = resultEvent.getMessage().getInvocationProperty(MuleProperties.MULE_REPLY_TO_STOP_PROPERTY);
                    if (!event.getExchangePattern().hasResponse() || !BooleanUtils.toBoolean(replyToStop))
                    {
                        processReplyTo(event, resultEvent, replyToHandler, replyTo);
                    }
                }
            }
            return resultEvent;
        }
        catch (Exception e)
        {
            event.getSession().setValid(false);
            if (e instanceof MuleException)
            {
                throw (MuleException) e;
            }
            else
            {
                throw new MessagingException(event, e, this);
            }
        }
    }

    protected void processReplyTo(MuleEvent event,
                                  MuleEvent result,
                                  ReplyToHandler replyToHandler,
                                  Object replyTo) throws MuleException
    {
        if (result != null && replyToHandler != null)
        {
            String requestor = result.getMessage().getOutboundProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY);
            if ((requestor != null && !requestor.equals(service.getName())) || requestor == null)
            {
                replyToHandler.processReplyTo(event, result.getMessage(), replyTo);
            }
        }
    }

}
