/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.transport.AbstractMessageDispatcher;

public class TestMessageDispatcher extends AbstractMessageDispatcher
{

    public TestMessageDispatcher(final OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    protected void doInitialise()
    {
        // template method
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        if (event.getEndpoint().getEndpointURI().toString().equals("test://AlwaysFail"))
        {
            throw new RoutingException(event.getMessage(), event.getEndpoint());
        }
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        if (event.getEndpoint().getEndpointURI().toString().equals("test://AlwaysFail"))
        {
            throw new RoutingException(event.getMessage(), event.getEndpoint());
        }
        return event.getMessage();
    }

    protected void doConnect() throws Exception
    {
        // no op
    }

    protected void doDisconnect() throws Exception
    {
        // no op
    }

    protected void doStart() 
    {
        // no op
    }

    protected void doStop() 
    {
        // no op
    }
}
