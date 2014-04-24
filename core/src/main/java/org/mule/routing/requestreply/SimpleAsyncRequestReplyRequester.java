/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.requestreply;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;

public class SimpleAsyncRequestReplyRequester extends AbstractAsyncRequestReplyRequester
    implements Startable, Stoppable
{

    protected MessageProcessor requestMessageProcessor;

    @Override
    protected void sendAsyncRequest(MuleEvent event) throws MuleException
    {
        setAsyncReplyProperties(event);
        requestMessageProcessor.process(event);
    }

    protected void setAsyncReplyProperties(MuleEvent event) throws MuleException
    {
        event.getMessage().setReplyTo(getReplyTo());
        event.getMessage().setOutboundProperty(MuleProperties.MULE_REPLY_TO_REQUESTOR_PROPERTY,
            event.getFlowConstruct().getName());
        String correlation = event.getFlowConstruct().getMessageInfoMapping().getCorrelationId(
            event.getMessage());
        event.getMessage().setCorrelationId(correlation);
    }

    private String getReplyTo()
    {
        InboundEndpoint endpoint = ((InboundEndpoint) replyMessageSource);
        return endpoint.getConnector().getCanonicalURI(endpoint.getEndpointURI());
    }

    @Override
    protected void verifyReplyMessageSource(MessageSource messageSource)
    {
        if (!(messageSource instanceof InboundEndpoint))
        {
            throw new IllegalArgumentException(
                "Only an InboundEndpoint reply MessageSource is supported with SimpleAsyncRequestReplyRequester");
        }
    }

    public void setMessageProcessor(MessageProcessor processor)
    {
        requestMessageProcessor = processor;
    }

    @Deprecated
    public void setMessageSource(MessageSource source)
    {
        setReplySource(source);
    }

    public void start() throws MuleException
    {
        if (replyMessageSource != null)
        {
            if (replyMessageSource instanceof FlowConstructAware)
            {
                ((FlowConstructAware) replyMessageSource).setFlowConstruct(this.flowConstruct);
            }
            if (replyMessageSource instanceof Initialisable)
            {
                ((Initialisable) replyMessageSource).initialise();
            }
            if (replyMessageSource instanceof Startable)
            {
                ((Startable) replyMessageSource).start();
            }
        }
        if (requestMessageProcessor != null)
        {
            if (requestMessageProcessor instanceof FlowConstructAware)
            {
                ((FlowConstructAware) requestMessageProcessor).setFlowConstruct(this.flowConstruct);
            }
            if (requestMessageProcessor instanceof Initialisable)
            {
                ((Initialisable) requestMessageProcessor).initialise();
            }
            if (requestMessageProcessor instanceof Startable)
            {
                ((Startable) requestMessageProcessor).start();
            }
        }
        super.start();
    }

    public void stop() throws MuleException
    {
        if (replyMessageSource != null && replyMessageSource instanceof Stoppable)
        {
            ((Stoppable) replyMessageSource).stop();

            if (requestMessageProcessor != null && requestMessageProcessor instanceof Stoppable)
            {
                ((Stoppable) requestMessageProcessor).stop();
            }
        }
        if (requestMessageProcessor != null)
        {
            if (requestMessageProcessor instanceof Stoppable)
            {
                ((Stoppable) requestMessageProcessor).stop();
            }
            if (requestMessageProcessor instanceof Disposable)
            {
                ((Disposable) requestMessageProcessor).dispose();
            }
        }
        super.stop();
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        super.setMuleContext(context);
        if (requestMessageProcessor instanceof MuleContextAware)
        {
            ((MuleContextAware)requestMessageProcessor).setMuleContext(context);
        }
    }

}
