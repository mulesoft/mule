/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.transport.AbstractMessageDispatcher;
import org.mule.tck.processor.TestNonBlockingProcessor;

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
    protected void doSendNonBlocking(MuleEvent event, CompletionHandler<MuleMessage, Exception, Void> completionHandler)
    {
        if (endpoint.getEndpointURI().toString().equals("test://AlwaysFail"))
        {
            completionHandler.onFailure(new RoutingException(event, (OutboundEndpoint) endpoint));
        }
        else
        {
            try
            {
                nonBlockingProcessor.process(event);
            }
            catch (MuleException e)
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
