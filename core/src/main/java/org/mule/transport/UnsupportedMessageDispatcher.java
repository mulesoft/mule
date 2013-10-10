/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;

public final class UnsupportedMessageDispatcher extends AbstractMessageDispatcher
{

    public UnsupportedMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Dispatch not supported for this transport.");
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        throw new UnsupportedOperationException("Send not supported for this transport.");
    }

    protected void doInitialise()
    {
        // empty
    }

    protected void doDispose()
    {
        // empty
    }

    protected void doConnect() throws Exception
    {
        // empty
    }

    protected void doDisconnect() throws Exception
    {
        // empty
    }

    protected void doStart() 
    {
        // empty
    }

    protected void doStop() 
    {
        // empty
    }
}
