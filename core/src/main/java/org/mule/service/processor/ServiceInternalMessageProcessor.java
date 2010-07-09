/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service.processor;

import org.mule.AbstractExceptionListener;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.ExceptionPayload;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.api.transport.PropertyScope;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.message.DefaultExceptionPayload;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.routing.response.AsyncReplyReceiveMessageProcessor;
import org.mule.transport.AbstractConnector;
import org.mule.transport.NullPayload;

import java.beans.ExceptionListener;

import org.apache.commons.lang.BooleanUtils;

public class ServiceInternalMessageProcessor extends AbstractInterceptingMessageProcessor
{

    protected Service service;
    protected MessageProcessor receiveAsyncReplyMessageProcessor;

    public ServiceInternalMessageProcessor(Service service)
    {
        this.service = service;
        receiveAsyncReplyMessageProcessor = new AsyncReplyReceiveMessageProcessor(service.getResponseRouter());
    }

    /**
     * We do all this together here rather than chaining them in order to conserve
     * 2.x exception handling behaviour
     */
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        ExceptionListener exceptionListener = service.getExceptionListener();
        MuleEvent resultEvent = null;
        try
        {
            Object replyTo = event.getMessage().getReplyTo();
            ReplyToHandler replyToHandler = getReplyToHandler(event.getMessage(),
                (InboundEndpoint) event.getEndpoint());
            // Do not propagate REPLY_TO beyond the inbound endpoint
            event.getMessage().setReplyTo(null);

            resultEvent = service.getComponent().process(event);
            resultEvent = processNext(resultEvent);
            resultEvent = receiveAsyncReplyMessageProcessor.process(resultEvent);

            // Allow components to stop processing of the ReplyTo property (e.g.
            // CXF)
            final String replyToStop = (String) resultEvent.getMessage().getProperty(MuleProperties.MULE_REPLY_TO_STOP_PROPERTY, PropertyScope.INVOCATION);
            if (!event.isSynchronous()
                || (resultEvent != null && !BooleanUtils.toBoolean(replyToStop)))
            {
                processReplyTo(event, resultEvent, replyToHandler, replyTo);
            }
        }
        catch (Exception e)
        {
            event.getSession().setValid(false);

            if (e instanceof MessagingException)
            {
                exceptionListener.exceptionThrown(e);
            }
            else
            {
                exceptionListener.exceptionThrown(new MessagingException(
                    CoreMessages.eventProcessingFailedFor(service.getName()), event.getMessage(), e));
            }
            if (event.isSynchronous())
            {
                if (resultEvent == null)
                {
                    if (exceptionListener != null && exceptionListener instanceof AbstractExceptionListener
                        && ((AbstractExceptionListener) exceptionListener).getReturnMessage() != null)
                    {
                        resultEvent = new DefaultMuleEvent(
                            ((AbstractExceptionListener) exceptionListener).getReturnMessage(), event);
                    }
                    else
                    {
                        resultEvent = new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(),
                            RequestContext.getEvent().getMessage(), event.getMuleContext()), event);
                    }
                }
                ExceptionPayload exceptionPayload = event.getMessage().getExceptionPayload();
                if (exceptionPayload == null)
                {
                    exceptionPayload = new DefaultExceptionPayload(e);
                }
                resultEvent.getMessage().setExceptionPayload(exceptionPayload);
            }
        }
        return resultEvent;
    }

    protected ReplyToHandler getReplyToHandler(MuleMessage message, InboundEndpoint endpoint)
    {
        Object replyTo = message.getReplyTo();
        ReplyToHandler replyToHandler = null;
        if (replyTo != null)
        {
            replyToHandler = ((AbstractConnector) endpoint.getConnector()).getReplyToHandler();
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
            // TODO event.getProperty() is confusing, rework it - takes PropertyScope as a default value, not scope 
            String requestor = (String) result.getMessage().getProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY, PropertyScope.OUTBOUND);
            if ((requestor != null && !requestor.equals(service.getName())) || requestor == null)
            {
                replyToHandler.processReplyTo(event, result.getMessage(), replyTo);
            }
        }
    }

}
