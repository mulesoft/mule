/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling;

import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.ResponseOutputStream;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.SessionHandler;
import org.mule.transport.RoutingMessageTemplate;

import java.io.OutputStream;

/**
 * <p>
 * Implementation of {@link RoutingMessageTemplate} for poll elements.
 * </p>
 * <p>
 * Poll elements
 * </p>
 *
 * @since 3.5.0
 */
public class PollMessageRouter extends RoutingMessageTemplate
{


    private FlowConstruct flowConstruct;
    private MessageProcessorPollingInterceptor interceptor;

    public PollMessageRouter(MessageProcessor listener,
                             MessageExchangePattern exchangePattern, SessionHandler sessionHandler, FlowConstruct flowConstruct,
                             MessageProcessorPollingInterceptor interceptor)
    {
        super(listener, exchangePattern, sessionHandler);
        this.flowConstruct = flowConstruct;
        this.interceptor = interceptor;
    }

    @Override
    public MuleEvent routeEvent(MuleEvent originalEvent, OutputStream outputStream) throws MuleException
    {
        warnIfMuleClientSendUsed(originalEvent.getMessage());

        propagateRootMessageIdProperty(originalEvent.getMessage());

        MuleEvent muleEvent = createMuleEvent(originalEvent.getMessage(), outputStream);

        interceptor.prepareRouting(originalEvent, muleEvent);

        return routeEvent(muleEvent);
    }

    @Override
    protected void applyInboundTransformers(MuleEvent muleEvent) throws MuleException
    {
        // no inbound transformer for poll
    }

    @Override
    protected MuleEvent doCreateEvent(MuleMessage message, ResponseOutputStream ros, MuleSession session)
    {
        return new DefaultMuleEvent(message, exchangePattern, flowConstruct, session);
    }

    @Override
    protected void fireResponseNotification(MuleEvent resultEvent)
    {
        //TODO: SEND notification? if so, what type?
        // We are not sending response notification now
    }

    @Override
    protected void applyResponseTransformers(MuleEvent resultEvent) throws MuleException
    {
        // no response for Polling
    }

    @Override
    protected MuleEvent handleResponse(MuleEvent resultEvent) throws MuleException
    {
        if (resultEvent != null)
        {
            interceptor.postProcessRouting(resultEvent);
        }
        return super.handleResponse(resultEvent);
    }
}
