/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.DefaultMuleEvent;
import org.mule.api.CompletionHandler;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.api.transport.NonBlockingReplyToHandler;
import org.mule.processor.TestNonBlockingProcessor;
import org.mule.transport.AbstractMessageDispatcher;

public class TestMessageDispatcher extends AbstractMessageDispatcher
{
    public TestMessageDispatcher(final OutboundEndpoint endpoint)
    {
        super(endpoint);
    }
    private MessageProcessor nonBlockingProcessor = new TestNonBlockingProcessor();

    @Override
    protected void doInitialise()
    {
        // template method
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        if (endpoint.getEndpointURI().toString().equals("test://AlwaysFail"))
        {
            throw new RoutingException(event, (OutboundEndpoint) endpoint);
        }
    }

    @Override
    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        if (endpoint.getEndpointURI().toString().equals("test://AlwaysFail"))
        {
            throw new RoutingException(event, (OutboundEndpoint) endpoint);
        }
        return event.getMessage();
    }

    @Override
    protected void doSendNonBlocking(MuleEvent event, final CompletionHandler<MuleMessage, Exception> completionHandler)
    {
        if (endpoint.getEndpointURI().toString().equals("test://AlwaysFail"))
        {
            completionHandler.onFailure(new RoutingException(event, (OutboundEndpoint) endpoint));
        }
        else
        {
            try
            {
                final MuleMessage response = event.getMessage();
                event = new DefaultMuleEvent(event, new NonBlockingReplyToHandler()
                {
                    @Override
                    public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo)
                    {
                        completionHandler.onCompletion(response);
                    }

                    @Override
                    public void processExceptionReplyTo(MessagingException exception, Object replyTo)
                    {
                        completionHandler.onFailure(exception);
                    }
                });
                nonBlockingProcessor.process(event);
            }
            catch (Exception e)
            {
                completionHandler.onFailure(e);
            }
        }
    }

    @Override
    protected void doConnect() throws Exception
    {
        // no op
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // no op
    }

    @Override
    protected void doStart() 
    {
        // no op
    }

    @Override
    protected void doStop() 
    {
        // no op
    }

    @Override
    protected boolean isSupportsNonBlocking()
    {
        return true;
    }
}
